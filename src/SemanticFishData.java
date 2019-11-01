import org.apache.jena.ontology.*;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.*;
import org.apache.jena.reasoner.ValidityReport;
import org.apache.jena.reasoner.ValidityReport.Report;
import org.apache.jena.tdb.TDBFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.jena.util.iterator.ExtendedIterator;
import java.sql.*;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;


// This program loads sample data from a database, converts it to instances of the Ontology classes
// and then runs a SPARQL query on the data
public class SemanticFishData {

	private static OntModel onto;
	
	// Define the namespaces we'll use later
	//static final String marineInstitute   = "http://www.marine.ie/SemanticFishData#";
	static final String ak   = "http://www.semanticweb.org/akennedy/ontologies/2019/9/untitled-ontology-16#";
	
	static final String worms = "urn:lsid:marinespecies.org:taxname:";
	static final String skos = "http://www.w3.org/2004/02/skos/core#";
	static final String icesVocab = "https://vocab.ices.dk/services/rdf/collection/";
	static final String IC_Divisions = icesVocab + "IC_Divisions/";
	static final String IC_Sub_areas = icesVocab + "IC_Sub-areas/";
	static final String IC_AreaTopLevel = icesVocab + "IC_AreaTopLevel/";
	static final String IC_Species = icesVocab + "IC_Species/";
	static final String IC_GearType = icesVocab + "IC_GearType/";
	static final String SpecWoRMS = icesVocab + "SpecWoRMS/";
	static final String SpecASFIS = icesVocab + "SpecASFIS/";
	static final String dbo = "http://dbpedia.org/ontology/";
	static final String dbp = "http://dbpedia.org/property/";
	static final String dbr = "http://dbpedia.org/resource/";
	static final String rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	static final String rdfs = "http://www.w3.org/2000/01/rdf-schema#";
	static final String owl = "http://www.w3.org/2002/07/owl#";
	static final String dct = "http://purl.org/dc/terms/";
	static final String foaf = "http://xmlns.com/foaf/0.1/";
	static final String prov = "http://www.w3.org/ns/prov#";
	static final String ns8 = "http://purl.org/linguistics/gold/";

	
	// We'll use this prefix in our SPARQL queries
	static final String prefixString = "PREFIX ak:   <" + ak + ">\r\n"
			+"PREFIX IC_Divisions: <" + IC_Divisions + ">\r\n" 
			+"PREFIX IC_Sub-areas: <" + IC_Sub_areas + ">\r\n"
			+"PREFIX IC_AreaTopLevel: <" + IC_AreaTopLevel + ">\r\n"
			+"PREFIX IC_Species: <" + IC_Species + ">\r\n"
			+"PREFIX IC_GearType: <" + IC_GearType + ">\r\n"
			+"PREFIX SpecWoRMS: <" + SpecWoRMS + ">\r\n"
			+"PREFIX SpecASFIS: <" + SpecASFIS + ">\r\n"
			+"PREFIX skos:<" + skos +">\r\n"
			+"PREFIX dbo: <" + dbo + ">\r\n"
			+"PREFIX dbp: <" + dbp + ">\r\n"
			+"PREFIX dbr: <" + dbr + ">\r\n"
			+"PREFIX rdf: <" + rdf + ">\r\n"
			+"PREFIX owl: <" + owl + ">\r\n";

	// Some classes from our ontology
	//static OntClass samplingSummary;
	static OntClass sample;
	//static OntClass bioSample;
	static OntClass species;
	static OntClass concept;
	static OntClass ICESDivision;
	static OntClass LandingDate;
	static OntClass Measurement;
	static OntClass Observation;
	static OntClass Quality;
	static OntClass Unit;
	
	static Dataset tdbDataset;
	
	// used for storing config data (e.g. user names, password, Url etc)
	static Properties myProperties;
	
	static final String OUTPUT_FILE = "resources/MyOntologyData.rdf";
	static final String VOCAB_FILE = "resources/MyVocabData.rdf";
	
