package eu.unicore.uftp.standalone.commands;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FilenameUtils;

import eu.unicore.uftp.client.FileInfo;
import eu.unicore.uftp.client.UFTPSessionClient;
import eu.unicore.uftp.standalone.ClientFacade;
import eu.unicore.uftp.standalone.ConnectionInfoManager;
import eu.unicore.uftp.standalone.lists.FileCrawler.RecursivePolicy;
import eu.unicore.uftp.standalone.lists.LocalFileCrawler;
import eu.unicore.uftp.standalone.lists.RemoteFileCrawler;
import eu.unicore.uftp.standalone.util.ClientPool;
import eu.unicore.uftp.standalone.util.ClientPool.TransferTask;
import eu.unicore.uftp.standalone.util.ClientPool.TransferTracker;
import eu.unicore.uftp.standalone.util.RangeMode;
import eu.unicore.uftp.standalone.util.ResumeMode;
import eu.unicore.uftp.standalone.util.UOptions;
import eu.unicore.uftp.standalone.util.UnitParser;
import eu.unicore.util.Log;
import eu.unicore.util.Pair;

public class UCP extends DataTransferCommand {

	protected ResumeMode resume = ResumeMode.NONE;

	protected String target;

	protected boolean recurse = false;

	protected boolean preserve = false;

	protected int numClients = 1;

	protected long splitThreshold = -1;

	protected boolean archiveMode = false;

	protected boolean showPerformance = false;

	protected ClientFacade client;

	protected final List<Pair<TransferTask,Future<Boolean>>> tasks = new ArrayList<>();

	@Override
	public String getName() {
		return "cp";
	}

	@Override
	public String getArgumentDescription() {
		return "<source> [<source> ...] <target>";
	}

	@Override
	public String getSynopsis(){
		return "Copy file(s). Wildcards '*' are supported.";
	}

	@Override
	protected UOptions getOptions() {
		UOptions options = super.getOptions();
		options.addOption(Option.builder("R").longOpt("resume")
				.desc("Check existing target file(s) and try to resume")
				.required(false)
				.build());
		options.addOption(Option.builder("r").longOpt("recurse")
				.desc("Recurse into subdirectories, if applicable")
				.required(false)
				.build());
		options.addOption(Option.builder("p").longOpt("preserve")
				.desc("Preserve file modification timestamp")
				.required(false)
				.build());
		options.addOption(Option.builder("t").longOpt("threads")
				.desc("Use specified number of UFTP connections (threads)")
				.required(false)
				.hasArg()
				.build());
		options.addOption(Option.builder("T").longOpt("split-threshold")
				.desc("Minimum size for files to be transferred using multiple threads (with 't')")
				.required(false)
				.hasArg()
				.build());
		options.addOption(Option.builder("a").longOpt("archive")
				.desc("Tell server to interpret data as tar/zip stream and unpack it")
				.required(false)
				.build());
		options.addOption(Option.builder("D").longOpt("show-performance")
				.desc("Show detailed transfer rates during the transfer")
				.required(false)
				.build());
		return options;
	}

	@Override
	public void parseOptions(String[] args) throws ParseException {
		super.parseOptions(args);
		if(fileArgs.length<2){
			throw new IllegalArgumentException("Missing argument: "+getArgumentDescription());
		}
		target = fileArgs[fileArgs.length-1];
		if(fileArgs.length>2) {
			if(!target.endsWith(":") && !target.endsWith("/")){
				target = target + "/";
			}
		}
		recurse = line.hasOption('r');
		preserve = line.hasOption('p');
		boolean doResume = line.hasOption('R');
		if(doResume) {
			resume = ResumeMode.APPEND;
		}
		archiveMode = line.hasOption('a');
		showPerformance = line.hasOption('D');
		if (line.hasOption('t')) {
			numClients = Integer.parseInt(line.getOptionValue('t'));
			if(numClients<1){
				throw new ParseException("Number of threads must be larger than '1'!");
			}
			if (line.hasOption('T')) {
				String thresh = line.getOptionValue('T');
				splitThreshold = (long)UnitParser.getCapacitiesParser(2).getDoubleValue(thresh);
			}
			if(!archiveMode){
				verbose("Using up to <{}> client threads.", numClients);
				if(splitThreshold>0) {
					verbose("Splitting files larger than {}",
							UnitParser.getCapacitiesParser(0).getHumanReadable(splitThreshold));
				}
			}
			if(doResume && line.hasOption('T')) {
				resume=ResumeMode.CHECKSUM;
			}
		}
		if (line.hasOption('B')) {
			initRange(line.getOptionValue('B'));
		}
		if(doResume && line.hasOption('B')){
			resume = ResumeMode.CHECKSUM;
			throw new ParseException("Resume mode is not (yet) supported in combination with a byte range!");
		}
		if(archiveMode){
			verbose("Archive mode ENABLED");
		}
		if(doResume) {
			verbose("Resume mode {} enabled.", resume);
		}
	}

