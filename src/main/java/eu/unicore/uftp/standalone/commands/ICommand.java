package eu.unicore.uftp.standalone.commands;

import java.io.IOException;

/**
 * Provide options and usage information for the various 
 * client commands
 * 
 * @author schuller
 */
public interface ICommand {

	public String getName();
	
	public void parseOptions(String[] args) throws Exception;

	public void printUsage() throws IOException;

	public String getArgumentDescription();
	
	public String getSynopsis();
	
	public boolean runCommand() throws Exception;


	public static final String _newline = System.getProperty("line.separator");  
}
