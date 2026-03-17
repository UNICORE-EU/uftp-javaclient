package eu.unicore.uftp.standalone.commands;

import org.apache.commons.cli.Option;
import org.json.JSONObject;

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
			System.out.println(j.toString(2));
		}
		else {
			System.out.println(auth.parseInfo(j, uri));
		}
	}

}