	@Override
	protected void run(ClientFacade client) throws Exception {
		this.client = client;
		int len = fileArgs.length-1;
		long start = System.currentTimeMillis();
		String[] sources = new String[len];
		System.arraycopy(fileArgs, 0, sources, 0, len);
		cp(sources, target);

		long totalSize = 0;
		for(Pair<TransferTask,Future<Boolean>> tp: tasks) {
			Future<Boolean> f = tp.getM2();
			TransferTask t = tp.getM1();
			try{
				f.get();
				totalSize += t.getDataSize();
			}catch(Exception e){
				message(Log.createFaultMessage("ERROR in <"+t.getId()+">", e));
			}
		}
		if(totalSize>0) {
			double rate = 1000* totalSize / (System.currentTimeMillis() - start);
			UnitParser up = UnitParser.getCapacitiesParser(1);
			verbose("\nTotal bytes transferred: {}B", up.getHumanReadable(totalSize));
			verbose("Net transfer rate:       {}B/sec",up.getHumanReadable(rate));
		}
	}

	/**
	 * Entry to the Copy feature
	 */
	public void cp(String[] sources, String destination)
			throws Exception {
		if (!ConnectionInfoManager.isLocal(destination)) {
			startUpload(sources, destination);
		}
		else if (ConnectionInfoManager.isLocal(destination)) {
			for(String source: sources) {
				startDownload(source, destination);
			}
		}
		else {
			String error = String.format("Unable to handle [%s, %s] combination. "
					+ "It is neither upload nor download", Arrays.asList(sources), destination);
			throw new IllegalArgumentException(error);
		}
	}


	private void startUpload(String[] localSources, String destinationURL) 
			throws Exception {
		try(UFTPSessionClient sc = client.doConnect(destinationURL)){
			String remotePath = client.getConnectionManager().getPath();
			if(remotePath.length()==0) {
				remotePath=".";
			}
			RecursivePolicy policy = recurse ? RecursivePolicy.RECURSIVE : RecursivePolicy.NONRECURSIVE;
			try(ClientPool pool = new ClientPool(tasks, numClients, client, destinationURL, verbose, showPerformance)){
				for(String localSource: localSources) {
					LocalFileCrawler fileList = new LocalFileCrawler(localSource, remotePath, sc, policy);
					fileList.crawl( (src, dest)-> executeSingleFileUpload(src, dest, pool, sc));
				}
			}
		}
	}

	private void executeSingleFileUpload(String local, String remotePath, ClientPool pool, UFTPSessionClient sc)
			throws FileNotFoundException, URISyntaxException, IOException {
		String dest = getFullRemoteDestination(local, remotePath);
		if(archiveMode) {
			sc.setType(UFTPSessionClient.TYPE_ARCHIVE);
		}
		if("-".equals(local)){
			try(InputStream is = System.in){
				is.skip(getOffset());
				sc.put(dest, getLength(), is);
			}
		}
		else{
			File file = new File(local);
			long offset = getOffset();
			long total = getLength()>-1? getLength() : file.length();
			if(ResumeMode.APPEND.equals(resume)){
				boolean targetExists = false;
				try{
					offset = sc.getFileSize(dest);
					targetExists = true;
				}catch(IOException ioe) {
					// does not exist
				}
				total = file.length() - offset;
				if(targetExists) {
					if(total>0){
						verbose("<{}>: resuming transfer, have <{}> bytes, remaining <{}>", dest, offset, total);
					}
					else{
						verbose("Nothing to do for <{}>", dest);
						return;
					}
				}
			}
			doUpload(pool, file, dest, offset, total, resume);
		}	
	}

