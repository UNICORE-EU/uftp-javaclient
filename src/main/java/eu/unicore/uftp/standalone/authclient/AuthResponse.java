package eu.unicore.uftp.standalone.authclient;

import eu.unicore.uftp.dpc.Utils.EncryptionAlgorithm;

/**
 * @author mgolik
 *
 */
public class AuthResponse {

    public boolean success = true;
    public String reason = "";
    public String serverHost;
    public Integer serverPort;
    public String secret = "";
    public byte[] encryptionKey = null;
    public EncryptionAlgorithm encryptionAlgorithm = null;

}
