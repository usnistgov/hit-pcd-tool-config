package gov.nist.hit.pcd.config;

import gov.nist.hit.core.service.ResourcebundleLoader;
import gov.nist.hit.core.service.exception.ProfileParserException;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class PCDBootstrap {


	@Autowired
	@Qualifier("resourceLoader")
	ResourcebundleLoader resourcebundleLoader;
	
	@PostConstruct
	public void init() throws  Exception{
		resourcebundleLoader.load();
	}
	
	
	
}
