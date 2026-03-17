package eu.unicore.uftp.standalone.commands;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import eu.unicore.uftp.client.UFTPSessionClient;
import eu.unicore.uftp.rsync.RsyncStats;
import eu.unicore.uftp.standalone.ClientFacade;
import eu.unicore.uftp.standalone.ConnectionInfoManager;

public class USYNC extends DataTransferCommand {

	@Override
	public String getName() {
		return "sync";
	}

	@Override
	public String getArgumentDescription() {
		return "<primary> <target>";
	}

	@Override
	public String getSynopsis(){
		return "Sync the target file with the given primary file.";
	}

	@Override
	protected void run(ClientFacade client) throws Exception {
		if(fileArgs.length<2) {
			throw new IllegalArgumentException("Missing argument: "+getArgumentDescription());
		}
		RsyncStats stats = null;
		UFTPSessionClient sc = null;
		String primary = fileArgs[0];
		String target = fileArgs[1];
		verbose("sync {} (MASTER) -> {} (SLAVE)", primary, target);	
		try {
			if (ConnectionInfoManager.isRemote(primary) && ConnectionInfoManager.isLocal(target)) {
				sc = client.doConnect(primary);
				Map<String, String> params = client.getConnectionManager().extractConnectionParameters(primary);
				String path = params.get("path");
				stats = rsyncLocalFile(target, path, sc);
			}
			else if (ConnectionInfoManager.isLocal(primary) && ConnectionInfoManager.isRemote(target)) {
				sc = client.doConnect(target);
				Map<String, String> params = client.getConnectionManager().extractConnectionParameters(target);
				String path = params.get("path");
				stats =  rsyncRemoteFile(primary, path, sc);
			}
			else {
				throw new IOException("Need one remote and one local file for sync.");
			}
			verbose("Statistics : {}", stats);
		}
		finally{
			IOUtils.closeQuietly(sc);
		}
	}

	private RsyncStats rsyncRemoteFile(String local, String remote, UFTPSessionClient sc) throws Exception {
		File localPrimary = new File(local);
		if (!localPrimary.isFile()) {
			throw new IllegalArgumentException(local + " is not a file");
		}
		return sc.syncRemoteFile(localPrimary, remote);
	}

	private RsyncStats rsyncLocalFile(String local, String remote, UFTPSessionClient sc) throws Exception {
		File localTarget = new File(local);
		if (!localTarget.isFile()) {
			throw new IllegalArgumentException(local + " is not a file");
		}
		return sc.syncLocalFile(remote, localTarget);
	}

}
