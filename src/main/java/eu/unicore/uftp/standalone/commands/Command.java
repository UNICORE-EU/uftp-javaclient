package eu.unicore.uftp.standalone.commands;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.hc.core5.http.HttpMessage;
import org.apache.logging.log4j.message.ParameterizedMessage;

import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.services.restclient.UsernamePassword;
import eu.unicore.uftp.dpc.Utils;
import eu.unicore.uftp.standalone.ClientDispatcher;
import eu.unicore.uftp.standalone.ClientFacade;
import eu.unicore.uftp.standalone.ConnectionInfoManager;
import eu.unicore.uftp.standalone.oidc.OIDCAgentAuth;
import eu.unicore.uftp.standalone.oidc.OIDCServerAuthN;
import eu.unicore.uftp.standalone.ssh.SSHAgent;
import eu.unicore.uftp.standalone.ssh.SshKeyHandler;
import eu.unicore.uftp.standalone.util.Anonymous;
import eu.unicore.uftp.standalone.util.ConsoleUtils;
import eu.unicore.uftp.standalone.util.UOptions;
import eu.unicore.util.Log;

/***
 * handles options related to authentication
 * 
 * @author schuller
 */
public abstract class Command implements ICommand {

	/**
	 * environment variable defining the UFTP user name
	 */
	public static String UFTP_USER = "UFTP_USER";
	
	protected String[] fileArgs;

	protected CommandLine line;

	protected String username, password, group, authheader;
	protected boolean enableSSH = true;
	protected String sshIdentity = null;
	protected Integer agentIdentityIndex = null;

	protected boolean queryPassword = false;

	protected boolean verbose = false;

	protected String oidcAccount = null;
	protected String oidcServerSettings = null;

	protected String clientIP;

	protected UOptions getOptions() {
		UOptions options = new UOptions();
		options.addOption(Option.builder("h").longOpt("help")
				.desc("Show help / usage information")
				.required(false)
				.build(), UOptions.GRP_GENERAL);
		options.addOption(Option.builder("v").longOpt("verbose")
				.desc("Be verbose")
				.required(false)
				.build(), UOptions.GRP_GENERAL);
		options.addOption(Option.builder("u").longOpt("user")
				.desc("Username for username[:password] or key-based authentication")
				.required(false)
				.hasArg()
				.build(), UOptions.GRP_AUTH);
		options.addOption(Option.builder("X").longOpt("client")
				.desc("Client IP address: address-list")
				.required(false)
				.hasArg()
				.build(), UOptions.GRP_GENERAL);
		options.addOption(Option.builder("g").longOpt("group")
				.desc("Requested group membership to be used")
				.required(false)
				.hasArg()
				.build(), UOptions.GRP_AUTH);
		options.addOption(Option.builder("A").longOpt("auth")
				.desc("Bearer token value for authentication")
				.required(false)
				.hasArg()
				.build(), UOptions.GRP_AUTH);
		options.addOption(Option.builder("O").longOpt("oidc-agent")
				.desc("Use oidc-agent with the specified account")
				.required(false)
				.hasArg()
				.build(), UOptions.GRP_AUTH);
		options.addOption(Option.builder("o").longOpt("oidc-server")
				.desc("Get token from OIDC server using the specified settings file")
				.required(false)
				.hasArg()
				.build(), UOptions.GRP_AUTH);
		options.addOption(Option.builder("P").longOpt("password")
				.desc("Interactively query for a missing password")
				.required(false)
				.build(), UOptions.GRP_AUTH);
		options.addOption(Option.builder("i").longOpt("identity")
				.desc("Identity file (private key) for key-based authentication")
				.required(false)
				.hasArg()
				.build(), UOptions.GRP_AUTH);
		return options;
	}