	// Main function
	public static void main(String[] args) {
		
		try {
			
			System.out.println("Started");
			
			// STEP 0) LOAD CONFIGURATION FROM FILE
			myProperties = getConfigFile("resources/app.config");
			
				
			// STEP 1) LOAD THE DATA
			// Either load the data from source, or if you have already saved the data to an RDF file you can just load it from there which will be quicker
			// Change the value of refreshData as required		
			boolean refreshData = true;
			
			
			if (refreshData) {			
				onto = loadDataFromSource();
			} else {				
				onto = loadDataFromRDF();
			}
			
			
			// STEP 2) CHECK IF OUR ONTMODEL DATA IS VALID
			checkValidity(onto);
			

			
			// STEP 3) RUN SOME SPARQL QUERIES SO WE CAN SEE WHAT THE DATA LOOKS LIKE
			
			System.out.println("Run SPARQL queries");
			String myQueryString = "";
		
			// Look at some records 
			myQueryString = "SELECT ?s\r\n" + 
					"WHERE\r\n" + 
					"   { ?s rdf:type ak:Sample }\r\n" + 
					"LIMIT 2";
			executeSPARQL(prefixString + " " + myQueryString);
			
			// Look at some records 
			myQueryString = "SELECT ?s\r\n" + 
					"WHERE\r\n" + 
					"   { ?s rdf:type ak:Species }\r\n" + 
					"LIMIT 2";
			executeSPARQL(prefixString + " " + myQueryString);
			
			// Look at some records 
			myQueryString = "SELECT ?s\r\n" + 
					"WHERE\r\n" + 
					"   { ?s rdf:type ak:ICESDivision }\r\n" + 
					"LIMIT 2";
			executeSPARQL(prefixString + " " + myQueryString);
			
			// Look at some mackerel samples 
			myQueryString = "SELECT ?s\r\n" + 
					"WHERE\r\n" + 
					"   { ?s ak:hasSpecies <http://www.semanticweb.org/akennedy/ontologies/2019/9/untitled-ontology-16#Species/127023>}\r\n" + 
					"LIMIT 5";
			executeSPARQL(prefixString + " " + myQueryString);
			
						
			// STEP 4) Save the data to a RDF format text file if we have refreshed it
			if (refreshData) {
		        System.out.println("Save the data to an RDF/XML format text file");
		                
		        try (OutputStream myFile = new FileOutputStream(OUTPUT_FILE)) {
		        	 
					onto.write(myFile, "RDFXML") ;
		 
		        } catch (IOException e) {
		            e.printStackTrace();
		        }
	        
			}
	        			
			// finished
	        System.out.println("Finished");
        
        
		} 
		// Print out any errors
		catch (Exception ex) {
			
			System.out.println("Error: " + ex.getMessage());
			ex.printStackTrace();
		}

	}
	
	// Load some settings in from file
	private static Properties getConfigFile(String fileName) {
		
		// The Properties file should be in the following format
		//			url=jdbc:sqlserver://
		//			serverName=YourDatabaseServerName
		//			portNumber=1234
		//			databaseName=YourDatabaseName
		//			userName=YourUserName
		//			password=YourPassword
		
		System.out.println("Load config from: " + fileName);
		
		Properties prop = new Properties();
		
	    try {
	        InputStream is = new FileInputStream(fileName);
	
	        // load the properties file
	        prop.load(is);
	
        
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	    
	    return prop;
		
	}
	
	// Load data from their original soruces - use this the first time you run the code of if you have changed anythign and need to refresh your data
	private static OntModel loadDataFromSource() {
		
		// STEP 1) LOAD IN OUR ONTOLOGY FROM FILE
		
		// Ontology model class which uses the micro OWL inference engine
		onto = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF, null );
		//onto = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RULE_INF , null );
				
		// Read in the ontology from file
		System.out.println("Load ontology from file");
		onto.read( "file:resources/SimpleFish_1710_turt.owl", "TURTLE" );
		
			
		// STEP 2) LOAD IN OUR INSTANCE DATA
							
		System.out.println("Load data from database and convert to objects in the ontology");
		getInstanceData();
				
		return onto;
		
	}
	
	// Load the onto OntModel object from RDF
	private static OntModel loadDataFromRDF() {
		
		System.out.println("Read data from RDF");
		
		onto = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF, null );
				
		onto.read( "file:"+OUTPUT_FILE, "RDFXML" );	
		System.out.println("Data should now be loaded successfully");
	
