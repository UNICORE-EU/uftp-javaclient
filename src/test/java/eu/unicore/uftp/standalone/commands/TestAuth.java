package eu.unicore.uftp.standalone.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.hc.client5.http.utils.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import eu.unicore.services.restclient.UsernamePassword;
import eu.unicore.uftp.standalone.BaseServiceTest;
import eu.unicore.uftp.standalone.ClientDispatcher;
import eu.unicore.uftp.standalone.ClientFacade;
import eu.unicore.uftp.standalone.ConnectionInfoManager;

public class TestAuth extends BaseServiceTest {

	ClientFacade client ;

	@BeforeEach
	public void setup() throws Exception {
		client = new ClientFacade(
				new ConnectionInfoManager(new UsernamePassword("demouser", "test123")));
	}

	@Test
	public void testCmd() throws Exception {
		String[] args = new String[]{ new Auth().getName(), "-h" };
		ClientDispatcher._main(args);
	}

	@Test
	public void testUPAuth() throws Exception {
		String[] args = new String[]{ new Auth().getName(),
				"-u", "demouser:test123", "-v",
				getAuthURL("")
		};
		assertEquals(0, ClientDispatcher._main(args));
	}

	@Test
	public void testTokenAuth() throws Exception {
		String token = "Basic " +
				new String(Base64.encodeBase64(("demouser:test123").getBytes()));
		String[] args = new String[]{ new Auth().getName(),
				"-X", "127.0.0.1",
				"-A", token, "-v", getAuthURL("")
		};
		assertEquals(0, ClientDispatcher._main(args));
	}

	@Test
	public void testOptionsConflict1() throws Exception {
		final String[] args = new String[]{ new Auth().getName(),
				"-u", "demouser", "-A", "foo", "-v",
				getAuthURL("")
		};
		assertThrows(IllegalArgumentException.class, ()->{
			ClientDispatcher._main(args);
		});
		final String[] args2 = new String[]{ new Auth().getName(),
				"-u", "demouser", "-O", "foo", "-v",
				getAuthURL("")
		};
		assertThrows(IllegalArgumentException.class, ()->{
			ClientDispatcher._main(args2);
		});
	}

}
