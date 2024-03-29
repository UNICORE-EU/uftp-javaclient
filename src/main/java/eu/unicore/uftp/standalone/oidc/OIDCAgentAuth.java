package eu.unicore.uftp.standalone.oidc;

import java.io.IOException;

import org.apache.hc.core5.http.HttpMessage;
import org.json.JSONObject;

import eu.unicore.services.rest.client.IAuthCallback;

public class OIDCAgentAuth implements IAuthCallback {
	
	private final String account;

	private OIDCAgentProxy ap;

	private String token;

	public OIDCAgentAuth(String account) {
		this.account = account;
	}

	@Override
	public void addAuthenticationHeaders(HttpMessage httpMessage) {
		if(token==null) {
			try{
				retrieveToken();
			}catch(Exception ex){
				throw new RuntimeException(ex);
			}
		}
		httpMessage.setHeader("Authorization","Bearer "+token);
	}

	protected void retrieveToken() throws Exception {
		setupOIDCAgent();
		JSONObject request = new JSONObject();
		request.put("request", "access_token");
		request.put("account", account);
		JSONObject reply = new JSONObject(ap.send(request.toString()));
		boolean success = "success".equalsIgnoreCase(reply.getString("status"));
		if(!success){
			String error = reply.optString("error", reply.toString());
			throw new IOException("Error received from oidc-agent: <"+error+">");
		}
		token = reply.getString("access_token");
	}

	protected void setupOIDCAgent() throws Exception {
		if(!OIDCAgentProxy.isConnectorAvailable())throw new IOException("oidc-agent is not available");
		ap = new OIDCAgentProxy();
	}

	@Override
	public String getType() {
		return "OIDC-AGENT";
	}
}
