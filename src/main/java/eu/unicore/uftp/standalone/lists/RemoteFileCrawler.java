package eu.unicore.uftp.standalone.lists;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;

import org.apache.commons.io.FilenameUtils;

import eu.unicore.uftp.client.FileInfo;
import eu.unicore.uftp.client.UFTPSessionClient;

/**
 *
 * @author jj
 */
public class RemoteFileCrawler extends FileCrawler {

    private final String path;

    private final String localTarget;
    
    private boolean createLocalDirs = true;

    private final UFTPSessionClient sc;
    
    private final String wildCard;
    
	private final String localSeparator;

	private final boolean isSingleFile;

	private final RecursivePolicy policy;

    public RemoteFileCrawler(String remoteSource, String localTarget, UFTPSessionClient sc, RecursivePolicy policy) throws IOException {
    	this.localSeparator = File.separator;
    	this.sc = sc;
    	this.localTarget = localTarget;
    	this.policy = policy;
        try{
    		if(!remoteSource.endsWith("/")){
    			if(getFileInfo(remoteSource).isDirectory()){
    				remoteSource += "/";
    			}
    		}
    	}catch(Exception ex){}
        this.path = FilenameUtils.getFullPath(remoteSource);
        
        String sourceName = FilenameUtils.getName(remoteSource);
        this.wildCard = (sourceName == null || sourceName.isEmpty())? "*" : sourceName;
        
        if(localTarget!=null && !localTarget.endsWith(localSeparator)){
			if(new File(localTarget).isDirectory()){
				localTarget += localSeparator;
			}
		}
        this.isSingleFile  = !wildCard.contains("*") && !wildCard.contains("?");
    }
    
    private FileInfo getFileInfo(String path) throws IOException{
    	return sc.stat(path);
    }

    Operation cmd;
    
    @Override
    public void crawl(Operation cmd) throws Exception {
    	File t = new File(localTarget);
		if(t.exists() && !t.isDirectory() && !isSingleFile && !isDevNull(localTarget)){
			throw new IOException("Copy target '"+localTarget+"' exists and is not a directory.");
		}
    	this.cmd = cmd;
        boolean recursive = (policy == RecursivePolicy.RECURSIVE);
        crawl(path, localTarget, recursive, false);
    }

    private boolean isDevNull(String dest) {
    	return "/dev/null".equals(dest);
    }
    
    private void crawl(String remoteDir, String destination, boolean recursive, boolean all) throws Exception {
    	Collection<FileInfo>files = sc.getFileInfoList(remoteDir);
    	if(files==null||files.size()==0)return;
        for (FileInfo file : files) {
        	String name = file.getPath();
        	if (all || FilenameUtils.wildcardMatch(name, wildCard)) {
        		if (file.isDirectory()) {
        			if(!name.endsWith("/"))name=name+"/";
        			String newDestination = isDevNull(destination) ? destination :
        				FilenameUtils.separatorsToSystem(FilenameUtils.concat(destination, name));
        			String newSource = FilenameUtils.concat(remoteDir, name);
        			if(createLocalDirs && !isDevNull(newDestination))Files.createDirectories(Paths.get(newDestination));
        			if(recursive){
        				// Unix 'cp' behaviour: when recursing, all subdirectories are copied
        				crawl(newSource, newDestination, recursive, true);
        			}
        			else {
        				System.err.println("uftp: omitting directory '"+name+"'");
        			}
        		} else {
        			cmd.execute(FilenameUtils.separatorsToUnix(FilenameUtils.concat(remoteDir, name)), destination);
        		}
        	}
        }
    }
  
    public boolean isSingleFile(String path) {
    	try{
    		if(sc.stat(path).isDirectory())return false;
    	}catch(Exception ex){}
        String name = FilenameUtils.getName(path);
        if (name == null || name.isEmpty()) {
            return false;
        }
        return !name.contains("*") && !name.contains("?");
    }

    public void setCreateLocalDirs(boolean createLocalDirs) {
    	this.createLocalDirs = createLocalDirs;
    }
}
