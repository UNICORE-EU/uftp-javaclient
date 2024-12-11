package eu.unicore.uftp.standalone.commands;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

import eu.unicore.uftp.dpc.Utils;
import eu.unicore.uftp.standalone.ClientFacade;
import eu.unicore.uftp.standalone.util.UOptions;
import eu.unicore.uftp.standalone.util.UnitParser;

/**
 * handles options related to data connections 
 * (like number of TCP streams per data connection)
 * 
 * @author schuller
 */
public abstract class DataTransferCommand extends RangedCommand {

	protected int streams = 1;
	
	protected long bandwithLimit = -1;

	protected boolean compress;
	protected boolean encrypt;

	protected int keylength;
	protected String algo;

	@Override
	protected UOptions getOptions() {
		UOptions options = super.getOptions();
		options.addOption(Option.builder("n").longOpt("streams")
				.desc("Number of TCP streams per connection/thread")
				.required(false)
				.hasArg().argName("Streams")
				.build(), UOptions.GRP_TRANSFER);
		options.addOption(Option.builder("E").longOpt("encrypt")
				.desc("Encrypt data connections")
				.required(false)
				.build(), UOptions.GRP_TRANSFER);
		options.addOption(Option.builder("C").longOpt("compress")
				.desc("Compress data for transfer")
				.required(false)
				.build(), UOptions.GRP_TRANSFER);
		options.addOption(Option.builder("K").longOpt("bandwith-limit")
				.desc("Limit bandwith per FTP connection (bytes per second)")
				.required(false)
				.hasArg().argName("BandwithLimit")
				.build(), UOptions.GRP_TRANSFER);
		return options;
	}
	
	@Override
	public void parseOptions(String[] args) throws ParseException {
		super.parseOptions(args);
		if (line.hasOption('n')) {
			streams = Integer.parseInt(line.getOptionValue('n'));
		}
		if (line.hasOption('K')) {
			UnitParser up = UnitParser.getCapacitiesParser(0);
			bandwithLimit = (long)up.getDoubleValue(line.getOptionValue('K'));
			verbose("LIMITING bandwidth per thread to {}B/s", up.getHumanReadable(bandwithLimit));
		}
		if (line.hasOption('E')) {
			encrypt = true;
			try{
				algo = Utils.getProperty("UFTP_ENCRYPTION_ALGORITHM", "Blowfish");
				algo = algo.toUpperCase();
				keylength = 56;
				if("aes".equalsIgnoreCase(algo)) {
					int l = 32;
					String aesLen = Utils.getProperty("UFTP_ENCRYPTION_AES_KEYSIZE", null);
					if(aesLen !=null) {
						l = Integer.parseInt(aesLen);
					}
					keylength = 16+l;
				}
				verbose("Encryption ({}) enabled with key length {}", algo, keylength);
			}catch(Exception ex){
				encrypt = false;
				System.err.println("WARN: cannot setup encryption: "+ex);
			}
		}
		compress = line.hasOption('C');
	}

	@Override
	protected void setOptions(ClientFacade client){
		super.setOptions(client);
		client.setStreams(streams);
		if(encrypt) {
			client.setEncryptionKeyLength(keylength);
			client.setEncryptionAlgorithm(algo);
		}
		client.setCompress(compress);
		client.setBandwithLimit(bandwithLimit);
	}
	
}
