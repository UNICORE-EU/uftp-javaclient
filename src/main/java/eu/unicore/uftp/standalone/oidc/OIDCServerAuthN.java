package eu.unicore.uftp.standalone.oidc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpMessage;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.http.message.StatusLine;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.json.JSONObject;

import eu.unicore.security.wsutil.client.authn.FilePermHelper;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.uftp.standalone.authclient.HttpClientFactory;
import eu.unicore.uftp.standalone.oidc.OIDCProperties.AuthMode;
import eu.unicore.uftp.standalone.util.ConsoleUtils;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.httpclient.HttpUtils;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * Gets a Bearer token from an OIDC server.
 *
 * @author schuller
 */
public class OIDCServerAuthN implements IAuthCallback {

	private final boolean verbose;

	private final OIDCProperties oidc;

	private String token;
	private String refreshToken;
	private long lastRefresh;
	
	public OIDCServerAuthN(String propertiesFile, boolean verbose) 
	{
		this.verbose = verbose;
		try (FileInputStream fis=new FileInputStream(new File(propertiesFile))){
			Properties p = new Properties();
			p.load(fis);
			oidc = new OIDCProperties(p);
		}catch(Exception e) {
			throw new ConfigurationException("Cannot load OIDC properties from <"+propertiesFile+">",e);
		}
		loadRefreshToken();
	}

	@Override
	public void addAuthenticationHeaders(HttpMessage httpMessage) throws Exception {
		if(refreshToken!=null) {
			refreshTokenIfNecessary();
		}
		if(token==null) {
			retrieveToken();
		}
		httpMessage.setHeader("Authorization","Bearer "+token);
	}

	@Override
	public String getType() {
		return "OIDC-SERVER";
	}

	protected void loadRefreshToken() {
		File tokenFile =  new File(oidc.getRefreshTokensFilename());
		try {
			if(tokenFile.exists()) {
				String tokenEndpoint = oidc.getValue(OIDCProperties.TOKEN_ENDPOINT);
				JSONObject tokens = new JSONObject(FileUtils.readFileToString(tokenFile, "UTF-8"));
				JSONObject token = tokens.optJSONObject(tokenEndpoint);
				refreshToken = token.getString("refresh_token");
				verbose("Loaded refresh token for <{}>", tokenEndpoint);
			}
		} catch (Exception ex) {
			verbose("Cannot load refresh token from <{}>", tokenFile);
		}
	}

	protected void storeRefreshToken() {
		if(refreshToken==null)return;
		File tokenFile =  new File(oidc.getRefreshTokensFilename());
		String tokenEndpoint = oidc.getValue(OIDCProperties.TOKEN_ENDPOINT);
		JSONObject tokens = new JSONObject();
		JSONObject token = new JSONObject();
		try (FileWriter writer=new FileWriter(tokenFile)){
			token.put("refresh_token", refreshToken);
			tokens.put(tokenEndpoint, token);
			tokens.write(writer);
			FilePermHelper.set0600(tokenFile);
		}catch(Exception e) {
			verbose("Cannot store refresh token to <{}>", tokenFile);
		}
	}

	protected void refreshTokenIfNecessary() throws Exception {
		long instant = System.currentTimeMillis() / 1000;
		if(instant < lastRefresh + oidc.getIntValue(OIDCProperties.REFRESH_INTERVAL)){
			return;
		}
		lastRefresh = instant;
		String url = oidc.getValue(OIDCProperties.TOKEN_ENDPOINT);
		verbose("Refreshing token from <{}>", url);
		List<BasicNameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("grant_type", "refresh_token"));
		params.add(new BasicNameValuePair("refresh_token", refreshToken));
		try {
			handleReply(executeCall(params));
		}catch(Exception e) {
			verbose("Error refreshing: {}", Log.createFaultMessage("",e));
			token = null;
		}
	}

	protected void retrieveToken() throws Exception {
		List<BasicNameValuePair> params = new ArrayList<>();

		String grantType = oidc.getValue(OIDCProperties.GRANT_TYPE);
		if(grantType!=null){
			params.add(new BasicNameValuePair("grant_type", grantType));
		}
		String username = oidc.getValue(OIDCProperties.USERNAME);
		if(username==null){
			username = ConsoleUtils.readPassword("Username:");
		}
		String password = oidc.getValue(OIDCProperties.PASSWORD);
		if(password==null){
			password = ConsoleUtils.readPassword("Password:");
		}
		String otp = oidc.getValue(OIDCProperties.OTP);
		if(otp!=null && otp.equalsIgnoreCase("QUERY")){
			otp = ConsoleUtils.readPassword("2-factor OTP:");
		}
		params.add(new BasicNameValuePair("username", username));
		params.add(new BasicNameValuePair("password", password));
		String scope = oidc.getValue(OIDCProperties.SCOPE);
		if(scope!=null && scope.length()>0){
			params.add(new BasicNameValuePair("scope", scope));
		}
		handleReply(executeCall(params));
		String url = oidc.getValue(OIDCProperties.TOKEN_ENDPOINT);
		verbose("Retrieved new token from <{}>", url);
	}
	
	private void handleReply(JSONObject reply) throws IOException {
		token = reply.optString("access_token", null);
		refreshToken = reply.optString("refresh_token", null);
		lastRefresh = System.currentTimeMillis() / 1000;
		storeRefreshToken();
	}

	private JSONObject executeCall(List<BasicNameValuePair> params) throws Exception {

		IClientConfiguration dcc = HttpClientFactory.getClientConfiguration();
		String url = oidc.getValue(OIDCProperties.TOKEN_ENDPOINT);
		HttpPost post = new HttpPost(url);
		
		String clientID = oidc.getValue(OIDCProperties.CLIENT_ID);
		String clientSecret = oidc.getValue(OIDCProperties.CLIENT_SECRET);
		AuthMode mode = oidc.getEnumValue(OIDCProperties.AUTH_MODE, AuthMode.class);
		if(AuthMode.BASIC.equals(mode)){
			post.addHeader("Authorization", 
					"Basic "+new String(Base64.encodeBase64((clientID+":"+clientSecret).getBytes())));
		}
		else if(AuthMode.POST.equals(mode)){
			params.add(new BasicNameValuePair("client_id", clientID));
			params.add(new BasicNameValuePair("client_secret", clientSecret));
		}
		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
		post.setEntity(entity);
		HttpClient client = HttpUtils.createClient(url, dcc);
		try(ClassicHttpResponse response = client.executeOpen(null, post, HttpClientContext.create())){
			String body = "";
			try{
				body = EntityUtils.toString(response.getEntity());
			}catch(Exception ex){};
			if(response.getCode()!=200){
				throw new Exception("Error <"+new StatusLine(response)+"> from OIDC server: "+body);
			}
			return new JSONObject(body);
		}
	}

	public void verbose(String msg, Object ... params) {
		if(verbose) {
			System.out.println(new ParameterizedMessage(msg, params).getFormattedMessage());
		}
	}
}
