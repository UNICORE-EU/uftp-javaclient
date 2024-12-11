
package eu.unicore.uftp.standalone.authclient;

import eu.emi.security.authn.x509.helpers.BinaryCertChainValidator;
import eu.unicore.uftp.dpc.Utils;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.HttpClientProperties;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 *
 * @author jj
 */
public class HttpClientFactory {

	public static IClientConfiguration getClientConfiguration() {
		DefaultClientConfiguration security = new DefaultClientConfiguration();
		security.setValidator(new BinaryCertChainValidator(true));
		security.setSslAuthn(true);
		security.setSslEnabled(true);
		checkHttpProxy(security.getHttpClientProperties());
		return security;
	}

	private static void checkHttpProxy(HttpClientProperties httpProps) {
		String httpProxy = Utils.getProperty("UFTP_HTTP_PROXY", null);
		if(httpProxy==null)return;
		httpProps.setProperty(HttpClientProperties.HTTP_PROXY_HOST, httpProxy);
		String httpProxyPort = Utils.getProperty("UFTP_HTTP_PROXY_PORT", null);
		if(httpProxyPort!=null) {
			httpProps.setProperty(HttpClientProperties.HTTP_PROXY_PORT, httpProxyPort);
		}
	}

}
