package eu.unicore.uftp.standalone.commands;

import java.io.File;

import org.apache.commons.cli.Option;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import eu.emi.security.authn.x509.helpers.BinaryCertChainValidator;
import eu.unicore.security.Client;
import eu.unicore.services.restclient.BaseClient;
import eu.unicore.services.restclient.utils.UnitParser;
import eu.unicore.uftp.dpc.Utils;
import eu.unicore.uftp.standalone.ClientFacade;
import eu.unicore.uftp.standalone.util.UOptions;
import eu.unicore.util.httpclient.DefaultClientConfiguration;

/**
 * create, update, delete shares
 * 
 * @author schuller
 */
public class Share extends Command {

	/**
	 * environment variable defining the server URL for sharing
	 */
	public static String UFTP_SHARE_URL = "UFTP_SHARE_URL";

	String url;

	boolean raw;

	@Override
	public String getName() {
		return "share";
	}

	@Override
	protected UOptions getOptions() {
		UOptions options = super.getOptions();
		options.addOption(Option.builder("l").longOpt("list")
				.desc("List shares")
				.required(false)
				.get());
		options.addOption(Option.builder("s").longOpt("server")
				.desc("URL to the share service e.g. <https://host:port/SITE/rest/share/NAME>")
				.required(false)
				.hasArg()
				.argName("serverURL")
				.get());
		options.addOption(Option.builder("a").longOpt("access")
				.desc("Allow access for the specified user")
				.required(false)
				.hasArg()
				.argName("userDN")
				.get());
		options.addOption(Option.builder("w").longOpt("write")
				.desc("Allow write access to the shared path")
				.required(false)
				.get());
		options.addOption(Option.builder("d").longOpt("delete")
				.desc("Delete access to the shared path")
				.required(false)
				.get());
		options.addOption(Option.builder("1").longOpt("one-time")
				.desc("Allow only one access to a share (one-time share)")
				.required(false)
				.get());
		options.addOption(Option.builder("L").longOpt("lifetime")
				.desc("Limit lifetime of share (in seconds)")
				.required(false)
				.hasArg()
				.argName("Seconds")
				.get());
		options.addOption(Option.builder("U").longOpt("update")
				.desc("Update share properties, e.g. path")
				.required(false)
				.hasArg()
				.argName("shareID")
				.get());
		options.addOption(Option.builder("R")
				.longOpt("raw")
				.desc("Show info in JSON format as sent by the server.")
				.required(false)
				.get());
		return options;
	}

	@Override
	protected void run(ClientFacade client) throws Exception {
		url = line.getOptionValue("s", Utils.getProperty(UFTP_SHARE_URL, null));
		raw = line.hasOption("R");
		if(url==null) {
			throw new IllegalArgumentException("Must specify share service via '--server <URL>' or environment variable 'UFTP_SHARE_URL'");
		}
		if(line.hasOption("l")){
			listShares(client);
			return;
		}
		else if(line.hasOption("U")) {
			String id = line.getOptionValue("U");
			url+="/"+id;
			update(client);
		}
		else {
			share(client);
		}
	}

	
	public void listShares(ClientFacade client) throws Exception {
		BaseClient bc = getClient(url, client);
		JSONObject shares = bc.getJSON();
		_lastList = shares;
		if(raw) {
			System.out.println(shares.toString(2));
		}
		else {
			JSONArray _shares = shares.optJSONArray("shares");
			if(_shares == null) {
				_shares = new JSONArray();
				try{
					_shares.put(shares.getJSONObject("share"));
				}catch(JSONException e) {
					throw new Exception("No shares found - please check your URL!");
				}
			}
			printHeader();
			for(int i=0; i<_shares.length();i++) {
				listShare(_shares.getJSONObject(i));
			}
		}
	}

	public void share(ClientFacade client) throws Exception {
		if(fileArgs.length==0) {
			throw new IllegalArgumentException("Missing argument: <path>");
		}
		JSONObject req = createRequest();
		BaseClient bc = getClient(url, client);
		String location = bc.create(req);
		boolean delete = line.hasOption("d");
		if(!delete && location!=null){
			bc.setURL(location);
			showNewShareInfo(bc.getJSON());
		}
	}

