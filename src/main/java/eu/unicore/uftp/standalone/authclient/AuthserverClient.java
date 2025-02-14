package eu.unicore.uftp.standalone.authclient;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.net.URIBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import eu.unicore.services.restclient.BaseClient;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.uftp.dpc.Utils;
import eu.unicore.uftp.standalone.ClientFacade;
import eu.unicore.uftp.standalone.util.UnitParser;

/**
 *
 * @author jj
 */
public class AuthserverClient implements AuthClient {

	private final String uri;

	private final IAuthCallback authData;

	private final Gson gson = new GsonBuilder().create();

	private final ClientFacade client;

	private boolean persistent = false;

	public AuthserverClient(String authUrl, IAuthCallback authData, ClientFacade client) {
		this.uri = authUrl;
		this.authData = authData;
		this.client = client;
	}

	@Override
	public void setPersistentSessions(boolean persistent) {
		this.persistent = persistent;
	}

	@Override
	public AuthResponse connect(String path) throws Exception {
		byte[] key = client.createEncryptionKey();
		String base64Key = key!=null? Utils.encodeBase64(key) : null;
		String encryptionAlgorithm = client.getEncryptionAlgorithm()!=null ?
				client.getEncryptionAlgorithm().toString() : null;
		AuthRequest authRequest = createRequestObject(path,
				client.getStreams(), base64Key, encryptionAlgorithm, client.isCompress(),
				client.getGroup(), client.getClientIP(), persistent);
		JSONObject request = new JSONObject(gson.toJson(authRequest));
		BaseClient bc = new BaseClient(uri, HttpClientFactory.getClientConfiguration(), authData);
		try(ClassicHttpResponse res = bc.post(request)){
			AuthResponse response = gson.fromJson(EntityUtils.toString(res.getEntity()), AuthResponse.class);
			if(key!=null) {
				response.encryptionKey = key;
				response.encryptionAlgorithm = client.getEncryptionAlgorithm();
			}
			return response;
		}
	}

	@Override
	public JSONObject getInfo() throws Exception {
		String infoURL = makeInfoURL(uri);
		BaseClient bc = new BaseClient(infoURL,
				HttpClientFactory.getClientConfiguration(),
				authData);
		return bc.getJSON();
	}
	
	@Override
	public String issueToken(long lifetime, boolean limited, boolean renewable) throws Exception {
		String tokenURL = makeIssueTokenURL(uri);
		URIBuilder b = new URIBuilder(tokenURL);
		if(lifetime>0)b.addParameter("lifetime", String.valueOf(lifetime));
		if(renewable)b.addParameter("renewable", "true");
		if(limited)b.addParameter("limited", "true");
		BaseClient bc = new BaseClient(b.build().toString(),
				HttpClientFactory.getClientConfiguration(),
				authData);
		try(ClassicHttpResponse res = bc.get(ContentType.WILDCARD)){
			return EntityUtils.toString(res.getEntity());
		}
	}

	@Override
	public String parseInfo(JSONObject info, String url) throws JSONException {
		StringBuilder sb = new StringBuilder();
		String infoURL = makeInfoURL(url);
		try(Formatter f = new Formatter(sb, null)){
			String crlf = System.getProperty("line.separator");
			f.format("Client identity:    %s%s", getID(info),crlf);
			f.format("Client auth method: %s%s", authData.getType(),crlf);
			f.format("Auth server type:   AuthServer v%s%s", getServerVersion(info), crlf);
			for(String key: info.keySet()) {
				if("client".equals(key) || "server".equals(key))continue;
				JSONObject server = info.getJSONObject(key);
				f.format("Server: %s%s", key, crlf);
				f.format("  URL base:         %s/%s:%s", infoURL, key, crlf);
				f.format("  Description:      %s%s", server.optString("description", "N/A"), crlf);
				f.format("  Remote user info: %s%s", getUserInfo(server), crlf);
				f.format("  Sharing support:  %s%s", getSharingSupport(server), crlf);
				long rateLimit = server.optLong("rateLimit", 0);
				if(rateLimit>0) {
					f.format("  Rate limit:       %sB/sec%s", UnitParser.getCapacitiesParser(0).getHumanReadable(rateLimit), crlf);
				}
				long sessionLimit = server.optLong("sessionLimit", 0);
				if(sessionLimit>0) {
					f.format("  Max. sessions:    %s%s", sessionLimit, crlf);
				}
				List<String> reservations = getReservations(server);
				if(reservations.size()>0) {
					f.format("  Reservations:%s", crlf);
					reservations.forEach( x -> f.format("    * %s%s", x, crlf));
				}
				try {
					String serverStatus = getServerStatus(server);
					f.format("  Server status:    %s%s", serverStatus, crlf);
				}catch(JSONException e) {}
			}
		}
		return sb.toString();
	}

	private String getID(JSONObject info) throws JSONException {
		return info.getJSONObject("client").getString("dn");
	}
	
	private String getUserInfo(JSONObject info) throws JSONException {
		StringBuilder sb = new StringBuilder();
		String uid = info.optString("uid", "N/A");
		String gid = info.optString("gid","N/A");
		sb.append("uid=").append(uid);
		sb.append(";gid=").append(gid);
		return sb.toString();
	}
	
	private String getServerStatus(JSONObject info) throws JSONException {
		return info.optString("status", "N/A");
	}

	private String getServerVersion(JSONObject info) throws JSONException {
		return info.getJSONObject("server").optString("version", "???");
	}

	private String getSharingSupport(JSONObject info) throws JSONException {
		boolean enabled = Boolean.parseBoolean(info.getJSONObject("dataSharing").optString("enabled", "N/A"));
		return enabled? "enabled" : "not available";
	}

	private List<String> getReservations(JSONObject info) throws JSONException {
		List<String> res = new ArrayList<>();
		JSONArray reservations = info.optJSONArray("reservations");
		if(reservations!=null) {
			reservations.forEach( x -> res.add(String.valueOf(x)));
		}
		return res;
	}
	
	public static String makeInfoURL(String url) {
		return url.split("/rest/auth")[0]+"/rest/auth";
	}

	public static String makeIssueTokenURL(String url) {
		return url.split("/rest/auth")[0]+"/rest/auth/token";
	}

	AuthRequest createRequestObject(String destinationPath, int streamCount,
			String encryptionKey, String encryptionAlgorithm, boolean compress,
			String group, String clientIP, boolean persistent) {
		AuthRequest ret = new AuthRequest();
		ret.serverPath = destinationPath;
		ret.streamCount = streamCount;
		ret.encryptionKey = encryptionKey;
		ret.encryptionAlgorithm = encryptionAlgorithm;
		ret.compress = compress;
		ret.group = group;
		ret.client = clientIP;
		ret.persistent = persistent;
		return ret;
	}

	public IAuthCallback getAuthData() {
		return authData;
	}

}
