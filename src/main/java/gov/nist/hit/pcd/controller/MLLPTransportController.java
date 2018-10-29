package gov.nist.hit.pcd.controller;

/**
 * This software was developed at the National Institute of Standards and Technology by employees of
 * the Federal Government in the course of their official duties. Pursuant to title 17 Section 105
 * of the United States Code this software is not subject to copyright protection and is in the
 * public domain. This is an experimental system. NIST assumes no responsibility whatsoever for its
 * use by other parties, and makes no guarantees, expressed or implied, about its quality,
 * reliability, or any other characteristic. We would appreciate acknowledgement if the software is
 * used. This software can be redistributed and/or modified freely provided that any derivative
 * works bear some notice that they are derived from it, and any modified versions bear some notice
 * that they have been modified.
 */


import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import gov.nist.auth.hit.core.domain.Account;
import gov.nist.auth.hit.core.domain.TransportConfig;
import gov.nist.hit.core.api.SessionContext;
import gov.nist.hit.core.domain.TestStep;
import gov.nist.hit.core.domain.Transaction;
import gov.nist.hit.core.domain.TransportMessage;
import gov.nist.hit.core.domain.TransportRequest;
import gov.nist.hit.core.domain.TransportResponse;
import gov.nist.hit.core.service.AccountService;
import gov.nist.hit.core.service.PasswordService;
import gov.nist.hit.core.service.Streamer;
import gov.nist.hit.core.service.TestStepService;
import gov.nist.hit.core.service.TransactionService;
import gov.nist.hit.core.service.TransportConfigService;
import gov.nist.hit.core.service.TransportMessageService;
import gov.nist.hit.core.service.exception.DuplicateTokenIdException;
import gov.nist.hit.core.service.exception.TestCaseException;
import gov.nist.hit.core.service.exception.TransportException;
import gov.nist.hit.core.service.exception.UserNotFoundException;
import gov.nist.hit.core.service.exception.UserTokenIdNotFoundException;
import gov.nist.hit.core.service.util.GCUtil;
import gov.nist.hit.core.transport.exception.TransportClientException;
import gov.nist.hit.pcd.controller.utils.Utils;
import gov.nist.hit.pcd.ws.MLLPTcpClient;
import io.swagger.annotations.ApiParam;



@RestController
@RequestMapping("/transport/pcd*/mllp")
// @Api(value = "Immunization SOAP Transport API", tags = "SOAP Transport")
public class MLLPTransportController {

	static final Logger logger = LoggerFactory.getLogger(MLLPTransportController.class);

	@Autowired
	protected TestStepService testStepService;

	@Autowired
	protected AccountService accountService;

	@Autowired
	protected TransactionService transactionService;

	@Autowired
	protected TransportMessageService transportMessageService;

	@Autowired
	protected TransportConfigService transportConfigService;

	@Autowired
	private PasswordService passwordService;

	@Autowired
	private Streamer streamer;
	
	
	@Autowired
	private MLLPTcpClient toTcp;
	

	

	private final static String PROTOCOL = "mllp";
	private final static String DOMAIN = "pcd";	
	
	private final static String APPLICATIONNAMESPACEID = "appnamespaceid";
	private final static String APPLICATIONUNIVERSALID = "appuniversalid";
	private final static String APPLICATIONUNIVERSALTYPE = "appuniversaltype";

	private final static String FACILITYNAMESPACEID = "facilitynamespaceid";
	private final static String FACILITYUNIVERSALID = "facilityuniversalid";
	private final static String FACILITYUNIVERSALTYPE = "facilityuniversaltype";

	public MLLPTransportController() {
		
	}

	// @ApiOperation(value = "Start the listener of an incoming transaction",
	// nickname = "startListener", notes = "A user session is required")
	@RequestMapping(value = "/startListener", method = RequestMethod.POST, produces = "application/json")
	public boolean startListener(
			@ApiParam(value = "the transport request", required = true) @RequestBody TransportRequest request,
			@ApiParam(value = "The user session", required = true) HttpSession session) throws UserNotFoundException {
		logger.info("Starting listener");
		Long userId = SessionContext.getCurrentUserId(session);
		if (userId == null || (accountService.findOne(userId)) == null) {
			throw new UserNotFoundException();
		}
		clearExchanges(userId);

		if (request.getResponseMessageId() == null)
			throw new gov.nist.hit.core.service.exception.TransportException("Response message not found");

		TransportMessage transportMessage = new TransportMessage();
		transportMessage.setMessageId(request.getResponseMessageId());
		Map<String, String> config = new HashMap<String, String>();
		config.putAll(getSutInitiatorConfig(userId));
		transportMessage.setProperties(config);
		transportMessageService.save(transportMessage);

		GCUtil.performGC();
		return true;
	}

