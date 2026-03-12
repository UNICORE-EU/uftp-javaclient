package eu.unicore.uftp.standalone.ssh;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;

import org.h2.util.Utils;

/**
 * Support for signing via the ssh-agent
 * https://datatracker.ietf.org/doc/draft-ietf-sshm-ssh-agent
 * (kudos to https://github.com/ymnk/jsch-agent-proxy)
 *
 * @author schuller
 */
public class SSHAgentProxy {

	private static final String socketName = "SSH_AUTH_SOCK";

	private boolean use_RSA_SHA2_256 = ! Utils.getProperty("UFTP_AGENT_USE_OLD_SIGNATURE", false);

	public static boolean isConnectorAvailable(){
		return System.getenv(socketName)!=null;
	}

	// low level request/reply method
	protected byte[] send(byte[]data) throws IOException {
		String path = System.getenv(socketName);
		UnixDomainSocketAddress unixSocketAddress = UnixDomainSocketAddress.of(path);
		try(SocketChannel channel =  SocketChannel.open(unixSocketAddress);
				DataOutputStream os = new DataOutputStream(Channels.newOutputStream(channel));
				DataInputStream is = new DataInputStream(Channels.newInputStream(channel)))
		{
			os.writeInt(data.length);
			os.write(data);
			os.flush();
			int length = is.readInt();
			return is.readNBytes(length);
		}
	}

	/**
	 * @param blob - identity to use
	 * @param data - data to sign
	 * @param useRSA_SHA2_256 - if true, the new style RSA signature will be used
	 * @return
	 * @throws IOException
	 */
	public synchronized byte[] sign(byte[] blob, byte[] data) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);
		dos.writeByte(SSH2_AGENTC_SIGN_REQUEST);
		dos.writeInt(blob.length);
		dos.write(blob);
		dos.writeInt(data.length);
		dos.write(data);
		int flags = use_RSA_SHA2_256 ? SSH_AGENT_RSA_SHA2_256 : SSH_AGENT_OLD_SIGNATURE;
		dos.writeInt(flags);
		byte[]reply = send(bos.toByteArray());
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(reply));
		byte rcode = dis.readByte();
		if(SSH2_AGENT_SIGN_RESPONSE!=rcode) {
			throw new IOException("Unexpected reply from agent: <"+rcode+">");
		}
		int len = dis.readInt();
		return dis.readNBytes(len);
	}

	public synchronized Identity[] getIdentities() throws IOException {
		Identity[] identities = null;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);
		dos.writeByte(SSH2_AGENTC_REQUEST_IDENTITIES);
		byte[]data = send(bos.toByteArray());
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
		byte rcode = dis.readByte();
		if(SSH2_AGENT_IDENTITIES_ANSWER!=rcode) {
			throw new IOException("Unexpected reply from agent: <"+rcode+">");
		}
		int count = dis.readInt();
		identities = new Identity[count];
		for(int i=0; i<identities.length; i++){
			int l1 = dis.readInt();
			byte[]blob = dis.readNBytes(l1);
			int l2 = dis.readInt();
			byte[]comment = dis.readNBytes(l2);
			identities[i] = new Identity(blob, comment);
		}
		return identities;
	}

	public static class Identity {
		private byte[] blob;
		private byte[] comment;

		Identity(byte[] blob, byte[] comment){
			this.blob = blob;
			this.comment = comment;
		}

		public byte[] getBlob(){
			return blob;
		}

		public byte[] getComment(){
			return comment;
		}
	}

	// constants used in the agent protocol
	private static final byte SSH2_AGENTC_REQUEST_IDENTITIES = 11;
	private static final byte SSH2_AGENT_IDENTITIES_ANSWER = 12;
	private static final byte SSH2_AGENTC_SIGN_REQUEST = 13;
	private static final byte SSH2_AGENT_SIGN_RESPONSE = 14;
	private static final byte SSH_AGENT_OLD_SIGNATURE = 0x01;
	private static final byte SSH_AGENT_RSA_SHA2_256 = 0x02;

}
