package gov.nist.hit.pcd;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import gov.nist.validation.report.Entry;
import gov.nist.validation.report.Report;
import hl7.v2.profile.XMLDeserializer;
import hl7.v2.validation.SyncHL7Validator;
import hl7.v2.validation.content.ConformanceContext;
import hl7.v2.validation.content.DefaultConformanceContext;
import hl7.v2.validation.vs.ValueSetLibrary;
import hl7.v2.validation.vs.ValueSetLibraryImpl;

public class TestPCD04 {
	
	@Test
    public void validateTest() throws Exception {

    	// create validator
        SyncHL7Validator validator = createValidator();
        File f = new File(TestPCD04.class.getResource("/PCD04/message.hl7").toURI());
        System.out.println(f.exists());
        
        String messageString = FileUtils.readFileToString(f);

        Report report = validator.check(messageString, "5a328e8a84aec05fed001fd7");

        Set<String> keys = report.getEntries().keySet();
        for (String key : keys) {
            List<Entry> entries = report.getEntries().get(key);
            if (entries != null && entries.size() > 0) {
                System.out.println("*** " + key + " ***");
                for (Entry entry : entries) {
                    switch (entry.getClassification()) {
                    case "Error":
                        // case "Alert":
                        printEntry(entry);
                    }
                }
            }
        }
    }

    private static void printEntry(Entry entry) {
        if (entry instanceof gov.nist.validation.report.impl.EntryImpl) {
            System.out.println(entry);
        } else if (entry instanceof hl7.v2.validation.vs.EnhancedEntry) {
            System.out.println(entry.toText());
        }
    }

    private static SyncHL7Validator createValidator()
            throws Exception {

        // The profile in XML
        InputStream profileXML =  TestPCD04.class.getResourceAsStream("/Global/Profile.xml");

        // The default conformance context file XML
        InputStream contextXML = TestPCD04.class.getResourceAsStream("/Global/Constraints.xml");

        // The value set library file XML
        InputStream vsLibXML = TestPCD04.class.getResourceAsStream("/Global/ValueSets.xml");
        
        // The test case specific conformance context file XML
        File specificConstraintsFile = null;
        List<InputStream> confContexts = null;
        if (specificConstraintsFile != null && specificConstraintsFile.exists()) {
            InputStream specificContextXML = new FileInputStream(
                    specificConstraintsFile);
            confContexts = Arrays.asList(contextXML, specificContextXML);
        } else {
            confContexts = Arrays.asList(contextXML);
        }

        ConformanceContext context = DefaultConformanceContext.apply(confContexts).get();
        hl7.v2.profile.Profile profile = XMLDeserializer.deserialize(profileXML).get();
        ValueSetLibrary valueSetLibrary = ValueSetLibraryImpl.apply(vsLibXML).get();

        return new SyncHL7Validator(profile, valueSetLibrary, context);

    }
	
}
