package eu.unicore.uftp.standalone;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;

import org.junit.jupiter.api.Test;

import eu.unicore.uftp.standalone.commands.ICommand;

public class DispatcherTest {

	@Test
	public void testGetCommands() {
		Collection<ICommand>commands = new ClientDispatcher().getCommands();
		assertNotNull(commands);
		assertTrue(commands.size()>0);
		for(ICommand c: commands){
			c.printUsage();
		}
	}
	
	@Test
	public void testVersionInfo() {
		ClientDispatcher.printVersion();
	}
}
