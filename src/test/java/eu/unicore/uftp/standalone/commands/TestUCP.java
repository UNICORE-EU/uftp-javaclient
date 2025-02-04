package eu.unicore.uftp.standalone.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import eu.unicore.services.restclient.UsernamePassword;
import eu.unicore.uftp.dpc.Utils;
import eu.unicore.uftp.standalone.BaseServiceTest;
import eu.unicore.uftp.standalone.ClientDispatcher;
import eu.unicore.uftp.standalone.ClientFacade;
import eu.unicore.uftp.standalone.ConnectionInfoManager;

public class TestUCP extends BaseServiceTest {

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
		String[] args = new String[]{ new UCP().getName(), "-h" };
		ClientDispatcher._main(args);
	}

	@Test
	public void testSingleUpload() throws Exception {
		String src = "./pom.xml";
		String target = testsDir.getAbsolutePath();
		String[] args = new String[]{ new UCP().getName(), "-u", "demouser:test123",
				"--preserve",
				src, getAuthURL(target)
		};
		assertEquals(0, ClientDispatcher._main(args));
		assertEquals(Utils.md5(new File(src)), Utils.md5(new File(testsDir, "pom.xml")));
		assertEquals(new File(src).lastModified()/1000,
				new File(testsDir, "pom.xml").lastModified()/1000);
	}

	@Test
	public void testRangedUpload() throws Exception {
		String src = "./pom.xml";
		String target = testsDir.getAbsolutePath();
		String[] args = new String[]{ new UCP().getName(), "-u", "demouser:test123",
				"-B0-10",
				src, getAuthURL(target)
		};
		assertEquals(0, ClientDispatcher._main(args));
		assertEquals(11, new File(testsDir,"/pom.xml").length());
	}

	@Test
	public void testResumeUpload() throws Exception {
		String src = "./pom.xml";
		String target = testsDir.getAbsolutePath();
		String[] args = new String[]{ new UCP().getName(), "-u", "demouser:test123",
				"-B0-10", "-v",
				src, getAuthURL(target)
		};
		assertEquals(0, ClientDispatcher._main(args));
		assertEquals(11, new File(testsDir,"/pom.xml").length());
		args = new String[]{ new UCP().getName(), "-u", "demouser:test123",
				"--resume", "-v",
				src, getAuthURL(target)
		};
		assertEquals(0, ClientDispatcher._main(args));
		assertEquals(Utils.md5(new File(src)), Utils.md5(new File(testsDir, "pom.xml")));
	}

	@Test
	public void testSingleUploadWithRename() throws Exception {
		String src = "./pom.xml";
		String target = new File(testsDir, "testfile.xml").getAbsolutePath();
		String[] args = new String[]{ new UCP().getName(), "-u", "demouser:test123",
				src, getAuthURL(target)
		};
		assertEquals(0, ClientDispatcher._main(args));
		assertEquals(Utils.md5(new File(src)), Utils.md5(new File(target)));
	}

	@Test
	public void testMultipleUploads() throws Exception {
		for(int i = 0; i<3; i++) {
			FileUtils.writeStringToFile(new File(testsDir, "inputs/file"+i), "test123"+i, "UTF-8");
		}
		// dir will not be copied in non-recursive mode
		File skipMe = new File(testsDir, "inputs/skipme");
		FileUtils.forceMkdir(skipMe);
		String src = testsDir.getAbsolutePath()+"/inputs/*";
		File targetF = new File(testsDir, "uploads");
		FileUtils.forceMkdir(targetF);
		String target = testsDir.getAbsolutePath();
		String[] args = new String[]{ new UCP().getName(), "-u", "demouser:test123",
				"-v", "-D", src, getAuthURL(target)
		};
		assertEquals(0, ClientDispatcher._main(args));
		for(int i = 0; i<3; i++) {
			assertEquals(Utils.md5(new File(testsDir, "inputs/file"+i)),
					Utils.md5(new File(target, "file"+i)));
		}
	}

	@Test
	public void testUploadsRecursive() throws Exception {
		for(int i = 0; i<3; i++) {
			FileUtils.writeStringToFile(new File(testsDir, "inputs/file"+i), "test123"+i, "UTF-8");
			FileUtils.writeStringToFile(new File(testsDir, "inputs/subdir/file"+i), "test123"+i, "UTF-8");
		}
		File targetF = new File(testsDir, "uploads");
		FileUtils.forceMkdir(targetF);
		String src = testsDir.getAbsolutePath()+"/inputs/*";
		String target = targetF.getAbsolutePath();
		String[] args = new String[]{ new UCP().getName(), "-u", "demouser:test123",
				"-v", "-D", "-r", src, getAuthURL(target)
		};
		assertEquals(0, ClientDispatcher._main(args));
		for(int i = 0; i<3; i++) {
			assertEquals(Utils.md5(new File(testsDir, "inputs/file"+i)),
					Utils.md5(new File(targetF, "file"+i)));
			assertEquals(Utils.md5(new File(testsDir, "inputs/subdir/file"+i)),
					Utils.md5(new File(target, "subdir/file"+i)));
		}
	}

	@Test
	public void testUploadSkipDir() throws Exception {
		FileUtils.writeStringToFile(new File(testsDir, "inputs/file1"), "test123", "UTF-8");
		String src = testsDir.getAbsolutePath()+"/inputs/";
		File targetF = new File(testsDir, "uploads");
		FileUtils.forceMkdir(targetF);
		String target = targetF.getAbsolutePath();
		String[] args = new String[]{ new UCP().getName(), "-u", "demouser:test123",
				"-v", src, getAuthURL(target)
		};
		assertEquals(0, ClientDispatcher._main(args));
		for(int i = 0; i<3; i++) {
			assertFalse(new File(testsDir, "uploads/file1").exists());
		}
	}

	@Test
	public void testUploadDir() throws Exception {
		for(int i = 0; i<3; i++) {
			FileUtils.writeStringToFile(new File(testsDir, "files/file"+i), "test123"+i, "UTF-8");
		}
		String src = testsDir.getAbsolutePath()+"/files/";
		File targetF = new File(testsDir, "uploads");
		FileUtils.forceMkdir(targetF);
		String target = targetF.getAbsolutePath();
		String[] args = new String[]{ new UCP().getName(), "-u", "demouser:test123",
				"-v", "-r", src, getAuthURL(target)
		};
		System.out.println(Arrays.asList(args));
		assertEquals(0, ClientDispatcher._main(args));
		for(int i = 0; i<3; i++) {
			assertTrue(new File(testsDir, "uploads/files/file"+i).exists());
		}
	}

	@Test
	public void testFailingDownload() throws Exception {
		String nonExistingFile = "/some/path/file1.dat";
		UCP ucp = new UCP();
		ucp.client = client;
		assertThrows(IOException.class, ()-> {
			ucp.cp(new String[]{getAuthURL(nonExistingFile)}, "/tmp/filexxx.dat"); 
		});
	}

	@Test
	public void testRenameDownload() throws Exception {
		String src =  new File("./pom.xml").getAbsolutePath();
		String target = testsDir.getAbsolutePath()+"/dl1.xml";
		String[] args = new String[]{ new UCP().getName(), "-u", "demouser:test123",
				getAuthURL(src), target
		};
		assertEquals(0, ClientDispatcher._main(args));
		assertEquals(Utils.md5(new File(src)), Utils.md5(new File(target)));
	}

	@Test
	public void testSingleDownload() throws Exception {
		String src =  new File("./pom.xml").getAbsolutePath();
		String target = testsDir.getAbsolutePath();
		String[] args = new String[]{ new UCP().getName(), "-u", "demouser:test123",
				getAuthURL(src), target
		};
		assertEquals(0, ClientDispatcher._main(args));
		assertEquals(Utils.md5(new File(src)), Utils.md5(new File(testsDir,"/pom.xml")));
	}

	@Test
	public void testDownloadToStdout() throws Exception {
		String src =  new File("./pom.xml").getAbsolutePath();
		String target = "-";
		String[] args = new String[]{ new UCP().getName(), "-u", "demouser:test123",
				"-B-20",
				getAuthURL(src), target
		};
		assertEquals(0, ClientDispatcher._main(args));
	}

	@Test
	public void testRangedDownload() throws Exception {
		String src =  new File("./pom.xml").getAbsolutePath();
		String target = testsDir.getAbsolutePath();
		String[] args = new String[]{ new UCP().getName(), "-u", "demouser:test123",
				"-B0-10",
				getAuthURL(src), target
		};
		assertEquals(0, ClientDispatcher._main(args));
		assertEquals(11, new File(testsDir,"/pom.xml").length());
	}

	@Test
	public void testResumeDownload() throws Exception {
		String src =  new File("./pom.xml").getAbsolutePath();
		String target = new File(testsDir,"copy.xml").getAbsolutePath();
		String[] args = new String[]{ new UCP().getName(), "-u", "demouser:test123",
				"-B0-5",
				getAuthURL(src), target
		};
		assertEquals(0, ClientDispatcher._main(args));

		args = new String[]{ new UCP().getName(), "-u", "demouser:test123",
				"--resume", "-v",
				getAuthURL(src), target
		};
		assertEquals(0, ClientDispatcher._main(args));
		assertEquals(Utils.md5(new File(src)), Utils.md5(new File(testsDir,"copy.xml")));
	}

	@Test
	public void testMultipleDownload() throws Exception {
		for(int i = 0; i<3; i++) {
			FileUtils.writeStringToFile(new File(testsDir, "inputs/file"+i), "test123"+i, "UTF-8");
		}
		FileUtils.writeStringToFile(new File(testsDir, "inputs/not_you"), "nope", "UTF-8");
		FileUtils.forceMkdir(new File(testsDir, "downloads"));
		String src = new File(testsDir,"inputs").getAbsolutePath()+"/file*";
		String target = new File(testsDir,"downloads/").getAbsolutePath();
		String[] args = new String[]{ new UCP().getName(), "-u", "demouser:test123",
				"-t", "2", "-T", "256k", "-v", "-r",
				getAuthURL(src), target
		};
		assertEquals(0, ClientDispatcher._main(args));
		for(int i = 0; i<3; i++) {
			assertEquals(Utils.md5(new File(testsDir, "inputs/file"+i)),
					Utils.md5(new File(testsDir, "downloads/file"+i)));
		}
		assertFalse(new File(testsDir, "downloads/not_you").exists());
	}

	@Test
	public void testDownloadRecursive() throws Exception {
		for(int i = 0; i<3; i++) {
			FileUtils.writeStringToFile(new File(testsDir, "inputs/file"+i), "test123"+i, "UTF-8");
		}
		for(int i = 0; i<3; i++) {
			FileUtils.writeStringToFile(new File(testsDir, "inputs/subdir/file"+i), "test123"+i, "UTF-8");
		}
		FileUtils.forceMkdir(new File(testsDir, "downloads"));
		String src = new File(testsDir,"inputs").getAbsolutePath()+"/*";
		String target = new File(testsDir,"downloads/").getAbsolutePath();
		String[] args = new String[]{ new UCP().getName(), "-u", "demouser:test123",
				"-t", "2", "-v", "-r",
				getAuthURL(src), target
		};
		assertEquals(0, ClientDispatcher._main(args));
		for(int i = 0; i<3; i++) {
			assertEquals(Utils.md5(new File(testsDir, "inputs/file"+i)),
					Utils.md5(new File(testsDir, "downloads/file"+i)));
		}
		for(int i = 0; i<3; i++) {
			assertEquals(Utils.md5(new File(testsDir, "inputs/file"+i)),
					Utils.md5(new File(testsDir, "downloads/subdir/file"+i)));
		}
	}

	@Test
	public void testMultiThreadedDownload() throws Exception {
		String src =  new File("./pom.xml").getAbsolutePath();
		String target = testsDir.getAbsolutePath();
		String[] args = new String[]{ new UCP().getName(), "-u", "demouser:test123",
				"-v",
				"-t", "2", "-T", "100",
				"-D",
				getAuthURL(src), target
		};
		assertEquals(0, ClientDispatcher._main(args));
		assertEquals(new File(src).length(), new File(testsDir, "pom.xml").length());
		assertEquals(Utils.md5(new File(src)), Utils.md5(new File(testsDir, "pom.xml")));
	}

	public void testMultiThreadedUpload() throws Exception {
		String src =  new File("./pom.xml").getAbsolutePath();
		String target = testsDir.getAbsolutePath();
		String[] args = new String[]{ new UCP().getName(), "-u", "demouser:test123",
				"-t", "2", "-T", "100",
				src, getAuthURL(target)
		};
		assertEquals(0, ClientDispatcher._main(args));
		assertEquals(new File(src).length(), new File(testsDir, "pom.xml").length());
		assertEquals(Utils.md5(new File(src)), Utils.md5(new File(testsDir, "pom.xml")));
	}

}
