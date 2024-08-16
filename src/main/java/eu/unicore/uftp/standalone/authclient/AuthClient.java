
package eu.unicore.uftp.standalone.authclient;

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
}
