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

public class TestULS extends BaseServiceTest {
	
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
    public void testSingle() throws Exception {
    	String file = new File("./pom.xml").getAbsolutePath();
    	String[] args = new String[] {
    			new ULS().getName(),
    			"-H", "-u", "demouser:test123",
    			getAuthURL(file)
    	};
    	assertEquals(0, ClientDispatcher._main(args));
    }

    @Test
    public void testMultiple() throws Exception {
    	String file1 = new File(".").getAbsolutePath();
    	String file2 = testsDir.getAbsolutePath();
    	String[] args = new String[] {
    			new ULS().getName(),
    			"-u", "demouser:test123",
    			getAuthURL(file1), getAuthURL(file2)
    	};
    	assertEquals(0, ClientDispatcher._main(args));
    }

}
