package gov.nist.hit.pcd.ws;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.ip.tcp.TcpInboundGateway;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNetServerConnectionFactory;
import org.springframework.messaging.MessageChannel;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.util.Terser;
import gov.nist.hit.core.domain.Transaction;
import gov.nist.hit.core.domain.TransportMessage;
import gov.nist.hit.core.repo.MessageRepository;
import gov.nist.hit.core.service.TransactionService;
import gov.nist.hit.core.service.TransportMessageService;
import gov.nist.hit.pcd.controller.utils.Utils;

@IntegrationComponentScan
@Configuration
@EnableIntegration
public class MLLPTcpServerConfig {

	@Autowired
	protected TransportMessageService transportMessageService;

	@Autowired
	protected TransactionService transactionService;

	@Autowired
	private MessageRepository messageRepository;
	
	private final int port = 13080;
	
	protected int maxMessageSize = 32768;

//	private String host = "localhost";
//	private int port = 8082;
//	
//	@MessagingGateway(defaultRequestChannel = "toTcp")
//	public interface Gateway {
//
//		String viaTcp(String in);
//
//	}
//
//	@Bean
//	@ServiceActivator(inputChannel = "toTcp")
//	public MessageHandler tcpOutGate(AbstractClientConnectionFactory connectionFactory) {
//		System.out.println();
//		TcpOutboundGateway gate = new TcpOutboundGateway();
//		gate.setConnectionFactory(connectionFactory);
//		gate.setOutputChannelName("resultToString");
//		return gate;
//	}

	@Bean
	public TcpInboundGateway tcpInGate(AbstractServerConnectionFactory connectionFactory) {
		TcpInboundGateway inGate = new TcpInboundGateway();
		inGate.setConnectionFactory(connectionFactory);
		inGate.setRequestChannel(fromTcp());
		return inGate;
	}

	@Bean
	public MessageChannel fromTcp() {
		return new DirectChannel();
	}

	@MessageEndpoint
	public class MLLPEndpoint {

		@Transformer(inputChannel = "fromTcp", outputChannel = "toMessageHandler")
		public String convert(byte[] bytes) {
			return new String(bytes);
		}

		@ServiceActivator(inputChannel = "toMessageHandler")
		public String messageHandler(String message) {

			try {
				String sutappnamespaceid;
				String taappnamespaceid;
				String sutfacilitynamespaceid;
				String tafacilitynamespaceid;
				Message msg = Utils.parseER7Message(message);

				Terser t = new Terser(msg);
				sutappnamespaceid = t.get("/MSH-3-1");
				taappnamespaceid = t.get("/MSH-5-1");

				sutfacilitynamespaceid = t.get("/MSH-4-1");
				tafacilitynamespaceid = t.get("/MSH-6-1");

				Map<String, String> properties = getProperties(taappnamespaceid, tafacilitynamespaceid);
				TransportMessage transportMessage = transportMessageService.findOneByProperties(properties);

				if (transportMessage != null) {
					String responseMessage = getResponseMessage(transportMessage.getMessageId());

					Message respMsg = Utils.parseER7Message(responseMessage);
					Terser t2 = new Terser(respMsg);
					t2.set("/MSH-3-1", taappnamespaceid);
					t2.set("/MSH-4-1", tafacilitynamespaceid);
					t2.set("/MSH-5-1", sutappnamespaceid);
					t2.set("/MSH-6-1", sutfacilitynamespaceid);

					String res = respMsg.toString();
					
					
					Transaction transaction = transactionService.findOneByProperties(properties);
					if (transaction == null) {
						transaction = new Transaction();
						transaction.setProperties(properties);
					}
					transaction.setIncoming(message);
					transaction.setOutgoing(res);
					transactionService.save(transaction);
					
					return Utils.wrapMLLPMessage(res);
				}

			} catch (HL7Exception e) {
				// Could not retrieve message information
				e.printStackTrace();
			}

			return null;
		}
//
//		@Transformer(inputChannel = "resultToString")
//		public String convertResult(byte[] bytes) {
//			System.out.println("sdfsdf");
//			return new String(bytes);
//		}

	}

//	@Bean
//	public AbstractClientConnectionFactory clientCF() {
//		return new TcpNetClientConnectionFactory(this.host, this.port);
//	}

	@Bean
	public AbstractServerConnectionFactory serverCF() {
		TcpNetServerConnectionFactory cf = new TcpNetServerConnectionFactory(this.port);
		CustomSerializerDeserializer ser = new CustomSerializerDeserializer();
		ser.setMaxMessageSize(this.maxMessageSize);		
		cf.setSerializer(ser);
		
		CustomSerializerDeserializer deser = new CustomSerializerDeserializer();
		deser.setMaxMessageSize(this.maxMessageSize);
		cf.setDeserializer(deser);
		
		
		return cf;
	}

	private Map<String, String> getProperties(String appnamespaceid, String facilitynamespaceid) {
		Map<String, String> properties = new HashMap<String, String>();
		properties.put("appnamespaceid", appnamespaceid);
		properties.put("facilitynamespaceid", facilitynamespaceid);
		return properties;
	}

	private String getResponseMessage(Long messageId) {
		if (messageId != null) {
			return messageRepository.getContentById(messageId);
		}
		return null;
	}

//	public String getHost() {
//		return host;
//	}
//
//	public void setHost(String host) {
//		this.host = host;
//	}
//
//	public int getPort() {
//		return port;
//	}
//
//	public void setPort(int port) {
//		this.port = port;
//	}
//	
//	


}
