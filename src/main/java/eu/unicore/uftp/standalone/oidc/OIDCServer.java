package eu.unicore.uftp.standalone.oidc;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.apache.hc.core5.http.HttpMessage;
import org.apache.logging.log4j.message.ParameterizedMessage;

import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.services.restclient.oidc.OIDCProperties;
import eu.unicore.services.restclient.oidc.OIDCServerAuthN;
import eu.unicore.uftp.standalone.authclient.HttpClientFactory;
import eu.unicore.uftp.standalone.util.ConsoleUtils;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * Gets a Bearer token from an OIDC server.
 * 
 * Parameters see {@link OIDCProperties}

 * @author schuller
 */
public class OIDCServer implements IAuthCallback {

	private final boolean verbose;

	private OIDCServerAuthN auth;
	
	public OIDCServer(String propertiesFile, boolean verbose) 
	{
		this.verbose = verbose;
		try (FileInputStream fis=new FileInputStream(new File(propertiesFile))){
			Properties p = new Properties();
			p.load(fis);
			setProperties(p);
		}catch(Exception e) {
			throw new ConfigurationException("Cannot load OIDC properties from <"+propertiesFile+">",e);
		}
	}

	@Override
	public void addAuthenticationHeaders(HttpMessage httpMessage) throws Exception {
		auth.addAuthenticationHeaders(httpMessage);
	}

	public void setProperties(Properties properties)
	{
		OIDCProperties oidcProperties = new OIDCProperties(properties);	
		IClientConfiguration dcc = HttpClientFactory.getClientConfiguration();
		this.auth = new OIDCServerAuthN(
				oidcProperties,
				dcc){
				protected void retrieveToken()throws Exception {
					String password = oidcProperties.getValue(OIDCProperties.PASSWORD);
					if(password==null){
						password = ConsoleUtils.readPassword("OIDC server password:");
						oidcProperties.setProperty(OIDCProperties.PASSWORD, password);
						properties.setProperty("oidc.password", password);
					}
					String otp = oidcProperties.getValue(OIDCProperties.OTP);
					if(otp!=null && otp.equalsIgnoreCase("QUERY")){
						otp = new String(ConsoleUtils.readPassword("OIDC server OTP code:"));
						oidcProperties.setProperty(OIDCProperties.OTP, otp);
					}
					super.retrieveToken();
				}
			};
	}

	public void verbose(String msg, Object ... params) {
		if(verbose) {
			System.out.println(new ParameterizedMessage(msg, params).getFormattedMessage());
		}
	}
}