	public void update(ClientFacade client) throws Exception {
		JSONObject req = createRequest();
		BaseClient bc = getClient(url, client);
		boolean delete = line.hasOption("d");
		try(ClassicHttpResponse r = bc.put(req)){
			if(!delete){
				showNewShareInfo(bc.getJSON());
			}
		}
	}

	protected JSONObject createRequest() {
		boolean anonymous = !line.hasOption("a");
		boolean write = line.hasOption("w");
		boolean delete = line.hasOption("d");
		if(write && delete){
			throw new IllegalArgumentException("Cannot have both --write and --delete");
		}
		if(write && anonymous){
			throw new IllegalArgumentException("Cannot have --write without specifying --access. "
					+ "If you REALLY want anonymous write access, use: --access 'cn=anonymous,o=unknown,ou=unknown'");
		}

		String accessType = write? "WRITE" : "READ" ;
		if(delete)accessType = "NONE";
		String target = anonymous? Client.ANONYMOUS_CLIENT_DN : line.getOptionValue('a');
		String path = null;
		if(fileArgs.length>0) {
			path = fileArgs[0];
			File file = new File(path);
			if(!file.isAbsolute()) {
				file = new File(System.getProperty("user.dir"), file.getPath());
			}
			path = file.getPath();
		}
		boolean onetime = line.hasOption("1");
		long lifetime = UnitParser.getTimeParser(0).getLongValue(line.getOptionValue("L", "0"));
		return createRequest(accessType, target, path, onetime, lifetime);
	}

	protected void showNewShareInfo(JSONObject info) throws Exception {
		System.out.println("Shared to: "+info.getJSONObject("share").getString("http"));
		if(raw)System.out.println(info.toString(2));
		_lastShare = info;
	}

	final String format = " %16s | %10s | %8s | %s";

	private void printHeader() {
		System.out.println(String.format(format, "ID", "Accessed", "Expires", "Path"));
		System.out.println(" -----------------|------------|----------|----------------");
	}

	private UnitParser timeParser = UnitParser.getTimeParser(0);
	
	protected void listShare(JSONObject s) throws Exception {
		if(raw)System.out.println(s.toString(2));
		else {
			boolean isDir = s.getBoolean("directory"); 
			String path = (isDir? "D ": "  ") + s.getString("path");
			Integer _lifetime = s.optIntegerObject("lifetime", null);
			String lifetime = _lifetime!=null? timeParser.getHumanReadable(_lifetime.doubleValue()) : "-";
			System.out.println(String.format(format, s.getString("id"), s.get("accessCount"), lifetime, path));
		}
	}

	@Override
	public String getArgumentDescription() {
		return "<path>"
				+" OR --write --access <target-dn> <path>"
				+" OR --delete <path>"
				+" OR --delete --access <target-dn> <path>"
				;
	}

	public String getSynopsis(){
		return "Create, update and delete shares.";
	}

	protected BaseClient getClient(String url, ClientFacade client) throws Exception {
		DefaultClientConfiguration sec = new DefaultClientConfiguration();
		sec.setValidator(new BinaryCertChainValidator(true));
		sec.setSslAuthn(true);
		sec.setSslEnabled(true);
		return new BaseClient(url, sec, client.getConnectionManager().getAuthData());
	}

	// creates request for creation or update of a share
	protected JSONObject createRequest(String access, String target, String path, boolean onetime, long lifetime) throws JSONException {
		JSONObject o = new JSONObject();
		AccessType t = AccessType.valueOf(access);
		if(path!=null)o.put("path", path);
		if(target!=null)o.put("user", target);
		if(access!=null)o.put("access", t.toString());
		if(onetime) {
			o.put("onetime", "true");
		}
		if(lifetime>0) {
			o.put("lifetime", String.valueOf(lifetime));
		}
		return o;
	}


	public enum AccessType {
		NONE,
		READ,
		WRITE,
		MODIFY
	}

	// for testing
	static JSONObject _lastList;
	static JSONObject _lastShare;

}
