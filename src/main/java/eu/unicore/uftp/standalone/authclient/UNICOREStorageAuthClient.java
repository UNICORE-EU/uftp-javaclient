package eu.unicore.uftp.standalone.authclient;

import java.util.Formatter;
import java.util.Random;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.net.URIBuilder;
import org.json.JSONException;
import org.json.JSONObject;

import eu.unicore.services.rest.client.BaseClient;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.uftp.dpc.Utils;
import eu.unicore.uftp.standalone.ClientFacade;

/**
 * create a session using the UNICORE Storage API
 * 
 * @author schuller
 */
public class UNICOREStorageAuthClient implements AuthClient {

	private final String uri;

	private final IAuthCallback authData;

	private final ClientFacade client;

	public UNICOREStorageAuthClient(String authUrl, IAuthCallback authData, ClientFacade client) {
		this.uri = authUrl;
		this.authData = authData;
		this.client = client;
	}

	@Override
	public AuthResponse connect(String path) throws Exception {
		BaseClient bc = new BaseClient(uri, HttpClientFactory.getClientConfiguration(), authData);
		byte[] key = client.createEncryptionKey();
		String base64Key = key!=null? Utils.encodeBase64(key) : null;
		JSONObject request = createRequestObject(path, base64Key, client.isCompress(), client.getClientIP(), true);
		try(ClassicHttpResponse res = bc.post(request)){
			JSONObject reply = bc.asJSON(res);
			AuthResponse response = new AuthResponse(true, "OK");
			response.serverHost = reply.getString("uftp.server.host");
			response.serverPort = Integer.parseInt(reply.getString("uftp.server.port"));
			if(key!=null) {
				response.encryptionKey = key;
				response.encryptionAlgorithm = client.getEncryptionAlgorithm();
			}
			response.secret = request.getJSONObject("extraParameters").getString("uftp.secret");
			return response;
		}
	}

	@Override
	public JSONObject getInfo() throws Exception {
		String infoURL = makeInfoURL(uri);
		BaseClient bc = new BaseClient(infoURL, HttpClientFactory.getClientConfiguration(), authData);
		return bc.getJSON();
	}
	
	static String crlf = System.getProperty("line.separator");
	
	
	@Override
	public String parseInfo(JSONObject info, String infoURL) throws JSONException {
		StringBuilder sb = new StringBuilder();
		try(Formatter f = new Formatter(sb, null)){
			f.format("Client identity:    %s%s", getID(info),crlf);
			f.format("Client auth method: %s%s", authData.getType(),crlf);
			f.format("Auth server type:   UNICORE/X v%s%s", getServerVersion(info), crlf);
			f.format("Remote user info:   %s%s", getUserInfo(info), crlf);
			try {
				String serverStatus = getServerStatus(info);
				f.format("UFTP Server status: %s%s", serverStatus, crlf);
			}catch(JSONException e) {}
		}
		return sb.toString();
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

	private String getID(JSONObject info) throws JSONException {
		return info.getJSONObject("client").getString("dn");
	}
	
	private String getUserInfo(JSONObject info) throws JSONException {
		JSONObject client = info.getJSONObject("client");
		StringBuilder sb = new StringBuilder();
		String role = client.getJSONObject("role").getString("selected");
		String uid = client.getJSONObject("xlogin").optString("UID", "N/A");
		String gid = client.getJSONObject("xlogin").optString("group","N/A");
		sb.append("uid=").append(uid);
		sb.append(";gid=").append(gid);
		sb.append(";role=").append(role);
		return sb.toString();
	}


	private String getServerStatus(JSONObject info) throws JSONException {
		JSONObject status = info.getJSONObject("server").getJSONObject("externalConnections");
		for(String key: status.keySet()) {
			if(!key.startsWith("UFTPD"))continue;
			return status.getString(key);
		}
		return "N/A";
	}

	private String getServerVersion(JSONObject info) throws JSONException {
		return info.getJSONObject("server").optString("version", "???");
	}
	
	public static String makeInfoURL(String url) {
		return url.split("/rest/core")[0]+"/rest/core";
	}

	public static String makeIssueTokenURL(String url) {
		return url.split("/rest/core")[0]+"/rest/core/token";
	}

	private JSONObject createRequestObject(String path, String encryptionKey, boolean compress, String clientIP, boolean persistent) throws JSONException {
		JSONObject ret = new JSONObject();
		ret.put("file", path);
		ret.put("protocol", "UFTP");
		JSONObject params = new JSONObject();
		ret.put("extraParameters", params);
		params.put("uftp.secret", generateSecret());
		if(clientIP!=null) {
			params.put("uftp.client.host", clientIP);
		}
		if(encryptionKey!=null) {
			params.put("uftp.encryption", "true");
		}
		params.put("uftp.compression", compress);
		params.put("uftp.persistent", persistent);
		return ret;
	}

	public IAuthCallback getAuthData() {
		return authData;
	}
	
	private static final char[] chars = "abcdefghijklmnopqrstuvwxyz".toCharArray();

	static String generateSecret() {
		StringBuilder sb = new StringBuilder();
		Random random = new Random();
		for (int i = 0; i < 20; i++) {
			char c = chars[random.nextInt(chars.length)];
			sb.append(c);
		}
		String output = sb.toString();
		return output;
	}
}