		return onto;
		
	}
	
	
	
	// Execute a SPARQL query against the OntModel
	private static void executeSPARQL(String myQueryString) {
		
		System.out.println("My SPARQL query: " + myQueryString);
		
		// Execute the query and display the results
		QueryExecution qe = QueryExecutionFactory.create (myQueryString,onto);
		ResultSetFormatter.out(qe.execSelect());
		
	}
	
	
	
	// Print an instance of a specified OntClass - if a blank string is passed in then all instances of that OntClass will be printed out
	private static void showInstances(String instanceID, OntClass oc, boolean ShowProperties ) {

		try {
			// Iterate through the data we just read in and see what's there
			for (ExtendedIterator<? extends OntResource>  bs = oc.listInstances(); bs.hasNext(); ) {
				  OntResource myRes = bs.next();
				  if (instanceID == "" || instanceID.equalsIgnoreCase(myRes.toString())) {
			      System.out.println("Instance " + myRes.toString());
			      	if (ShowProperties) {
						for (StmtIterator myIt = myRes.listProperties();myIt.hasNext();) {
						      System.out.println("Property" + myIt.next().toString());
						}					
					}
				  }
			      
			}
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
		
		//System.out.println("Finished showInstances");			
	}
	
	
	// Load an SQL query from file
	private static String getQuery() {
		
		String myQuery = "";

		try {
			// This query just gets a sample of the data
			//myQuery = new String(Files.readAllBytes(Paths.get("resources/SummaryQuery.sql")));
			myQuery = new String(Files.readAllBytes(Paths.get("resources/StockmanQuery.sql")));
		} catch (IOException e) {
            System.out.println(e.getMessage());
		}
		
		return myQuery;
	}
	
	// Build a connection string to the required database
    private static String getConnectionUrl() {
    	   
	    final String url = myProperties.getProperty("url");
	    final String serverName = myProperties.getProperty("serverName");
	    final String portNumber = myProperties.getProperty("portNumber");
	    final String databaseName = myProperties.getProperty("databaseName");

    	
        return url + serverName + ":" + portNumber + ";databaseName=" + databaseName ;
    }
    
    // Take data from a HashTable and build an instance of the BioSample class 
    private static boolean CreateSampleRecord(Hashtable<String, String> myHT) {
    	
    	boolean success = false;
    	
    	// Define our classes, based on the classes in the ontology
		sample = onto.getOntClass (ak + "Sample");
		species = onto.getOntClass (ak + "Species");
    	ICESDivision = onto.getOntClass (ak + "ICESDivision");
    	LandingDate = onto.getOntClass (ak + "LandingDate");
    	Measurement = onto.getOntClass (ak + "Measurement");
    	Observation = onto.getOntClass (ak + "Observation");
    	Quality = onto.getOntClass (ak + "Quality");
    	Unit = onto.getOntClass (ak + "Unit");
    	
    	// Define our properties, based on the properties in the ontology
    	ObjectProperty hasDivision = onto.getObjectProperty(ak + "hasDivision");
    	ObjectProperty hasLandingDate = onto.getObjectProperty(ak + "hasLandingDate");
    	ObjectProperty hasMeasurement = onto.getObjectProperty(ak + "hasMeasurement");	
    	ObjectProperty hasObservation = onto.getObjectProperty(ak + "hasObservation");
    	ObjectProperty hasQuality = onto.getObjectProperty(ak + "hasQuality");
    	ObjectProperty hasSpecies = onto.getObjectProperty(ak + "hasSpecies");
    	ObjectProperty hasUnit = onto.getObjectProperty(ak + "hasUnit");
		
	
    	try {
    		if (onto != null && myHT != null)
    		{
    			// Create a new sample
    			String sampleID = myHT.get("SampleID");
    			//Individual myInd = onto.createIndividual (marineInstitute + sampleID, samplingSummary);
    			Individual myInd = onto.createIndividual (ak + "Sample/" + sampleID, sample);
    			    			
    			// Add ICES sub-division to sample - need to add it as an instance of the ICESDivision class, not just the division as text
    			String divisionValue = myHT.get("ICES_Div");
    			if (divisionValue!= null && divisionValue != "") {
    				// See if we need to create an individual for this division - if so, do it
    				String divName = ak  + "ICESDivision/" + divisionValue;
    				Individual myDiv = null;
    				myDiv = onto.getIndividual(divName);
    				if (myDiv == null ) myDiv = onto.createIndividual (divName, ICESDivision);
    				// Now add the division as a property to the sample
    				myInd.addProperty(hasDivision, myDiv);
    			}
    			
    			// Add species to sample - need to add it as an instance of the Species class, not just the aphiaid as text
    			String speciesValue = myHT.get("AphiaID");
    			if (speciesValue!= null && speciesValue != "") {
    				// See if we need to create an individual for this species - if so, do it
    				String specName = ak  + "Species/" + speciesValue;
    				Individual mySpec = null;
    				mySpec = onto.getIndividual(specName);
    				if (mySpec == null ) mySpec = onto.createIndividual (specName, species);
    				// Now add the division as a property to the sample
    				myInd.addProperty(hasSpecies, mySpec);
    			}
    			
    			// We'll use the value of length for the hasMeasurement property - just a float
    			// We'll also add in a Unit - cm for length
    			String lengthValue = myHT.get("FishLength");
    			if (lengthValue!= null && lengthValue != "") {
    				
    				myInd.addProperty(hasMeasurement, onto.createTypedLiteral(lengthValue));
    				
    				// Unit
    				String unitName = ak  + "Unit/" + "cm";
    				Individual myUnit = null;
    				myUnit = onto.getIndividual(unitName);
    				if (myUnit == null ) myUnit = onto.createIndividual (unitName, Unit);
    				// Now add the Unit as a property to the sample
    				myInd.addProperty(hasUnit, myUnit);
    				
    				// Observation
    				String obsName = ak  + "Observation/" + "LengthMeasurement";
    				Individual myObs = null;
    				myObs = onto.getIndividual(obsName);
    				if (myObs == null ) myObs = onto.createIndividual (obsName, Observation);
    				// Now add the Observation as a property to the sample
    				myInd.addProperty(hasObservation, myObs);
    				
    				// Quality
    				String qualityName = ak  + "Quality/" + "Length";
    				Individual myQual = null;
    				myQual = onto.getIndividual(qualityName);
    				if (myQual == null ) myQual = onto.createIndividual (qualityName, Quality);
    				// Now add the Quality as a property to the sample
    				myInd.addProperty(hasQuality, myQual);
    				
    			}
    			
    			// Add Sample Date to sample 
    			String landingdateValue = myHT.get("SampleDate");
    			if (landingdateValue!= null && landingdateValue != "") {
    				
    				// Convert the text value to a date then create a typed literal for it
    				java.util.Date myDate = new SimpleDateFormat("yyyy-MM-dd").parse(landingdateValue.substring(0, 10));
    				String myDateString =  new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(myDate).toString(); 
    				myInd.addProperty(hasLandingDate, onto.createTypedLiteral(myDateString,"xsd:dateTimeStamp"));
    				
    			}
    			    			

    		}
    		
    		
    	} catch (Exception ex) {
    		System.out.println(ex.getMessage());
    	}
    	
    	return success;
    	
    }
	
    // Import data by connecting to the database, running a query, and then building instances of ontology objects
    // from that data
	private static void getInstanceData() {
		
		// Need to have a JDBC driver 
		// e.g. Download the driver from https://docs.microsoft.com/en-us/sql/connect/jdbc/microsoft-jdbc-driver-for-sql-server?view=sql-server-2017
		// Then add one of the downloaded jdbc JAR to your build path (you need to make sure its the JAR targeting the correct JRE)
		    
	    final String userName = myProperties.getProperty("userName");
	    final String password = myProperties.getProperty("password");
		
        try {
        	Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        	
        	//System.out.println(getConnectionUrl());
            Connection conn = DriverManager.getConnection(getConnectionUrl(), userName, password);
            //System.out.println("Made connection");
            Statement stmt = conn.createStatement();
            java.sql.ResultSet rs;
 
            rs = stmt.executeQuery(getQuery());
            //System.out.println("Run query");
            
            ResultSetMetaData metadata = rs.getMetaData();
            int columnCount = metadata.getColumnCount();
            //System.out.println(columnCount);
            
            while ( rs.next() ) {
            
            	// Use a Hastable to store the row data
            	Hashtable<String, String> myHT = new Hashtable<String, String>();
            	
            	
                for (int i = 1; i <= columnCount; i++) {
                	// if the value is null, replace it with a blank space instead
                	String valueToUse = rs.getString(i) == null ? "":  rs.getString(i);  
                	//System.out.println(valueToUse);
                    myHT.put(metadata.getColumnName(i),valueToUse   );
                }
                
                // Create an  ontology object
                CreateSampleRecord(myHT);
            	
                //String myTest = rs.getString("SummaryID");
                //System.out.println(myTest);
            }
            conn.close();
        } catch (Exception e) {
            System.out.println("Error: " +  e.getMessage());
        }
	}
	
	// Check is the data in our OntModel is valid (e.g. property values are in the correct range)
	private static void checkValidity(OntModel onto) {
		
		// Check if our data is valid
		ValidityReport validity = onto.validate();
		if (validity.isValid()) {
		    System.out.println("Ontology data validates");
		} else {
		    System.out.println("Ontology data does not validate");
		    for (Iterator<Report> i = validity.getReports(); i.hasNext(); ) {
		        System.out.println(" - " + i.next());
		    }
		    // Stop the program if data isn't valid
		    System.exit(1);
		}
	
	}

}
