package eu.unicore.uftp.standalone.oidc;

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
			retrieveToken();
		}
		httpMessage.setHeader("Authorization","Bearer "+token);
	}

	protected void retrieveToken() {
		boolean success = true;
		String error = "";
		try {
			setupOIDCAgent();
			JSONObject request = new JSONObject();
			request.put("request", "access_token");
			request.put("account", account);
			JSONObject reply = new JSONObject(ap.send(request.toString()));
			success = "success".equalsIgnoreCase(reply.getString("status"));
			token = reply.getString("access_token");
			if(!success){
				error = reply.optString("error", reply.toString());
			}
		}catch(Exception ex) {
			throw new RuntimeException("Error accessing oidc-agent", ex);
		}
		if(!success) {
			throw new RuntimeException("Error received from oidc-agent: <"+error+">");
		}
	}

	protected void setupOIDCAgent() {
		if(ap==null) {
			if(!OIDCAgentProxy.isConnectorAvailable())throw new RuntimeException("oidc-agent is not available");
			ap = new OIDCAgentProxy();
		}
	}

	@Override
	public String getType() {
		return "OIDC-AGENT";
	}

	// unit testing
	public void setAgentProxy(OIDCAgentProxy ap) {
		this.ap = ap;
	}
}
