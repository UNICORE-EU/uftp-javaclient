package eu.unicore.uftp.standalone.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.io.IOUtils;
import org.jline.terminal.Terminal;
import org.jline.terminal.Terminal.Signal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp.Capability;

import eu.unicore.uftp.client.UFTPProgressListener2;
import eu.unicore.uftp.standalone.util.ClientPool.TransferTracker;


/**
 * Show multi-threaded performance / progress
 * when transferring multiple files and/or chunks.
 *
 * @author schuller
 */
public class MultiProgressBar implements UFTPProgressListener2, Closeable {

	private final Terminal terminal;	
	private int width;

	private final UnitParser rateParser=UnitParser.getCapacitiesParser(1);
	private final UnitParser sizeParser=UnitParser.getCapacitiesParser(0);
	
	// data indexed per thread
	private final int maxThreads;
	private final long[] threadIds;
	private final long startedAt[];
	private final long have[];
	private final double rate[];
	private final TransferTracker[] trackers;

	// current list of active transfers
	private final CopyOnWriteArrayList<TransferTracker> runningTransfers =
			new CopyOnWriteArrayList<>();

	public MultiProgressBar(int maxThreads) throws IOException {
		this.maxThreads = maxThreads;
		this.threadIds = new long[maxThreads];
		this.startedAt = new long[maxThreads];
		this.have = new long[maxThreads];
		this.rate = new double[maxThreads];
		this.trackers = new TransferTracker[maxThreads];
		this.terminal = TerminalBuilder.terminal();
		width = terminal.getWidth();
		terminal.handle(Signal.WINCH, (sig)->{
			// window changed
			synchronized (MultiProgressBar.this) {
				width = terminal.getWidth();
			}
		});
	}

	/**
	 * register a new file transfer 
	 * NOTE: this must be called from a worker thread!
	 */
	public void registerNew(TransferTracker tracker){
		int i = getThreadIndex();
		rate[i] = 0;
		have[i] = 0;
		trackers[i] = tracker;
		startedAt[i] = System.currentTimeMillis();
		runningTransfers.addIfAbsent(tracker);
	}

	@Override
	public void setTransferSize(long size){
		// NOP
	}

	public synchronized void notifyTotalBytesTransferred(long total){
		if(terminal==null || total<0)return;
		int i = getThreadIndex();
		updateRate(i);
		trackers[i].addBytesTransferred(total-have[i]);
		have[i]=total;
		output();
	}

	public void closeCurrentThread(){
		closeCurrentThread(null);
	}

	public void closeCurrentThread(String message){
		int index = getThreadIndex();
		TransferTracker tt = trackers[index];
		cleanup(index);
		int active = tt.chunkCounter.decrementAndGet();
		String line;
		if(active==0){
			try {
				if(message==null) {
					double rate = 1000*(double)tt.size / (System.currentTimeMillis()-tt.start.get());
					String rateS = rateParser.getHumanReadable(tt.size)+"B " +
								   sizeParser.getHumanReadable(rate)+"B/s";
					int w = width - 15 - rateS.length();
					if(w>3) {
						String path = w>tt.file.length() ?
								tt.file :
								"..." + tt.file.substring(tt.file.length()-w+3);
						line = String.format("%4s %-"+w+"s %s 100%%",
								tt.transferID, path, rateS);
					} else line="...";
				}
				else {
					line = String.format("%s", message);
				}
				synchronized(this){
					for(int i=0;i<nLines; i++)terminal.puts(Capability.cursor_up);
					terminal.puts(Capability.carriage_return);
					terminal.writer().write(line);
					terminal.writer().write(lineSep);
					terminal.writer().write(lineSep);
					terminal.flush();
					nLines=1+line.length()/width;
				}
				runningTransfers.remove(tt);
			}catch(Exception ex) {}
		}
	}

	@Override
	public void close(){
		System.out.println();
		IOUtils.closeQuietly(terminal);
	}


	private int getThreadIndex(){
		long currentId = Thread.currentThread().getId();
		for(int i=0; i<maxThreads; i++){
			long threadId = threadIds[i];
			if(threadId==0){
				threadIds[i] = currentId;
				return i;
			}
			else if(threadId==currentId){
				return i;
			}
		}
		throw new IllegalStateException();
	}

	private void updateRate(int i){
		rate[i]=1000*(double)have[i]/(System.currentTimeMillis()-startedAt[i]);
	}

	// number of output lines we printed last time
	private volatile int nLines = 0;
	private String lineSep = System.getProperty("line.separator");

	private void output() {
		if(terminal==null)return;
		List<String>lines = new ArrayList<>();
		for(TransferTracker tt: runningTransfers) {
			int percent = (int)(100.0*tt.getBytesTransferred()/tt.size);
			String ptp = getPerThreadPerformance(tt);
			int w = width - 15 - ptp.length();
			if(w>3) {
				String path = w>tt.file.length() ?
						tt.file :
						"..."+tt.file.substring(tt.file.length()-w+3);
				lines.add(
						String.format("%4s %-"+w+"s %s %3d%%", tt.transferID, path, ptp, percent)
						);
			} else lines.add("...");
		}
		for(int i=0;i<nLines; i++)terminal.puts(Capability.cursor_up);
		terminal.puts(Capability.carriage_return);
		for(String line: lines) {
			terminal.writer().write(line);
			terminal.writer().write(lineSep);
		}
		terminal.flush();
		nLines = lines.size();
	}

	private String getPerThreadPerformance(TransferTracker tt){
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<maxThreads;i++){
			if(!tt.equals(trackers[i]))continue;
			if(rate[i]>0){
				sb.append(String.format("%sB/s ", rateParser.getHumanReadable(rate[i])));
			}
			else{
				sb.append("------- ");
			}
		}
		return sb.toString();
	}

	private void cleanup(int i) {
		rate[i] = 0;
		have[i] = 0;
		trackers[i] = null;
		startedAt[i]=0;
	}

}
