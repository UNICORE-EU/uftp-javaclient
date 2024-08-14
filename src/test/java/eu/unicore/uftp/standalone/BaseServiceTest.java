package eu.unicore.uftp.standalone;

import java.io.File;
import java.net.InetAddress;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import eu.unicore.services.Kernel;
import eu.unicore.uftp.server.UFTPServer;

public abstract class BaseServiceTest {

	protected static Kernel authServer;

	protected static UFTPServer server;
	protected static Thread serverThread;

	protected static int cmdPort = 63321;
	protected static int listenPort = 63320;

	public String getInfoURL() {
		return "http://localhost:9001/rest/auth";
	}

	public String getAuthURL(String filename) {
		return getInfoURL()+"/TEST:"+filename;
	}

	public String getShareURL() {
		return "http://localhost:9001/rest/share/TEST";
	}

	@BeforeAll
	public static void startServers() throws Exception {
		FileUtils.deleteQuietly(new File("target","data"));
		InetAddress host = InetAddress.getByName("localhost");
		server = new UFTPServer(host, cmdPort, host, listenPort);
		serverThread = new Thread(server);
		serverThread.start();
		authServer = new Kernel("src/test/resources/container.properties");
		authServer.startSynchronous();
	}

	@AfterAll
	public static void stopServers() throws Exception {
		server.stop();
		try {
			authServer.shutdown();
		}catch(Exception ex) {}
	}

}
