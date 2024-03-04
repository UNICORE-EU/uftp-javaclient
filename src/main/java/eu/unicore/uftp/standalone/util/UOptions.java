package eu.unicore.uftp.standalone.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * Collection of options for commandline, checking if option names (both long and short) are unique.
 * Adding an option with already taken name results in IllegalArgumentException.<br/>
 * 
 * This class also allows to handle multiple groups of options, to improve printing usage info<br/>
 * 
 * It is not thread-safe.<br/>
 * 
 * @author kstasiak
 * @author schuller
 */
public class UOptions extends Options {
	private static final long serialVersionUID = 1L;
	
	private final Set<String> usedOptionNames = new HashSet<>();

	public static final String GRP_DEFAULT = "__DEFAULT__";
	
	public static final String GRP_GENERAL = "__GENERAL__";
	
	public static final String GRP_AUTH = "__AUTHENTICATION__";

	public static final String GRP_TRANSFER = "__TRANSFER__";
	
	private final Map<String,List<Option>> optionGroups = new HashMap<>();
	
	private void markAsUsed(String optionName) {
		if(usedOptionNames.contains(optionName)) {
			throw new IllegalArgumentException("The option \'"+optionName+
					"\' has already been registered before.");
		}
		usedOptionNames.add(optionName);
	}
	
	@Override
	public Options addOption(Option option){
		return addOption(option, GRP_DEFAULT);
	}
	
	public Options addOption(Option option, String optionGroup){
		markAsUsed(option.getLongOpt());
		markAsUsed(option.getOpt());
		Options result = super.addOption(option);
		List<Option>grp = optionGroups.get(optionGroup);
		if(grp==null){
			grp = new ArrayList<>();
			optionGroups.put(optionGroup, grp);
		}
		grp.add(option);
		return result;
	}
	
	private List<Option>getOptionsGroup(String name){
		return optionGroups.get(name);
	}
	
	public Options getDefaultOptions(){
		List<Option> defOpts = getOptionsGroup(GRP_DEFAULT);
		if(defOpts==null)return null;
		Options res=new Options();
		for(Option o: defOpts){
			res.addOption(o);
		}
		return res;
	}
	
	public Options getGeneralOptions(){
		List<Option> secOpts = getOptionsGroup(GRP_GENERAL);
		if(secOpts==null)return null;
		Options res=new Options();
		for(Option o: secOpts){
			res.addOption(o);
		}
		return res;
	}
	
	public Options getAuthenticationOptions(){
		List<Option> secOpts = getOptionsGroup(GRP_AUTH);
		if(secOpts==null)return null;
		Options res=new Options();
		for(Option o: secOpts){
			res.addOption(o);
		}
		return res;
	}

	public Options getTransferOptions(){
		List<Option> secOpts = getOptionsGroup(GRP_TRANSFER);
		if(secOpts==null)return null;
		Options res=new Options();
		for(Option o: secOpts){
			res.addOption(o);
		}
		return res;
	}
}
