
package eu.unicore.uftp.standalone.authclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.services.rest.client.UsernamePassword;

/**
 *
 * @author jj
 */
public class AuthClientTest {

    @Test
    public void testGetRequest() {
        System.out.println("getRequest");
        String destinationPath = "/some/path";
        int streamCount = 20;
        String encryptionKey = "encryptionKey";
        boolean compress = true;
        String clientIP = "127.0.0.1";
        String username = "user";
        String password = "pass";
        IAuthCallback auth = new UsernamePassword(username, password);
        AuthserverClient instance = new AuthserverClient("https://server:9991", auth, null);
        AuthRequest result = instance.createRequestObject(destinationPath, streamCount,
        		encryptionKey, "foo", compress, null, clientIP, true);
        assertNotNull(result);
        assertEquals(destinationPath, result.serverPath);
        assertEquals(streamCount, result.streamCount);
        assertEquals(compress, result.compress);
        assertEquals(clientIP, result.client);
        assertTrue(result.persistent);
    }

    @Test
    public void testInfoURL() {
    	String u1 = "https://foo:1234/X/rest/core/storages/WORK:fjslfjlsdf/:/";
    	assertEquals("https://foo:1234/X/rest/core", UNICOREStorageAuthClient.makeInfoURL(u1));
    	String u2 = "https://foo:1234/X/rest/auth/TEST:fjslfjlsdf/:/";
    	assertEquals("https://foo:1234/X/rest/auth", AuthserverClient.makeInfoURL(u2));
    }
    
    @Test
    public void testPathHandling() {
    	Path p1 = Path.of("/foo/bar/.");
    	System.out.println(p1.getParent());	
    }

}
