package gov.nist.hit.pcd.ws;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.log4j.Logger;
import org.springframework.integration.ip.tcp.serializer.AbstractPooledBufferByteArraySerializer;
import org.springframework.integration.ip.tcp.serializer.SoftEndOfStreamException;
import org.springframework.integration.mapping.MessageMappingException;

public class CustomSerializerDeserializer extends AbstractPooledBufferByteArraySerializer{

	private static final Logger logger = Logger.getLogger(CustomSerializerDeserializer.class.getName());

	public static final char VT = 0x0b;
	public static final char FS = 0x1c;
	public static final char CR = 0x0d;
	
	
	@Override
	public void serialize(byte[] bytes, OutputStream outputStream) throws IOException {
		outputStream.write(VT);
		outputStream.write(bytes);
		outputStream.write(FS+CR);
	}

	@Override
	protected byte[] doDeserialize(InputStream inputStream, byte[] buffer) throws IOException {
		boolean foundFS = false;
		int bite = inputStream.read();
		if (bite < 0) {
			System.out.println("stream closed");
			throw new SoftEndOfStreamException("Stream closed between payloads");
		}
		int n = 0;
		try {
			if (bite != VT) {
				throw new MessageMappingException("Expected VT to begin message");
			}
			while ((bite = inputStream.read()) >= 0) {
				if (foundFS) {
					if (bite == CR) {
						break;
					}else {
						buffer[n++] = FS;
						if (n >= this.maxMessageSize) {
							throw new IOException("VT not found before max message length: "
									+ this.maxMessageSize);
						}
					}
				}
				if (bite == FS) {
					foundFS = true;
					continue;
				}else {
					foundFS = false;
				}
				if (bite != VT) {
					buffer[n++] = (byte) bite;
					if (n >= this.maxMessageSize) {
						throw new IOException("VT not found before max message length: "
								+ this.maxMessageSize);
					}
				}
				
			}
			return copyToSizedArray(buffer, n);
		}
		catch (IOException e) {
			publishEvent(e, buffer, n);
			throw e;
		}
		catch (RuntimeException e) {
			publishEvent(e, buffer, n);
			throw e;
		}
	}

}