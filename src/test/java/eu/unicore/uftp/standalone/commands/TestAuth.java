package eu.unicore.uftp.standalone.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.utils.Base64;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import eu.unicore.services.restclient.UsernamePassword;
import eu.unicore.services.restclient.sshkey.SSHKey;
import eu.unicore.services.restclient.sshkey.SSHKeyUC;
import eu.unicore.uftp.standalone.BaseServiceTest;
import eu.unicore.uftp.standalone.ClientDispatcher;
import eu.unicore.uftp.standalone.ClientFacade;
import eu.unicore.uftp.standalone.ConnectionInfoManager;
import eu.unicore.uftp.standalone.oidc.OIDCAgentAuth;
import eu.unicore.uftp.standalone.oidc.OIDCAgentProxy;
import eu.unicore.uftp.standalone.ssh.SshKeyHandler;

public class TestAuth extends BaseServiceTest {

	ClientFacade client ;

	@BeforeEach
	public void setup() throws Exception {
		client = new ClientFacade(
				new ConnectionInfoManager(new UsernamePassword("demouser", "test123")));
	}

	@Test
	public void testCmd() throws Exception {
		String[] args = new String[]{ new Auth().getName(), "-h" };
		ClientDispatcher._main(args);
	}

	@Test
	public void testUPAuth() throws Exception {
		String[] args = new String[]{ new Auth().getName(),
				"-u", "demouser:test123", "-v",
				getAuthURL("")
		};
		assertEquals(0, ClientDispatcher._main(args));
	}

	@Test
	public void testTokenAuth() throws Exception {
		String token = "Basic " +
				new String(Base64.encodeBase64(("demouser:test123").getBytes()));
		String[] args = new String[]{ new Auth().getName(),
				"-X", "127.0.0.1",
				"-A", token, "-v", getAuthURL("")
		};
		assertEquals(0, ClientDispatcher._main(args));
	}

	@Test
	public void testOptionsConflict1() throws Exception {
		final String[] args = new String[]{ new Auth().getName(),
				"-u", "demouser", "-A", "foo", "-v",
				getAuthURL("")
		};
		assertThrows(IllegalArgumentException.class, ()->{
			ClientDispatcher._main(args);
		});
		final String[] args2 = new String[]{ new Auth().getName(),
				"-u", "demouser", "-O", "foo", "-v",
				getAuthURL("")
		};
		assertThrows(IllegalArgumentException.class, ()->{
			ClientDispatcher._main(args2);
		});
	}

	@Test
	public void testSSHKeyOldStyle() throws Exception {
		var sh = new SshKeyHandler(new File("src/test/resources/test_id"), "demouser", "123");
		sh.setPreferJWT(false);
		System.setProperty("UFTP_NO_AGENT", "true");
		var auth = sh.getAuthData();
		assertTrue(auth instanceof SSHKeyUC);
		var m = new HttpGet("https://test");
		auth.addAuthenticationHeaders(m);
		var h = m.getHeader("Authorization");
		assertNotNull(h);
	}

	@Test
	public void testSSHKeyJWT() throws Exception {
		var sh = new SshKeyHandler(new File("src/test/resources/test_id"), "demouser", "123");
		sh.setPreferJWT(true);
		System.setProperty("UFTP_NO_AGENT", "true");
		var auth = sh.getAuthData();
		System.out.println(auth);
		assertTrue(auth instanceof SSHKey);
		var m = new HttpGet("https://test");
		auth.addAuthenticationHeaders(m);
		var h = m.getHeader("Authorization");
		assertNotNull(h);
	}

	@Test
	public void testOIDCAgentAuth() throws Exception {
		var a = new OIDCAgentAuth("test");
		a.setAgentProxy(new MockAP());
		var m = new HttpGet("https://test");
		a.addAuthenticationHeaders(m);
		var h = m.getHeader("Authorization");
		assertNotNull(h);
		assertEquals("Bearer some_access_token", h.getValue());
	}

	@Test
	public void testOIDCAgentAuthError() throws Exception {
		var a = new OIDCAgentAuth("wrong_account");
		a.setAgentProxy(new MockAP());
		var m = new HttpGet("https://test");
		assertThrows(RuntimeException.class,()->{
			a.addAuthenticationHeaders(m);
		});
	}

	public static class MockAP extends OIDCAgentProxy {
		@Override
		public String send(String data) {
			JSONObject request = new JSONObject(data);
			JSONObject j = new JSONObject();
			if("test".equals(request.getString("account"))){
				j.put("status", "success");
				j.put("access_token", "some_access_token");
			}
			else {
				j.put("status", "Error: No account configured with that short name");
			}
			return j.toString();
		}
	}
}
