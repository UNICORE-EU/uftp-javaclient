package eu.unicore.uftp.standalone.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import eu.unicore.services.restclient.UsernamePassword;
import eu.unicore.uftp.standalone.BaseServiceTest;
import eu.unicore.uftp.standalone.ClientDispatcher;
import eu.unicore.uftp.standalone.ClientFacade;
import eu.unicore.uftp.standalone.ConnectionInfoManager;

public class TestRCP extends BaseServiceTest {
	
	ClientFacade client ;
	File testsDir;

	@BeforeEach
	public void setup() throws Exception {
		client = new ClientFacade(
				new ConnectionInfoManager(new UsernamePassword("demouser", "test123")));
		testsDir = new File("target", "testdata");
		FileUtils.deleteQuietly(testsDir);
		testsDir.mkdirs();
	}

    @Test
    public void testCmd() throws Exception {
    	String[] args = new String[]{ new URCP().getName(), "-h" };
    	ClientDispatcher._main(args);
    }

    @Test
    public void testRCP1() throws Exception {
    	String src = new File("./pom.xml").getAbsolutePath();
    	String target = new File(testsDir, "test.dat").getAbsolutePath();
    	String[] args = new String[]{ new URCP().getName(), "-u", "demouser:test123",
    			"-n", "2", "-E",
    			getAuthURL(src), getAuthURL(target)
    	};
    	assertEquals(0, ClientDispatcher._main(args));
    	Thread.sleep(5000);
    	// TBD status check
    }

}
