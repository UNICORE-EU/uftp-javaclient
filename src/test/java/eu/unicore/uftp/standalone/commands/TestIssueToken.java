package eu.unicore.uftp.standalone.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import eu.unicore.uftp.standalone.BaseServiceTest;
import eu.unicore.uftp.standalone.ClientDispatcher;

public class TestIssueToken extends BaseServiceTest {

    @Test
    public void testCmd() throws Exception {
    	String[] args = new String[]{ new IssueToken().getName(), "-h" };
    	ClientDispatcher._main(args);
    }

    @Test
    public void testIssueToken() throws Exception {
    	String[] args = new String[]{ new IssueToken().getName(),
    			"-u", "demouser:test123",
    			"-I", "-l", "3600", "-R", "-L",
    			getAuthURL("")
    	};
    	assertEquals(0, ClientDispatcher._main(args));
    }

}