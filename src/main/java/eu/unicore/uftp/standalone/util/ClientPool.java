package eu.unicore.uftp.standalone.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.message.ParameterizedMessage;

import eu.unicore.uftp.client.UFTPSessionClient;
import eu.unicore.uftp.dpc.Utils;
import eu.unicore.uftp.standalone.ClientFacade;
import eu.unicore.util.Log;
import eu.unicore.util.Pair;

public class ClientPool implements Closeable {

	private final AtomicInteger num = new AtomicInteger(0);

	final ExecutorService es;

	final int poolSize;

	final SessionClientPool clients;

	final Queue<Pair<TransferTask,Future<Boolean>>> tasks;

	final boolean verbose;

	final int retryCount;

	private MultiProgressBar pb = null;

	public ClientPool(Queue<Pair<TransferTask,Future<Boolean>>> tasks, int poolSize, 
			final ClientFacade clientFacade, final String uri,
			boolean verbose, boolean showPerformance, int retryCount) {
		this.tasks = tasks;
		this.clients = new SessionClientPool(clientFacade, uri);
		this.poolSize = poolSize;
		this.verbose = verbose;
		this.retryCount = retryCount;
		if(showPerformance) {
			try{
				this.pb = new MultiProgressBar(poolSize);
			}catch(IOException ioe) {
				verbose("Cannot setup progress bar: {}", Log.getDetailMessage(ioe));
			}
		}
		this.es = Executors.newFixedThreadPool(poolSize, new ThreadFactory() {

			boolean createNewThreads = true;

			@Override
			public Thread newThread(Runnable r) {
				if(!createNewThreads) return null;
				try{
					UFTPSessionClient client = clientFacade.doConnect(uri);
					clients.add(client);
					UFTPClientThread t = new UFTPClientThread(r, clients);
					t.setName("UFTPClient-"+num.incrementAndGet());
					return t;
				}catch(Exception ex){
					clientFacade.message("Creating new client thread failed: {}", ex);
					createNewThreads = false;
					if(num.get()==0) {
						throw new RuntimeException(ex);
					}
					return null;
				}
			}
		});
	}

	@Override
	public void close() throws IOException{
		tasks.forEach(p -> {
			try{
				p.getM2().get();
			}catch(Exception e){}
		});
		clients.forEach(sc -> IOUtils.closeQuietly(sc));
		if(pb!=null)pb.close();
		es.shutdown();	
	}

	public void submit(TransferTask r) {
		if(pb!=null)r.setProgressListener(pb);
		tasks.add(new Pair<>(r, es.submit(r)));
	}

	public void verbose(String msg, Object ... params) {
		if(verbose)message(msg, params);
	}

	public void message(String msg, Object ... params) {
		System.out.println(new ParameterizedMessage(msg, params).getFormattedMessage());
	}

	public static class SessionClientPool extends LinkedBlockingQueue<UFTPSessionClient> {

		private static final long serialVersionUID=1l;

		private AtomicInteger numClients = new AtomicInteger(0);
		private final ClientFacade cf;
		private final String uri;
		
		public SessionClientPool(ClientFacade cf, String uri){
			this.cf = cf;
			this.uri = uri;
		}

		@Override
		public boolean add(UFTPSessionClient c) {
			numClients.incrementAndGet();
			return super.add(c);
		}

		@Override
		public UFTPSessionClient take() throws InterruptedException {
			if(numClients.get()==0) {
				try{
					add(cf.doConnect(uri));
				}catch(Exception ex) {
					cf.message("Creating new UFTP session failed: {}", ex);
				}
			}
			UFTPSessionClient c = super.take();
			numClients.decrementAndGet();
			return c;
		}
	}

	public static class UFTPClientThread extends Thread {

		private final BlockingQueue<UFTPSessionClient> clients;

		private UFTPSessionClient sc;

		public UFTPClientThread(Runnable target,  BlockingQueue<UFTPSessionClient> clients) {
			super(target);
			this.clients = clients;
		}

		public UFTPSessionClient getClient()throws InterruptedException {
			if(sc==null)sc = clients.take();
			return sc;
		}

		public void error() {
			sc = null;
		}

		public void done() {
			// client is still usable here, let's give it back for the next task
			if(sc!=null) {
				clients.add(sc);
			}
			sc=null;
		}
	}

	public static abstract class TransferTask implements Callable<Boolean> {

