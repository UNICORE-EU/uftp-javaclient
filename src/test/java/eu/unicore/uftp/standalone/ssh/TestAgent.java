package eu.unicore.uftp.standalone.ssh;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import eu.unicore.services.restclient.sshkey.SSHKeyUC;
import eu.unicore.services.restclient.sshkey.SSHUtils;

/**
 * functional test that tests the agent support using real keys
 */
public class TestAgent {
	
	@Test
	@Disabled
	public void testSigningUsingAgent() throws Exception {
		String token = "test123";
		String user = "demouser";
		File privKey = new File(System.getProperty("user.home")+"/.ssh/id_rsa");
		SshKeyHandler handler = new SshKeyHandler(privKey, user, token, true);
		handler.selectIdentity();
		SSHKeyUC authData = (SSHKeyUC)handler.getAuthData();
		String pubKey = FileUtils.readFileToString(new File(System.getProperty("user.home")+"/.ssh/id_rsa.pub"), "UTF-8");
		assertTrue(SSHUtils.validateAuthData(authData, pubKey));
	}

	@Test
	@Disabled
	public void testSigningUsingAgentEd25519() throws Exception {
		String token = "test123";
		String user = "demouser";
		File privKey = new File(System.getProperty("user.home")+"/.ssh/id_ed25519");
		SshKeyHandler handler = new SshKeyHandler(privKey, user, token, true);
		handler.selectIdentity();
		SSHKeyUC authData = (SSHKeyUC)handler.getAuthData();
		String pubKey = FileUtils.readFileToString(new File(System.getProperty("user.home")+"/.ssh/id_ed25519.pub"), "UTF-8");
		assertTrue(SSHUtils.validateAuthData(authData, pubKey));
	}

}
