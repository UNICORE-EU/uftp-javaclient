package eu.unicore.uftp.standalone.commands;

import java.util.Map;

import org.apache.commons.cli.Option;
import org.json.JSONObject;

import eu.unicore.uftp.client.UFTPSessionClient;
import eu.unicore.uftp.standalone.ClientFacade;
import eu.unicore.uftp.standalone.ConnectionInfoManager;
import eu.unicore.uftp.standalone.authclient.AuthClient;
import eu.unicore.uftp.standalone.util.UOptions;

/**
 * show server info
 *
 * @author schuller
 */
public class Info extends Command {

	boolean raw = false;

	boolean unicoreXStyle = false;

	boolean checkSession = true;

	@Override
	public String getName() {
		return "info";
	}

	@Override
	public String getSynopsis(){
		return "Gets info about the remote server";
	}

	@Override
	protected UOptions getOptions() {
		UOptions options = super.getOptions();
		options.addOption(Option.builder("R").longOpt("raw")
				.desc("Print the JSON response from server")
				.required(false)
				.get());
		options.addOption(Option.builder("N").longOpt("no-check-session")
				.desc("Don't try to establish and check a UFTP session")
				.required(false)
				.get());
		return options;
	}

	@Override
	public String getArgumentDescription() {
		return "<UFTP-Auth-URL>";
	}

	@Override
	public void parseOptions(String[] args) throws Exception {
		super.parseOptions(args);
		raw = line.hasOption('R');
		checkSession = !line.hasOption('N');
	}

	@Override
	protected void run(ClientFacade client) throws Exception {	
		ConnectionInfoManager mgr = client.getConnectionManager();
		if(fileArgs.length==0) {
			throw new IllegalArgumentException("Missing argument: "+getArgumentDescription());
		}
		String uri = fileArgs[0];
		mgr.init(uri+":/");
		AuthClient auth = mgr.getAuthClient(client);
		JSONObject j = auth.getInfo();
		if(raw) {
			message(j.toString(2));
		}
		else {
			message(auth.parseInfo(j, uri));
		}
		if(checkSession && auth.isValidUser(j)) {
			Map<String,String> servers = auth.getServers();
			if(servers.size()>0) {
				message("Checking UFTP connection(s)...");
			}
			for(String name: servers.keySet()) {
				verbose("Checking UFTP connection to '{}' ...", name);
				try (UFTPSessionClient sc = client.doConnect(servers.get(name)+":.")){
					sc.stat(".");
					message(" - UFTP connection to '{}': OK", name);
				}catch(Exception e) {
					error("FAILED UFTP connection to '{}': {}", name, e);
				}
			}		
		}
	}
}
