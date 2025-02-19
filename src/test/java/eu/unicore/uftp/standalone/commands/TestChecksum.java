package eu.unicore.uftp.standalone.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import eu.unicore.services.restclient.UsernamePassword;
import eu.unicore.uftp.standalone.BaseServiceTest;
import eu.unicore.uftp.standalone.ClientDispatcher;
import eu.unicore.uftp.standalone.ClientFacade;
import eu.unicore.uftp.standalone.ConnectionInfoManager;

public class TestChecksum extends BaseServiceTest {
	
	ClientFacade client ;
	
	@BeforeEach
	public void setup() throws Exception {
		client = new ClientFacade(
				new ConnectionInfoManager(new UsernamePassword("demouser", "test123")));
	}
	
    @Test
    public void testCmd() throws Exception {
    	String[] args = new String[]{ new Checksum().getName(), "-h" };
    	ClientDispatcher._main(args);
    }

    @Test
    public void test1() throws Exception {
    	String src = new File("./pom.xml").getAbsolutePath();
       	String[] args = new String[]{ new Checksum().getName(), "-u", "demouser:test123",
       			getAuthURL(src)};
       	assertEquals(0, ClientDispatcher._main(args));
    }
    
    @Test
    public void testSHA512() throws Exception {
    	String src = new File("./pom.xml").getAbsolutePath();
       	String[] args = new String[]{ new Checksum().getName(), "-u", "demouser:test123",
       			"-a", "SHA-512",
       			getAuthURL(src)};
       	assertEquals(0, ClientDispatcher._main(args));
    }
    
}
