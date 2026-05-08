
package eu.unicore.uftp.standalone.authclient;

import java.util.Collections;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author jj
 */
public interface AuthClient {

	/**
     * perform auth handshake for access to the given path
     */
    public AuthResponse connect(String path) throws Exception;

    public JSONObject getInfo() throws Exception;

    public String parseInfo(JSONObject obj, String infoURL) throws JSONException;

    public String issueToken(long lifetime, boolean limited, boolean renewable) throws Exception;

	public default void setPersistentSessions(boolean persistent) {}

	public default boolean isValidUser(JSONObject info) {
		try {
			return info!=null &&
				"user".equals(info.getJSONObject("client").getJSONObject("role").getString("selected"));
		}catch(JSONException je) {
			return false;
		}
	}

	/**
	 * get the available server(s)
	 * @return map with server name and auth base URL
	 * @throws JSONException
	 */
	public default Map<String,String> getServers() throws Exception {
		return Collections.emptyMap();
	}

}
