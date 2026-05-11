package eu.unicore.uftp.standalone.commands;

import java.util.Map;

import org.apache.commons.cli.Option;
import org.apache.commons.io.output.NullOutputStream;
import org.json.JSONObject;

import eu.unicore.services.restclient.utils.UnitParser;
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

	boolean checkPerformance = false;

	@Override
	public String getName() {
		return "info";
	}

	@Override
	public String getSynopsis(){
		return "Gets info about the remote server and runs some checks.";
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
		options.addOption(Option.builder("p").longOpt("performance-check")
				.desc("Run a quick test of the single-thread performance")
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
		verbose("Raw info mode: {}", raw);
		checkSession = !line.hasOption('N');
		verbose("Checking UFTP connect and session creation: {}", checkSession);
		checkPerformance = line.hasOption('p');
		verbose("Running quick performance check: {}", checkPerformance);
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
			if(servers.size()==0) {
				message("No UFTP servers found.");
				return;
			}
			for(String name: servers.keySet()) {
				verbose("Checking UFTP connection to '{}' ...", name);
				try (UFTPSessionClient sc = client.doConnect(servers.get(name)+":/")){
					sc.stat(".");
					message("UFTPD connection to '{}' ({}): OK", name, client.getLastUFTPDHost());
					if(checkPerformance) {
						try {
							sc.stat("/dev/zero");
						}catch(Exception ex) {
							message("Cannot test performance - remote storage does not have '/dev/zero'");
							return;
						}
						message(" --> {}B/s", runPerftest(sc));
					}
				}catch(Exception e) {
					error("FAILED UFTPD tests for '{}': {}", name, e);
				}
			}
		}
	}

	private String runPerftest(UFTPSessionClient sc) throws Exception {
		message("Testing single-stream performance, this can take a few seconds ..."
				+ " (ctrl-c to interrupt)");
		long l = 10*1024*1024;
		UnitParser sizeParser = UnitParser.getCapacitiesParser(0);
		UnitParser rateParser = UnitParser.getCapacitiesParser(1);
		long duration = 0;
		double maxRate = 0;
		do {
			long start = System.currentTimeMillis();
			sc.get("/dev/zero", 0, l, NullOutputStream.INSTANCE);
			duration = System.currentTimeMillis()-start;
			double rate = 1000*(double)l/duration;
			maxRate = Math.max(rate, maxRate);
			verbose("   ... data size {}B --> {}B/s", sizeParser.getHumanReadable(l),
					rateParser.getHumanReadable(rate));
			l = l * (long)Math.pow(2, Math.max(1, (4000-duration)/1000));
		}while(duration<5000);
		return rateParser.getHumanReadable(maxRate);
	}
}
