package eu.unicore.uftp.standalone.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import eu.unicore.services.restclient.UsernamePassword;
import eu.unicore.uftp.standalone.BaseServiceTest;
import eu.unicore.uftp.standalone.ClientDispatcher;
import eu.unicore.uftp.standalone.ClientFacade;
import eu.unicore.uftp.standalone.ConnectionInfoManager;

public class TestInfo extends BaseServiceTest {
	
	ClientFacade client ;
	
	@BeforeEach
	public void setup() throws Exception {
		client = new ClientFacade(
				new ConnectionInfoManager(new UsernamePassword("demouser", "test123")));
	}
	
    @Test
    public void testUsageAndVersion() throws Exception {
    	String[] args = new String[]{ "-h" };
    	ClientDispatcher._main(args);
    	args = new String[]{ "--version" };
    	ClientDispatcher._main(args);
    }

    @Test
    public void testCmd() throws Exception {
    	String[] args = new String[]{ new Info().getName(), "-h" };
    	ClientDispatcher._main(args);
    }

    @Test
    public void testInfo() throws Exception {
    	String[] args = new String[]{ new Info().getName(),
    			"-u", "demouser:test123",
    			getAuthURL("")
    	};
    	assertEquals(0, ClientDispatcher._main(args));
    	args = new String[]{ new Info().getName(),
    			"-u", "demouser:test123",
    			"-R", getAuthURL("")
    	};
    	assertEquals(0, ClientDispatcher._main(args));
    }

    @Test
    public void testKey() throws Exception {
    	String[] args = new String[]{ new Info().getName(),
    			"-u", "demouser",
    			"--identity", "src/test/resources/test_id",
    			getAuthURL("")
    	};
    	assertEquals(0, ClientDispatcher._main(args));
    }

    @Test
    public void testAnonymousInfo() throws Exception {
    	String[] args = new String[]{ new Info().getName(),
    			"-u", "anonymous",
    			getAuthURL("")
    	};
    	assertEquals(0, ClientDispatcher._main(args));
    }

}
