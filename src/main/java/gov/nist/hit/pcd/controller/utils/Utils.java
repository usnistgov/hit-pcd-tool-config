package gov.nist.hit.pcd.controller.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.DefaultModelClassFactory;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.util.Terser;

public class Utils {

	public static final char VT = 0x0b;
	public static final char FS = 0x1c;
	public static final char CR = 0x0d;
	
	
	public static String wrapMLLPMessage(String message) {
		return (VT + message + FS + CR).replaceAll("\n","\r");
	}
	
	public static String unWrapMLLPMessage(String message) {
		return message.substring(1,message.length() -1);
	}
	
	
	 public static String getUrl(HttpServletRequest request) {
	        String scheme = request.getScheme();
	        String host = request.getHeader("Host");
	        if (host.contains("psapps01.nist.gov")) {
				host = "www-s.nist.gov";
			}
	        return scheme + "://" + host + request.getContextPath();
	    }
	
	 public static String extractMessage(InputStream in) throws IOException {
	        StringWriter inBuf = new StringWriter();
	        int c = 0;
	        boolean gotFS = false;
	        boolean newMessage = false;
	        while ((c = in.read()) != -1) {
	            /* ignore everything till we see, start of new message char */
	            if (!newMessage) {
	                if (c == VT) {
	                    newMessage = true;
	                    continue;
	                }
	            }
	            /* see if we're done */
	            if (gotFS) {
	                if (c == CR) {
	                    break;
	                } else {
	                    /*
	                     * don't think FS can be part of msg, but if we didn't get a
	                     * CR, we have to assume it is
	                     */
	                    inBuf.write(FS);
	                }
	            }
	            /* check for field separator char */
	            if (c == FS) {
	                gotFS = true;
	                continue;
	            } else {
	                gotFS = false;
	            }
	            if (c != VT)
	                inBuf.write(c);
	        }
	        return inBuf.toString();
	    }
	 
	 
	 
	public static Message parseER7Message(String msg) throws HL7Exception {
		HapiContext context = new DefaultHapiContext();

		PipeParser parser = context.getPipeParser();

		context.setModelClassFactory(new DefaultModelClassFactory());

		// 20169838-v23
		Message message = parser.parse(msg);
		Terser t = new Terser(message);
		System.out.println(t.get("/MSH-5"));

		return message;
	}
	 
}