		private MultiProgressBar pb = null;

		private String id;

		private long dataSize = -1;

		private TransferTracker transferTracker;

		private final ClientPool pool;

		public TransferTask(ClientPool pool) {
			this.pool = pool;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public long getDataSize() {
			return dataSize;
		}

		public void setDataSize(long dataSize) {
			this.dataSize = dataSize;
		}

		public void setProgressListener(MultiProgressBar pb) {
			this.pb = pb;
		}

		// return true if this is not the first time running this task
		public boolean isRetrying() {
			return executionCounter>0;
		}

		private int executionCounter = 0;
		private Exception lastError = null;

		public UFTPSessionClient getSessionClient() throws InterruptedException {
			UFTPClientThread t = (UFTPClientThread)Thread.currentThread();
			UFTPSessionClient sc = t.getClient();
			if(pb!=null) { 
				pb.registerNew(transferTracker);
				sc.setProgressListener(pb);
			}
			return sc;
		}

		public void setTransferTracker(TransferTracker transferTracker) {
			this.transferTracker = transferTracker;
		}

		public Exception getLastError() {
			return lastError;
		}

		public int getRunCount() {
			return executionCounter;
		}

		protected abstract void doCall() throws Exception;
		
		@Override
		public Boolean call(){
			try {
				executionCounter++;
				if(transferTracker!=null) {
					transferTracker.start.compareAndExchange(0, System.currentTimeMillis());
				}
				doCall();
				((UFTPClientThread)Thread.currentThread()).done();
			}
			catch(Exception e) {
				lastError = e;
				((UFTPClientThread)Thread.currentThread()).error();
				if(executionCounter<=pool.retryCount) {
					// re-submit, creating a new task
					verbose("Re-submitting task {} after error {}", this.getId(), e.getMessage());
					transferTracker.reset();
					if(pb!=null) {
						pb.onRetryDiscard();
					}
					pool.submit(this);
					return Boolean.TRUE;
				}else {
					// we give up
					return Boolean.FALSE;
				}
			}
			if(pb!=null) {
				pb.closeCurrentThread();
			}
			return Boolean.TRUE;
		}

		public boolean checksumMatches(String localFile, String remoteFile, long offset, long size)
				throws Exception{
			String localCS = checksum(localFile, offset, size);
			@SuppressWarnings("resource")
			String remoteCS = getSessionClient().getHash(remoteFile, offset, size).hash;
			return localCS.equals(remoteCS);
		}

		public void verbose(String msg, Object...params) {
			System.out.println(new ParameterizedMessage(msg, params).getFormattedMessage());
		}
	}

	/*
	 * one of these is created per transferred file
	 */
	public static class TransferTracker {
		private static final AtomicInteger transferIdGen = new AtomicInteger(0);
		public final int transferID = transferIdGen.incrementAndGet();

		public final String file;
		public final long size;
		public final AtomicLong start;
		public final int numChunks;
		public final AtomicInteger chunkCounter;
		private long bytesTransferred;

		public TransferTracker(String file, long size, int numChunks, AtomicInteger chunkCounter, AtomicLong start) {
			this.file = file;
			this.size = size;
			this.chunkCounter = chunkCounter;
			this.start = start;
			this.numChunks = numChunks;
		}

		public void addBytesTransferred(long bytes) {
			bytesTransferred+=bytes;
		}

		public long getBytesTransferred() {
			return bytesTransferred;
		}

		public void reset() {
			bytesTransferred=0;
		}

		@Override
		public boolean equals(Object other) {
			if(other!=null && other instanceof TransferTracker) {
				return ((TransferTracker)other).transferID == transferID;
			}
			return false;
		}

	}

	final static String algo = "MD5";

	public static String checksum(String localFile, long offset, long size) throws Exception {
		try(RandomAccessFile f = new RandomAccessFile(localFile, "r")){
			f.seek(offset);
			int bufsize = 16384;
			byte[]buf = new byte[bufsize];
			MessageDigest md = MessageDigest.getInstance(algo);
			int r = 0;
			int l = 0;
			long remaining = size;
			while(true) {
				l = remaining>bufsize? bufsize:(int)remaining;
				r = f.read(buf, 0, l);
				if (r <= 0) {
					break;
				}
				remaining -= r;
				md.update(buf, 0, r);
			}
			return Utils.hexString(md.digest());
		}
	}
}