package eu.unicore.uftp.standalone.ssh;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;

import org.apache.commons.codec.binary.Base64;

import eu.emi.security.authn.x509.helpers.PasswordSupplier;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.services.restclient.sshkey.SSHKey;
import eu.unicore.services.restclient.sshkey.SSHKeyUC;
import eu.unicore.services.restclient.sshkey.SSHUtils;
import eu.unicore.uftp.dpc.Utils;
import eu.unicore.uftp.standalone.util.ConsoleUtils;
import eu.unicore.util.Log;

/**
 * create SSHKey auth info using SSH agent, if possible. 
 * If not, query the key password interactively and 
 * do the signing
 *
 * @author schuller
 */
public class SshKeyHandler {

	private final File privateKey;

	private final String userName;

	private final String token;

	private final boolean verbose;

	// will use agent with a user-selected identity
	private boolean selectIdentity = false;

	private boolean preferJWT = false;

	public SshKeyHandler(File privateKey, String userName, String token, boolean verbose) {
		this.verbose = verbose;
		this.privateKey = privateKey;
		this.userName = userName;
		this.token = token;
		this.preferJWT = Boolean.parseBoolean(Utils.getProperty("UFTP_SSH_PREFER_JWT", "false"));
		if(preferJWT) {
			// can't sign JWT using the agent
			System.setProperty("UFTP_NO_AGENT", "true");
		}
	}

	public IAuthCallback getAuthData() throws Exception {
		IAuthCallback result = null;
		if(SSHAgent.isAgentAvailable()){
			try{
				result = useAgent();
			}catch(Exception ex){
				System.err.println(Log.createFaultMessage("WARNING: Error trying to use SSH agent", ex));
				System.setProperty("UFTP_NO_AGENT", "true");
			}
		}
		if(result==null) {
			result = create();
		}
		return result;
	}

	public void selectIdentity() {
		this.selectIdentity = true;
	}

	public void setPreferJWT(boolean preferJWT) {
		this.preferJWT = preferJWT;
	}

	protected IAuthCallback create() throws GeneralSecurityException, IOException {
		if(privateKey == null || !privateKey.exists()){
			throw new IOException("No private key found!");
		}
		final PasswordSupplier pf = new PasswordSupplier() {
			private char[] _p;
			@Override
			public char[] getPassword() {
				if(_p==null) {
					String pwd = Utils.getProperty("UFTP_PASSWORD", null);
					if(pwd!=null) {
						_p = pwd.toCharArray();
					}
				}
				if(_p==null) {
					_p = ConsoleUtils.readPassword("Enter passphrase for '"+privateKey.getAbsolutePath()+"': ").toCharArray();
				}
				return _p;
			}
		};
		if(preferJWT) {
			return new SSHKey(userName, privateKey, pf);
		}
		else {
			SSHKeyUC sshauth = SSHUtils.createAuthData(privateKey, pf , token);
			sshauth.username = userName;
			return sshauth;
		}
	}

	protected IAuthCallback useAgent() throws Exception {
		SSHAgent agent = new SSHAgent();
		agent.setVerbose(verbose);
		if(selectIdentity) {
			try{
				agent.selectIdentity(privateKey.getAbsolutePath());
			}catch(Exception ex) {
				return null;
			}
		}
		byte[] signature = agent.sign(token);
		SSHKeyUC authData = new SSHKeyUC();
		authData.signature = new String(Base64.encodeBase64(signature));
		authData.token = new String(Base64.encodeBase64(token.getBytes()));
		authData.username= userName;
		return authData;
	}

}