	// @ApiOperation(value = "Stop the listener of an incoming transaction",
	// nickname = "stopListener", notes = "A user session is required")
	@RequestMapping(value = "/stopListener", method = RequestMethod.POST, produces = "application/json")
	public boolean stopListener(@ApiParam(value = "the request", required = true) @RequestBody TransportRequest request,
			@ApiParam(value = "The user session", required = true) HttpSession session) throws UserNotFoundException {
		logger.info("Stopping listener ");
		Long userId = SessionContext.getCurrentUserId(session);
		if (userId == null || (accountService.findOne(userId)) == null) {
			throw new UserNotFoundException();
		}
		clearExchanges(userId);
		GCUtil.performGC();
		return true;
	}

	private boolean clearExchanges(Long userId) {
		Map<String, String> config = getSutInitiatorConfig(userId);
		Map<String, String> criteria = new HashMap<String, String>();
		criteria.put(APPLICATIONNAMESPACEID, config.get(APPLICATIONNAMESPACEID));
		criteria.put(FACILITYNAMESPACEID, config.get(FACILITYNAMESPACEID));
		clearMessages(criteria);
		clearTransactions(criteria);
		return true;
	}

	private void clearMessages(Map<String, String> criteria) {
		List<TransportMessage> transportMessages = transportMessageService.findAllByProperties(criteria);
		if (transportMessages != null && !transportMessages.isEmpty()) {
			transportMessageService.delete(transportMessages);
		}
	}

	private void clearTransactions(Map<String, String> criteria) {
		List<Transaction> transactions = transactionService.findAllByProperties(criteria);
		if (transactions != null && !transactions.isEmpty()) {
			transactionService.delete(transactions);
		}
	}

	private Map<String, String> getSutInitiatorConfig(Long userId) {
		TransportConfig config = transportConfigService.findOneByUserAndProtocolAndDomain(userId, PROTOCOL, DOMAIN);
		Map<String, String> sutInitiator = config != null ? config.getSutInitiator() : null;
		if (sutInitiator == null || sutInitiator.isEmpty())
			throw new gov.nist.hit.core.service.exception.TransportException(
					"No System Under Test configuration info found");
		return sutInitiator;
	}

	// @ApiOperation(value = "Search a transaction of user", nickname =
	// "searchTransaction")
	@RequestMapping(value = "/searchTransaction", method = RequestMethod.POST, produces = "application/json")
	public void searchTransaction(HttpServletResponse response,
			@ApiParam(value = "the transport request", required = true) @RequestBody TransportRequest request)
			throws IOException {
		logger.info("Searching transaction...");
		Map<String, String> criteria = new HashMap<String, String>();
		criteria.put(APPLICATIONNAMESPACEID, request.getConfig().get(APPLICATIONNAMESPACEID));
		criteria.put(FACILITYNAMESPACEID, request.getConfig().get(FACILITYNAMESPACEID));
		streamer.stream(response.getOutputStream(), transactionService.findOneByProperties(criteria));

	}

	// @ApiOperation(value = "Send a message", nickname = "searchTransaction",
	// notes = "A user session is required")
	@RequestMapping(value = "/send", method = RequestMethod.POST, produces = "application/json")
	public void send(HttpServletResponse response,
			@ApiParam(value = "the transport request", required = true) @RequestBody TransportRequest request,
			@ApiParam(value = "The user session", required = true) HttpSession session)
			throws TransportClientException {
		logger.info("Sending message");
		try {
			Long userId = SessionContext.getCurrentUserId(session);
			if (userId == null || (accountService.findOne(userId)) == null) {
				throw new UserNotFoundException();
			}

			if (request.getConfig().get("port") == null || "".equals(request.getConfig().get("port")) || !StringUtils.isNumeric(request.getConfig().get("port")) || request.getConfig().get("ip") == null || "".equals(request.getConfig().get("ip"))) {
				throw new TransportException("No valid IP and/or port specified");
			}
			Long testStepId = request.getTestStepId();
			TestStep testStep = testStepService.findOne(testStepId);
			if (testStep == null)
				throw new TestCaseException("Unknown test step with id=" + testStepId);
			String outgoingMessage = request.getMessage();	
					
			String  incomingMessage = toTcp.send(outgoingMessage, request.getConfig().get("ip"), Integer.parseInt(request.getConfig().get("port")));
			
			Transaction transaction = new Transaction();
			transaction.setOutgoing(outgoingMessage);
			transaction.setIncoming(incomingMessage);
			streamer.stream(response.getOutputStream(), transaction);
		} catch (Exception e1) {
			throw new TransportException("Failed to send the message." + e1.getMessage());
		}
	}