	@Override
	public void parseOptions(String[] args) throws ParseException {
		Options options = getOptions();
		CommandLineParser parser = new DefaultParser();
		line = parser.parse(options, args);
		fileArgs = line.getArgs();
		if (line.hasOption('v')){
			verbose = true;
			verbose("Verbose mode");
		}
		if (line.hasOption('u')){
			String up = line.getOptionValue('u');
			String[] tokens = up!=null? up.split(":",2) : null;
			username = tokens!=null ? tokens[0] : null;
			password = tokens!=null && tokens.length>1 ? tokens[1] : null;
			if(password == null){
				if(line.hasOption('P')){
					password = Utils.getProperty("UFTP_PASSWORD", null);
					if(password==null) {
						password = ConsoleUtils.readPassword("Password:");
					}
					enableSSH = false;
				}
			}
			if(password!=null)enableSSH=false;
		}
		if (line.hasOption('g')){
			group = line.getOptionValue('g');
		}
		if (line.hasOption('A')){
			if(line.hasOption('u') || line.hasOption('O')){
				throw new IllegalArgumentException("Only one of '-u', '-A' or 'O' can be used!");
			}
			authheader = line.getOptionValue('A');
			if(authheader.split(" ").length==1) {
				authheader = "Bearer "+authheader;
			}
			enableSSH = false;
		}
		if (line.hasOption('O')){
			if(line.hasOption('u') || line.hasOption('A') || line.hasOption('o')){
				throw new IllegalArgumentException("Only one of '-u', '-A', 'o', or '-O' can be used!");
			}
			oidcAccount = line.getOptionValue('O');
			enableSSH = false;
		}
		if (line.hasOption('o')){
			if(line.hasOption('u') || line.hasOption('A') || line.hasOption('O')){
				throw new IllegalArgumentException("Only one of '-u', '-A', 'o', or '-O' can be used!");
			}
			oidcServerSettings = line.getOptionValue('o');
			enableSSH = false;
		}
		if(enableSSH){
			if(!line.hasOption('u')) {
				username = Utils.getProperty(UFTP_USER, null);
				if(username==null)username = System.getProperty("user.name");
			}
			if (line.hasOption('i')){
				sshIdentity = line.getOptionValue('i');
			}
		}
		if(line.hasOption('X')){
			clientIP = line.getOptionValue('X');
		}
	}

	protected void setOptions(ClientFacade client){
		client.setVerbose(verbose);
		client.setGroup(group);
		client.setClientIP(clientIP);
	}

	protected abstract void run(ClientFacade facade) throws Exception;

	public boolean runCommand() throws Exception {
		if(line.hasOption("h")) {
			printUsage();
			return true;
		}
		try{
			ConnectionInfoManager cim = new ConnectionInfoManager(getAuthData());
			ClientFacade facade = new ClientFacade(cim);
			setOptions(facade);
			run(facade);
			return true;
		}catch(Exception ex) {
			error(Log.createFaultMessage("ERROR", ex));
			return false;
		}
	}
	
	/**
	 * print help
	 */
	@Override
	public void printUsage() {
		message("UFTP Client {}", ClientDispatcher.getVersion());

		HelpFormatter formatter = new HelpFormatter();
		String newLine=System.getProperty("line.separator");

		StringBuilder s = new StringBuilder();
		s.append(getName()).append(" [OPTIONS] ").append(getArgumentDescription()).append(newLine);
		s.append(getSynopsis()).append(newLine);
		s.append("Remote URLs are built as follows:").append(newLine);
		s.append(getRemoteURLExample1()).append(newLine);
		s.append(getRemoteURLExample2()).append(newLine).append(newLine);
		s.append("Options:").append(newLine);
		
		String syntax = s.toString();
		
		UOptions options = getOptions();
		Options def = options.getDefaultOptions();
		if(def!=null){
			formatter.printHelp(syntax, def);
		}
		else{
			formatter.printHelp(syntax, new Options());
		}
		Options transferOptions = options.getTransferOptions();
		if(transferOptions!=null){
			System.out.println();
			formatter.setSyntaxPrefix("Transfer options:");
			formatter.printHelp(" "+newLine, transferOptions);
		}
		Options authOptions = options.getAuthenticationOptions();
		if(authOptions!=null){
			System.out.println();
			formatter.setSyntaxPrefix("Authentication options:");
			formatter.printHelp(" "+newLine, authOptions);
		}
		Options general=options.getGeneralOptions();
		if(general!=null){
			System.out.println();
			formatter.setSyntaxPrefix("General options:");
			formatter.printHelp(" "+newLine, general);
		}
	}
	
