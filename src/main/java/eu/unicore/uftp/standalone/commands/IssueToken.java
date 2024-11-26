package eu.unicore.uftp.standalone.commands;

import org.apache.commons.cli.Option;
import org.json.JSONObject;

import eu.unicore.services.restclient.jwt.JWTUtils;
import eu.unicore.uftp.standalone.ClientFacade;
import eu.unicore.uftp.standalone.ConnectionInfoManager;
import eu.unicore.uftp.standalone.authclient.AuthClient;
import eu.unicore.uftp.standalone.util.UOptions;

/**
 * ask the auth server to issue a JWT token for
 * later use
 * 
 * @author schuller
 */
public class IssueToken extends Command {

	private int lifetime=-1;

	private boolean limited=false;
	
	private boolean renewable=false;
	
	private boolean inspect=false;
	
	@Override
	public String getName() {
		return "issue-token";
	}

	@Override
	protected UOptions getOptions() {
		UOptions options = super.getOptions();
		options.addOption(Option.builder("l")
				.longOpt("lifetime")
				.desc("Initial lifetime (in seconds) for token.")
				.argName("Lifetime")
				.hasArg()
				.required(false)
				.build());
		options.addOption(Option.builder("L")
				.longOpt("limited")
				.desc("Token should be limited to the issuing server")
				.required(false)
				.build());
		options.addOption(Option.builder("R")
				.longOpt("renewable")
				.desc("Token can be used to get a fresh token.")
				.required(false)
				.build());
		options.addOption(Option.builder("I")
				.longOpt("inspect")
				.desc("Inspect the issued token")
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
		if(line.hasOption("l")){
			lifetime = Integer.valueOf(line.getOptionValue("l"));
		}
		limited = line.hasOption("L");
		renewable = line.hasOption("R");
		inspect = line.hasOption("I");
		AuthClient auth = mgr.getAuthClient(client);
		String token = auth.issueToken(lifetime, limited, renewable);
		if(inspect){
			inspect(token);
		}
		System.out.println(token);		
	}

	private void inspect(String token) throws Exception {
		JSONObject o = JWTUtils.getPayload(token);
		String sub = o.getString("sub");
		String uid = o.optString("uid", null);
		if(uid!=null) {
			sub = sub + " (uid="+uid+")";
		}
		message("Subject:      "+sub);
		message("Lifetime (s): "+(o.getInt("exp")-o.getInt("iat")));
		message("Issued by:    "+o.getString("iss"));
		message("Valid for:    "+o.optString("aud", "<unlimited>"));
		message("Renewable:    "+o.optString("renewable", "no"));
	}

	@Override
	public String getArgumentDescription() {
		return "<Server-URL>";
	}
	@Override
	public String getSynopsis() {
		return "Asks the Authserver to issue a JWT authentication token. "+
				"Lifetime and other properties can be configured.";
	}

}
