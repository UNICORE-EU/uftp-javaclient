package eu.unicore.uftp.standalone.ssh;

import java.io.File;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import eu.unicore.services.restclient.jwt.JWTUtils;
import eu.unicore.services.restclient.sshkey.SSHUtils;

/**
 * functional test that tests the agent support using real keys
 */
public class TestAgent {
	
	@Test
	@Disabled
	public void testSigningUsingAgentRSA() throws Exception {
		String user = "demouser";
		File privKey = new File(System.getProperty("user.home")+"/.ssh/id_rsa_new");
		SshKeyHandler handler = new SshKeyHandler(privKey, user, true);
		handler.selectIdentity();
		SSHAgentKey authData = (SSHAgentKey)handler.getAuthData();
		String token = authData.getToken();
		Collection<PublicKey> pubKeys = new ArrayList<>();
		pubKeys.add(SSHUtils.readPublicKey(new File(System.getProperty("user.home")+
				"/.ssh/id_rsa_new.pub")));
		JWTUtils.verifyJWTToken(token, pubKeys, null);
	}

	@Test
	@Disabled
	public void testSigningUsingAgentEd25519() throws Exception {
		String user = "demouser";
		File privKey = new File(System.getProperty("user.home")+"/.ssh/id_ed25519");
		SshKeyHandler handler = new SshKeyHandler(privKey, user, true);
		handler.selectIdentity();
		SSHAgentKey authData = (SSHAgentKey)handler.getAuthData();
		String token = authData.getToken();
		Collection<PublicKey> pubKeys = new ArrayList<>();
		pubKeys.add(SSHUtils.readPublicKey(new File(System.getProperty("user.home")+
				"/.ssh/id_ed25519.pub")));
		JWTUtils.verifyJWTToken(token, pubKeys, null);
	}

}