	protected String getRemoteURLExample1(){
		return "* https://<auth_addr>/rest/auth/<SERVER>:<file_path>";
	}

	protected String getRemoteURLExample2(){
		return "* https://<ux_addr>/rest/core/storages/<STORAGE>:<file_path>";
	}



	protected IAuthCallback getAuthData() throws Exception {
		if("anonymous".equals(username)) {
			return new Anonymous();
		}

		if(enableSSH){
			return getSSHAuthData();
		}
		
		if(authheader!=null){
			return new IAuthCallback() {

				@Override
				public String getType() {
					return "Header";
				}

				@Override
				public void addAuthenticationHeaders(HttpMessage httpMessage) {
					httpMessage.setHeader("Authorization", authheader);
				}
			};
		}
		else if(oidcAccount!=null) {
			return new OIDCAgentAuth(oidcAccount);
		}
		else if(oidcServerSettings!=null) {
			return new OIDCServerAuthN(oidcServerSettings, verbose);			
		}
		else{
			return getUPAuthData();
		}

	}

	protected IAuthCallback getUPAuthData(){
		return new UsernamePassword(username, password);
	}

	protected IAuthCallback getSSHAuthData() throws Exception {
		String token = String.valueOf(System.currentTimeMillis());
		return getSSHAuthData(token);
	}

	protected IAuthCallback getSSHAuthData(String token) throws Exception {
		File keyFile = null;
		boolean haveAgent = SSHAgent.isAgentAvailable();
		int numKeys = 0;
		
		File[] dirs = new File[] {
				 new File(System.getProperty("user.home"),".uftp"),
				 new File(System.getProperty("user.home"),".ssh")
		};
		
		if(sshIdentity==null){
			FilenameFilter ff = new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.startsWith("id_") && !name.endsWith(".pub");
				}
			};
			outer: for(File keysDir: dirs) {
				if(keysDir.exists()){
					File[] ops = keysDir.listFiles(ff);
					numKeys+=ops.length;
					for(File key: keysDir.listFiles(ff)) {
						if(!key.isDirectory() && key.canRead()) {
							keyFile = key;
							break outer;
						};
					}
				}
			}
			if(keyFile == null || !keyFile.exists()){
				throw new IOException("No useable private key found in "+Arrays.asList(dirs)+". Please use the --identity option!");
			}
			if(numKeys>1) {
				verbose("NOTE: more than one useable key found -"
						+ " you might want to use '--identity <path_to_private_key>'");
			}
		}
		else {
			keyFile = new File(sshIdentity);
			if(!haveAgent && !keyFile.exists()){
				throw new IOException("Private key file " + sshIdentity + " does not exist.");
			}
		}
			if(haveAgent) {
				verbose("Using SSH agent");
			}
			if(keyFile!=null){
				verbose("Using SSH key <{}>", keyFile.getAbsolutePath());
			}
		SshKeyHandler ssh = new SshKeyHandler(keyFile, username, token, verbose);
		if(haveAgent && sshIdentity!=null) {
			ssh.selectIdentity();
		}
		return ssh.getAuthData();
	}

	public void verbose(String msg, Object ... params) {
		if(verbose)message(msg, params);
	}

	public void error(String msg, Object ... params) {
		System.err.println(format(msg, params));
	}

	public void message(String msg, Object ... params) {
		System.out.println(format(msg, params));
	}

	private String format(String msg, Object ... params) {
		return new ParameterizedMessage(msg, params).getFormattedMessage();
	}
}
