package eu.unicore.uftp.standalone.commands;

import org.apache.commons.cli.Option;

import eu.unicore.uftp.standalone.ClientFacade;
import eu.unicore.uftp.standalone.ConnectionInfoManager;
import eu.unicore.uftp.standalone.authclient.AuthClient;
import eu.unicore.uftp.standalone.authclient.AuthResponse;
import eu.unicore.uftp.standalone.util.UOptions;

/**
 * Only authenticate the user and print out connect info 
 * for use with other tools such as 'ftp' or 'curl'
 *
 * @author schuller
 */
public class Auth extends DataTransferCommand {

	@Override
	public String getName() {
		return "authenticate";
	}

	@Override
	protected UOptions getOptions() {
		UOptions options = super.getOptions();
		options.addOption(Option.builder("p").longOpt("persistent")
				.desc("Allow multiple FTP sessions with the same password (e.g. for 'curlftpfs')")
				.required(false)
				.build());
		return options;
	}

	@Override
	protected void run(ClientFacade client) throws Exception {
		ConnectionInfoManager mgr = client.getConnectionManager();
		if(fileArgs.length==0) {
			throw new IllegalArgumentException("Missing argument: "+getArgumentDescription());
		}
		String uri = fileArgs[0];
		mgr.init(uri);
		AuthClient auth = mgr.getAuthClient(client);
		if(line.hasOption('p')){
			auth.setPersistentSessions(true);
		}
		AuthResponse res = auth.connect(mgr.getBasedir());
		if(!res.success){
			System.out.println("Error: "+res.reason);
		}
		else{
			System.out.println("Connect to: "+res.serverHost+":"+res.serverPort+ " pwd: "+res.secret);	
		}	
	}

	@Override
	public String getArgumentDescription() {
		return "<Server-URL>[:/path/to/initial/directory]";
	}

	@Override
	public String getSynopsis() {
		return "Authenticates the user and prints out connect info. Optionally specify the initial directory for the FTP session.";
	}

}
