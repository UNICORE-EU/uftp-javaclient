package eu.unicore.uftp.standalone.util;

/**
 * How to handle resumed transfers
 * 
 * @author schuller
 */
public enum ResumeMode {

	/**
	 * no resume
	 */
	NONE, 

	/**
	 * append missing data to existing file
	 */
	APPEND,

	/**
	 * checksum and do partial writes
	 */
	CHECKSUM
}
