package gov.nist.hit.pcd.config;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.core.JsonProcessingException;

import gov.nist.hit.core.hl7v2.service.HL7V2MessageParser;
import gov.nist.hit.core.hl7v2.service.HL7V2MessageValidator;
import gov.nist.hit.core.hl7v2.service.HL7V2ResourceLoader;
import gov.nist.hit.core.hl7v2.service.HL7V2ValidationReportConverter;
import gov.nist.hit.core.hl7v2.service.impl.HL7V2MessageParserImpl;
import gov.nist.hit.core.hl7v2.service.impl.HL7V2MessageValidatorImpl;
import gov.nist.hit.core.hl7v2.service.impl.HL7V2ResourceLoaderImpl;
import gov.nist.hit.core.hl7v2.service.impl.HL7V2ValidationReportConverterImpl;
import gov.nist.hit.core.service.ResourceLoader;
import gov.nist.hit.core.service.exception.ProfileParserException;
import gov.nist.hit.core.service.wctp.WCTPMessageParser;
import gov.nist.hit.core.service.wctp.WCTPMessageParserImpl;
import gov.nist.hit.core.service.wctp.WCTPMessageValidator;
import gov.nist.hit.core.service.wctp.WCTPMessageValidatorImpl;
import gov.nist.hit.core.service.wctp.WCTPResourceLoader;
import gov.nist.hit.core.service.wctp.WCTPResourceLoaderImpl;
import gov.nist.hit.core.service.wctp.WCTPValidationReportConverter;
import gov.nist.hit.core.service.wctp.WCTPValidationReportConverterImpl;
import gov.nist.hit.pcd.core.PCDResourceLoaderImpl;

@Configuration
public class PCDWebBeanConfig {

	
		// GVT Specific
		@Bean(name = "resourceLoader")
		public ResourceLoader resourceLoader() throws JsonProcessingException, ProfileParserException, IOException {
			return new PCDResourceLoaderImpl();
		}

		// HL7V2
		@Bean(name = "hl7v2ResourceLoader")
		public HL7V2ResourceLoader hl7v2ResourceLoader() throws JsonProcessingException, ProfileParserException, IOException {
			return new HL7V2ResourceLoaderImpl();
		}

		
		
	
//	@Bean
//	  public ResourceLoader resourceLoader() throws JsonProcessingException, ProfileParserException, IOException {
//	      return new HL7V2ResourceLoaderImpl();
//	  }

	 @Bean
	  public HL7V2ValidationReportConverter hl7v2ValidationReportConverter() {
	      return new HL7V2ValidationReportConverterImpl();
	 }

	  @Bean
	  public HL7V2MessageValidator hl7v2MessageValidator() {
	     return new HL7V2MessageValidatorImpl();
	  }
	  
	  @Bean
	  public HL7V2MessageParser hl7v2MessageParser() {
	    return new HL7V2MessageParserImpl();
	  }
	  
	// WCTP specific
			@Bean(name = "wctpResourceLoader")
			public WCTPResourceLoader wctpResourceLoader() {
				return new WCTPResourceLoaderImpl();
			}
			
	  @Bean
		public WCTPValidationReportConverter wctpValidationReportConverter() {
			return new WCTPValidationReportConverterImpl();
		}

		@Bean
		public WCTPMessageValidator wctpMessageValidator() {
			return new WCTPMessageValidatorImpl();
		}

		@Bean
		public WCTPMessageParser wctpMessageParser() {
			return new WCTPMessageParserImpl();
		}
	  
	  
	 
	
}