	// @ApiOperation(value = "Get the configuration information of user",
	// nickname = "searchTransaction", notes = "A user session is required")
	@RequestMapping(value = "/configs", method = RequestMethod.POST, produces = "application/json")
	public TransportConfig configs(@ApiParam(value = "The user session", required = true) HttpSession session,
			HttpServletRequest request) throws UserNotFoundException {
		logger.info("Fetching user configuration information ... ");
		Long userId = SessionContext.getCurrentUserId(session);
		Account user = null;
		if (userId == null || (user = accountService.findOne(userId)) == null) {
			throw new UserNotFoundException();
		}
		TransportConfig transportConfig = transportConfigService.findOneByUserAndProtocolAndDomain(userId, PROTOCOL,
				DOMAIN);
		if (transportConfig == null) {
			transportConfig = transportConfigService.create(PROTOCOL, DOMAIN);
			transportConfig.setUserId(userId);
			Map<String, String> taInitiatorConfig = taInitiatorConfig(user, request);
			transportConfig.setTaInitiator(taInitiatorConfig);
		}
		Map<String, String> sutInitiatorConfig = sutInitiatorConfig(user, request);
		transportConfig.setSutInitiator(sutInitiatorConfig);
		transportConfigService.save(transportConfig);
		GCUtil.performGC();
		return transportConfig;
	}
	
	

	private Map<String, String> taInitiatorConfig(Account user, HttpServletRequest request)
			throws UserNotFoundException {
		logger.info("Creating user ta initiator config information ... ");
		Map<String, String> config = new HashMap<String, String>();
		config.put(APPLICATIONNAMESPACEID, "");
		config.put(APPLICATIONUNIVERSALID, "");
		config.put(APPLICATIONUNIVERSALTYPE, "");
		config.put(FACILITYNAMESPACEID, "");
		config.put(FACILITYUNIVERSALID, "");
		config.put(FACILITYUNIVERSALTYPE, "");
		return config;
	}

	private Map<String, String> sutInitiatorConfig(Account user, HttpServletRequest request)
			throws UserNotFoundException {
		logger.info("Creating user sut initiator config information ... ");
		Map<String, String> config = new HashMap<String, String>();
		int token = new Random().nextInt(999);
		config.put(APPLICATIONNAMESPACEID, user.isGuestAccount() ? "app_vendor_" + user.getId() + "_" + token : user.getUsername());		
		config.put(FACILITYNAMESPACEID, user.isGuestAccount() ? "fac_vendor_" + user.getId() + "_" + token : user.getUsername());
		
		
		String endpoint = "";
        endpoint = Utils.getUrl(request);
       
//        endpoint += "/api/" + DOMAIN + "/" + PROTOCOL + "/message";       
				
		config.put("endpoint",endpoint);
		return config;
	}

	// @ApiOperation(value = "", nickname = "", notes = "A user session is
	// required", hidden = true)
	@RequestMapping(value = "/populateMessage", method = RequestMethod.POST, produces = "application/json")
	public TransportResponse populateMessage(@ApiParam(value = "The user session", required = true) HttpSession session,
			@ApiParam(value = "The transport request", required = true) @RequestBody TransportRequest transportRequest)
			throws UserNotFoundException {
		logger.info("Fetching user configuration information ... ");
		Long userId = SessionContext.getCurrentUserId(session);
		if (userId == null || (accountService.findOne(userId)) == null) {
			throw new UserNotFoundException();
		}
		GCUtil.performGC();
		return new TransportResponse(transportRequest.getTestStepId(), transportRequest.getMessage(), null);
	}

	public TestStepService getTestStepService() {
		return testStepService;
	}

	public void setTestStepService(TestStepService testStepService) {
		this.testStepService = testStepService;
	}

	public TransportMessageService getTransportMessageService() {
		return transportMessageService;
	}

	public void setTransportMessageService(TransportMessageService transportMessageService) {
		this.transportMessageService = transportMessageService;
	}

	public TransactionService getTransactionService() {
		return transactionService;
	}

	public void setTransactionService(TransactionService transactionService) {
		this.transactionService = transactionService;
	}

	public TransportConfigService getTransportConfigService() {
		return transportConfigService;
	}

	public void setTransportConfigService(TransportConfigService transportConfigService) {
		this.transportConfigService = transportConfigService;
	}

	public AccountService getAccountService() {
		return accountService;
	}

	public void setAccountService(AccountService accountService) {
		this.accountService = accountService;
	}

	public PasswordService getPasswordService() {
		return passwordService;
	}

	public void setPasswordService(PasswordService passwordService) {
		this.passwordService = passwordService;
	}

	@ResponseBody
	@ExceptionHandler(UserTokenIdNotFoundException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public String facilityIdNotFound(UserTokenIdNotFoundException ex) {
		logger.debug(ex.getMessage());
		return ex.getMessage();
	}

	@ResponseBody
	@ExceptionHandler(DuplicateTokenIdException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public String DuplicateFacilityIdException(DuplicateTokenIdException ex) {
		logger.debug(ex.getMessage());
		return ex.getMessage();
	}

}