	private void doUpload(ClientPool pool, final File local, final String remotePath,
			final long start, final long total, final ResumeMode resumeMode) throws IOException {
		int numChunks = computeNumChunks(total);
		long chunkSize = total / numChunks;
		long last = start+total-1;
		verbose("Uploading: '{}' --> '{}', start={} length={} numChunks={} chunkSize={}",
				local.getPath(), remotePath, start, total, numChunks, chunkSize);
		int width = String.valueOf(numChunks).length();
		String localFileID = local.getPath();
		String shortID = localFileID+"->"+remotePath;
		TransferTracker ti = new TransferTracker(remotePath, total,
				numChunks, new AtomicInteger(numChunks), new AtomicLong(0));
		for(int i = 0; i<numChunks; i++){
			final long end = last;
			final long first =  i<numChunks-1 ? end - chunkSize : start;
			TransferTask task = getUploadChunkTask(remotePath, localFileID, first, end, resumeMode);
			String id = numChunks>1 ?
					String.format("%s->%s [%0"+width+"d/%d]", localFileID, remotePath, i+1, numChunks):
					shortID;
			task.setId(id);
			task.setTransferTracker(ti);
			task.setDataSize(end-first);
			pool.submit(task);
			last = first - 1;
		}
	}

	private TransferTask getUploadChunkTask(final String remote, String local, final long start, final long end, final ResumeMode resumeMode)
			throws IOException {
		TransferTask task = new TransferTask() {
			@Override
			public void doCall()throws Exception {
				if(ResumeMode.CHECKSUM.equals(resumeMode)) {
					if(checksumMatches(local, remote, start, end-start+1)) {
						verbose("Nothing to do for <{}> [{}:{}]", remote, start, end);
						return;
					}
				}
				final File file = new File(local);
				try(RandomAccessFile raf = new RandomAccessFile(file, "r"))
				{
					InputStream fis = Channels.newInputStream(raf.getChannel());
					UFTPSessionClient sc = getSessionClient();
					if(start>0)raf.seek(start);
					long length = end-start+1;
					long written = sc.put(remote, length, start, fis);
					if(written<length)throw new IOException("Premature end of upload. "
							+ "Wrote <"+written+"> of <"+length+"> bytes.");
					if(preserve && !remote.startsWith("/dev/")){
						Calendar to = Calendar.getInstance();
						to.setTimeInMillis(file.lastModified());
						try{
							sc.setModificationTime(remote, to);
						}catch(Exception ex) {
							verbose("WARNNG: could not set file modification time on remote file '{}'",
									remote);
						}
					}
				}
			}};
			return task;
	}

	private void startDownload(String remote, String destination) 
			throws Exception {
		try(UFTPSessionClient sc = client.doConnect(remote)){
			String path = client.getConnectionManager().getPath();
			RecursivePolicy policy = recurse ? RecursivePolicy.RECURSIVE : RecursivePolicy.NONRECURSIVE;
			try(ClientPool pool = new ClientPool(tasks, numClients, client, remote, verbose, showPerformance)){
				RemoteFileCrawler fileList = new RemoteFileCrawler(path, destination, sc, policy);
				fileList.crawl( (src, dest) -> executeSingleFileDownload(src, dest, pool, sc));
			}
		}
	}

	private void executeSingleFileDownload(String remotePath, String local, ClientPool pool, UFTPSessionClient sc)
			throws Exception {
		String dest = getFullLocalDestination(remotePath, local);
		File file = new File(dest);
		if("-".equals(local)){
			sc.get(remotePath, getOffset(), getLength(), System.out);
			sc.resetDataConnections();
		}else{
			FileInfo fi = sc.stat(remotePath);
			long offset = getOffset();
			long total = getLength()>-1? getLength() : fi.getSize();
			if(ResumeMode.APPEND.equals(resume)){
				if(file.exists()) {
					offset = file.length();
					total = total - offset;
					if(total>0){
						verbose("<{}>: resuming transfer, have <{}> bytes, remaining <{}>", remotePath, offset, total);
					}
					else{
						verbose("Nothing to do for <{}>", remotePath);
						return;
					}
				}
			}
			if(ResumeMode.NONE.equals(resume) && file.exists()) {
				// truncate now to make sure we don't overwrite only part of the file
				try (RandomAccessFile raf = new RandomAccessFile(file, "rw")){
					raf.setLength(0);
				}catch(Exception e) {}
			}
			doDownload(pool, remotePath, dest, fi, offset, total, resume);
		}
	}

