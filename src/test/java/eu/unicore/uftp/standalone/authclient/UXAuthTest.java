
package eu.unicore.uftp.standalone.authclient;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import eu.unicore.services.rest.RestService;
import eu.unicore.services.rest.impl.ApplicationBaseResource;
import eu.unicore.services.utils.deployment.DeploymentDescriptorImpl;
import eu.unicore.uftp.authserver.AuthServiceConfig;
import eu.unicore.uftp.authserver.UFTPDInstance;
import eu.unicore.uftp.server.requests.UFTPSessionRequest;
import eu.unicore.uftp.standalone.BaseServiceTest;
import eu.unicore.uftp.standalone.ClientDispatcher;
import eu.unicore.uftp.standalone.commands.Auth;
import eu.unicore.uftp.standalone.commands.Info;
import eu.unicore.uftp.standalone.commands.IssueToken;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public class UXAuthTest extends BaseServiceTest {

	@BeforeAll
	public static void deployApp() throws Exception {
		DeploymentDescriptorImpl dd = new DeploymentDescriptorImpl();
		dd.setType(RestService.TYPE);
		dd.setImplementation(FakeUXApplication.class);
		dd.setName("core");
		dd.setKernel(authServer);
		authServer.getDeploymentManager().deployService(dd);
	}

	public String getInfoURL() {
		return "https://localhost:9001/rest/core/storages/TEST";
	}

	public String getAuthURL(String filename) {
		return getInfoURL()+":"+filename;
	}

    @Test
    public void testInfo() throws Exception {
    	String[] args = new String[]{ new Info().getName(),
    			"-u", "demouser:test123", "-v",
    			getAuthURL("")
    	};
    	assertEquals(0, ClientDispatcher._main(args));
    }

    @Test
	public void testUPAuth() throws Exception {
		String[] args = new String[]{ new Auth().getName(),
				"-u", "demouser:test123", "-v", "-p",
				getAuthURL("")
		};
		assertEquals(0, ClientDispatcher._main(args));
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

	public static class FakeUXApplication extends Application {
		@Override
		public Set<Class<?>> getClasses() {
			Set<Class<?>>classes=new HashSet<>();
			classes.add(FakeUX.class);
			return classes;
		}
	}

	@Path("/")
	public static class FakeUX extends ApplicationBaseResource {

		@POST
		@Path("/storages/TEST/exports")
		@Produces("application/json")
		@Consumes("application/json")
		public String createExport(String json) throws Exception {
			JSONObject req = new JSONObject(json);
			String secret = req.getJSONObject("extraParameters").getString("uftp.secret");
			UFTPDInstance uftpd = getUFTPD();
			UFTPSessionRequest job = new UFTPSessionRequest(new InetAddress[] {InetAddress.getByName("127.0.0.1")},
					"nobody" ,secret, "."); 
			String reply = uftpd.sendRequest(job);
			System.out.println("--> uftpd reply: "+reply);
			if(reply==null || ! (reply.startsWith("OK") || reply.startsWith("200 OK") )){
				String err="UFTPD server reported an error";
				if(reply!=null){
					err+=": "+reply.trim();
				}
				throw new IOException(err);
			}
			JSONObject j = new JSONObject();
			j.put("uftp.server.host", uftpd.getHost());
			j.put("uftp.server.port", String.valueOf(uftpd.getPort()));
			return j.toString();
		}

		protected UFTPDInstance getUFTPD() throws Exception {
			return kernel.getAttribute(AuthServiceConfig.class).getServer("TEST").
					getUFTPDInstance();
		}

		@GET
		@Path("/")
		@Produces(MediaType.APPLICATION_JSON)
		public Response listAll(@QueryParam("fields")String fields) throws Exception {
			try {
				parsePropertySpec(fields);
				return Response.ok().entity(getJSON().toString()).build();
			}catch(Exception ex) {
				return handleError("", ex, null);
			}
		}

	}

}
