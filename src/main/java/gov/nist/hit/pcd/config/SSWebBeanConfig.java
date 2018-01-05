package gov.nist.hit.pcd.config;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.core.JsonProcessingException;

import gov.nist.hit.core.hl7v2.service.HL7V2MessageParser;
import gov.nist.hit.core.hl7v2.service.HL7V2MessageParserImpl;
import gov.nist.hit.core.hl7v2.service.HL7V2MessageValidator;
import gov.nist.hit.core.hl7v2.service.HL7V2MessageValidatorImpl;
import gov.nist.hit.core.hl7v2.service.HL7V2ResourceLoaderImpl;
import gov.nist.hit.core.hl7v2.service.HL7V2ValidationReportConverter;
import gov.nist.hit.core.hl7v2.service.HL7V2ValidationReportConverterImpl;
import gov.nist.hit.core.service.ResourceLoader;
import gov.nist.hit.core.service.exception.ProfileParserException;

@Configuration
public class SSWebBeanConfig {

	
	@Bean
	  public ResourceLoader resourceLoader() throws JsonProcessingException, ProfileParserException, IOException {
	      return new HL7V2ResourceLoaderImpl();
	  }

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
	
}