	private void doDownload(ClientPool pool, final String remotePath, final String local,
			final FileInfo remoteInfo, final long start, final long total, final ResumeMode resumeMode)
			throws URISyntaxException, IOException {
		int numChunks = computeNumChunks(total);
		long chunkSize = total / numChunks;
		verbose("Downloading: '{}' --> '{}', start={} length={} numChunks={} chunkSize={}",
				remotePath, local, start, total, numChunks, chunkSize);
		int width = String.valueOf(numChunks).length();
		String shortID = remotePath+"->"+local;
		TransferTracker ti = new TransferTracker(local, total,
				numChunks, new AtomicInteger(numChunks), new AtomicLong(0));
		long first = start;
		for(int i = 0; i<numChunks; i++){
			long end = i<numChunks-1? first + chunkSize : start+total-1;
			RangeMode rm = numChunks>1 || !ResumeMode.NONE.equals(resume) ?
					RangeMode.READ_WRITE : rangeMode;
			TransferTask task = getDownloadChunkTask(remotePath, local, first, end,
						remoteInfo, rm, resumeMode);
			String id = numChunks>1 ?
					String.format("%s->%s [%0"+width+"d/%d]", remotePath, local, i+1, numChunks):
					shortID;
			task.setId(id);
			task.setTransferTracker(ti);
			task.setDataSize(end-first);
			pool.submit(task);
			first = end + 1;
		}
	}

	private TransferTask getDownloadChunkTask(String remotePath, String dest, long start, long end, FileInfo fi,
			final RangeMode rangeMode, final ResumeMode resumeMode)
					throws FileNotFoundException, URISyntaxException, IOException{
		TransferTask task = new TransferTask() {
			public void doCall() throws Exception {
				if(ResumeMode.CHECKSUM.equals(resumeMode)) {
					if(checksumMatches(dest, remotePath, start, end-start+1)) {
						verbose("Nothing to do for <{}> [{}:{}]", remotePath, start, end);
						return;
					}
				}
				UFTPSessionClient sc = getSessionClient();
				File file = new File(dest);
				OutputStream fos = null;
				try(RandomAccessFile raf = new RandomAccessFile(file, "rw")){
					if(rangeMode==RangeMode.READ_WRITE){
						raf.seek(start);
					}
					else if(rangeMode==RangeMode.APPEND) {
						raf.seek(file.length());
					}
					fos = Channels.newOutputStream(raf.getChannel());
					long length = end-start+1;
					long received = sc.get(remotePath, start, length, fos);
					if(received<length)throw new IOException("Premature end of stream. "
							+ "Expected: <"+length+"> bytes, received <"+received+">");
					if(preserve && !dest.startsWith("/dev/")){
						try{
							Files.setLastModifiedTime(file.toPath(),FileTime.fromMillis(fi.getLastModified()));
						}catch(Exception ex){}
					}
				}
			}
		};
		return task;
	}

	/**
	 * if user specifies a remote directory, append the source file name
	 */
	String getFullRemoteDestination(String source, String destination) {
		if(destination.endsWith("/") && !"-".equals(source)){
			return FilenameUtils.concat(destination, FilenameUtils.getName(source));
		}
		else return destination;
	}

	/**
	 * Get the final target file name. If the local destination is a directory,
	 * append the source file name
	 */
	String getFullLocalDestination(String source, String destination) {
		String destName = FilenameUtils.getName(destination);
		File destFile = new File(destination);
		if (destName == null || destName.isEmpty() || destFile.isDirectory()) {
			destName = FilenameUtils.getName(source);
			//verify not null?
		}
		return destFile.isDirectory() ?
				new File(destFile, destName).getPath() :
					FilenameUtils.concat(FilenameUtils.getFullPath(destination), destName);
	}

	// we don't want chunks smaller than half of the split threshold
	// otherwise create a few more chunks than we have threads to try 
	// and avoid idle threads
	int computeNumChunks(long dataSize) {
		if(splitThreshold<0 || dataSize<splitThreshold || numClients<2){
			return 1;
		}
		long numChunks = 2 * dataSize / splitThreshold;
		return (int)Math.min(numChunks, (long)(1.25*numClients));
	}

}
