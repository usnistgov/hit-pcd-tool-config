package gov.nist.hit.pcd.ws;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import org.springframework.stereotype.Service;

import gov.nist.hit.pcd.controller.utils.Utils;

@Service
public class MLLPTcpClient {
	
	
	public String send(String message, String host, int port ) {		
	    try (Socket socket = new Socket(host, port)) {
	    		OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
            writer.println(Utils.wrapMLLPMessage(message));
            
            InputStream input = socket.getInputStream();	        
	        String data = Utils.extractMessage(input);
	        
	        return data;
	    } catch (UnknownHostException ex) {
	        System.out.println("Server not found: " + ex.getMessage());
	    } catch (IOException ex) {
	        System.out.println("I/O error: " + ex.getMessage());
	    }
		return null;
	}
	
	
    
    
}