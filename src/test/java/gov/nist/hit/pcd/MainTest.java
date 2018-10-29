package gov.nist.hit.pcd;

import gov.nist.hit.pcd.custom.ResourceHandler;

public class MainTest {

	public void TestPCD01() {

		ResourceHandler rl = new ResourceHandler();

		System.out.println(rl.codeExists("70071"));
		System.out.println(rl.dupletIsValid("70071", "MDC_DEV_PUMP_INFUSATE_SOURCE_PRIMARY"));

	}

}
