import org.apache.jena.ontology.*;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.*;
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
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


// This program loads sample data from a database, converts it to instances of the Ontology classes
// and then runs a SPARQL query on the data
public class SemanticFishData {

	private static OntModel onto;
	
	// Define the namespaces we'll use later
	static final String marineInstitute   = "http://www.marine.ie/SemanticFishData#";
	
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
	static final String prefixString = "PREFIX mi:   <" + marineInstitute + ">\r\n"
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
	static OntClass samplingSummary;
	//static OntClass bioSample;
	static OntClass species;
	static OntClass concept;
	
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
			
			// Set this to true if you want to download the vocab data from the remote servers
			// or false if you have the vocab data already saved to file (quicker)
			boolean loadVocabfromServer = false;
			
			if (refreshData) {			
				onto = loadDataFromSource(loadVocabfromServer);
			} else {				
				//onto = loadDataFromTDB();
				onto = loadDataFromRDF();
			}
			
			//System.out.println("See what the ontology knows about sample 2017-1-27.4.a-PTM-127023");
			//showInstances(marineInstitute + "2017-1-27.4.a-PTM-127023",samplingSummary,true);
			//showInstances("",bioSample,false);
				
			// STEP 2) RUN SPARQL QUERIES
			
			System.out.println("Run SPARQL queries");
			String myQueryString = "";
		
			// Look at some records 
			myQueryString = "SELECT ?s\r\n" + 
					"WHERE\r\n" + 
					"   { ?s rdf:type mi:SamplingSummary }\r\n" + 
					"LIMIT 2";
			executeSPARQL(prefixString + " " + myQueryString);
			
			// Look at some MAC records 
			myQueryString = "SELECT ?s\r\n" + 
					"WHERE\r\n" + 
					"   { ?s mi:isOfSpecies SpecWoRMS:127023}\r\n" + 
					"LIMIT 5";
			//executeSPARQL(prefixString + " " + myQueryString);
			
			myQueryString = "SELECT ?s ?f\r\n" + 
					"WHERE\r\n" + 
					"   { ?s mi:isOfSpecies ?f .\r\n" + 
					"     ?f owl:sameAs SpecASFIS:MAC .\r\n" + 
					"}\r\n" + 
					"LIMIT 5";
			//executeSPARQL(prefixString + " " + myQueryString);
			
			myQueryString = "SELECT ?s \r\n" + 
					"WHERE\r\n" + 
					"   { ?s mi:isOfSpecies SpecASFIS:MAC .\r\n" + 
					"}\r\n" + 
					"LIMIT 5";
			//executeSPARQL(prefixString + " " + myQueryString);
			
			// Show all things that are sameAs each other 
			myQueryString = "SELECT ?n1 ?a ?n2 ?b\r\n" + 
					"WHERE { \r\n" + 
					"?a owl:sameAs ?b . \r\n" + 
					"OPTIONAL {?a skos:prefLabel ?n1 } .\r\n" + 
					"OPTIONAL {?b skos:prefLabel ?n2 } .\r\n" + 
					"}";
			//executeSPARQL(prefixString + " " + myQueryString);
			
	
												
			// Pull an image and abstract through from DBPedia
            //- need to use the SERVICE keyword to point to the dbpedia SPARQL end point
			// This works but is slow - load the needed data instead?
			myQueryString = "select ?samp ?species ?bin_name ?db_species ?image ?abstract\r\n" + 
					"where \r\n" + 
					"{\r\n" + 
					"    ?samp a mi:SamplingSummary .\r\n" + 
					"    ?samp mi:isOfSpecies ?species .\r\n" + 
					"    ?species mi:binomialUntyped ?bin_name .\r\n" + 
					"    ?db_species mi:binomialUntyped ?bin_name.\r\n" + 
					"    ?db_species dbp:binomial ?typed_name.\r\n" + 
					"   \r\n" + 
					"SERVICE <http://dbpedia.org/sparql> 					 \r\n" + 
					"    {\r\n" + 
					"        ?db_species dbo:thumbnail ?image  .\r\n" + 
					"        ?db_species dbo:abstract ?abstract  .\r\n" + 
					"    }\r\n" + 
					"    FILTER (lang(?abstract) = 'en')\r\n" + 
					"\r\n" + 
					"}";
			//executeSPARQL(prefixString + " " + myQueryString);
			
			
			// STEP 3) Save the data to TDB if we have refreshed it
			// This take a lot memory so I've commented it out for now
			//if (refreshData) saveDataToTDB();
			
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
	private static OntModel loadDataFromSource(boolean loadfromServer) {
		
		// STEP 1) LOAD IN OUR ONTOLOGY FROM FILE
		
		// Ontology model class which uses the micro OWL inference engine
		onto = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF, null );
		//onto = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RULE_INF , null );
			
		// Load vocab data
		loadVocabData(loadfromServer);
			
		//onto.write(System.out, "N3") ;
		
		// Read in the ontology from file
		System.out.println("Load ontology from file");
		onto.read( "file:resources/SamplingSummary.n3", "N3" );
		
		
		// define our bioSample and species classes using the relevent ontology class definitions
		samplingSummary = onto.getOntClass (marineInstitute + "SamplingSummary");
		//bioSample = onto.getOntClass (marineInstitute + "BioSample");
		species = onto.getOntClass (marineInstitute + "Species");
		
		//System.out.println("BioSample class: " + bioSample.toString());
	
		// STEP 2) LOAD IN OUR INSTANCE DATA
							
		System.out.println("Load data from database and convert to objects in the ontology");
		getInstanceData();
		

		
		return onto;
		
	}
	
	// Load the onto OntModel object from TDB
	// Not used at the moment because we are just loading/saving to RDF
	private static OntModel loadDataFromTDB() {
		
		System.out.println("Read data from TDB");
		
		// Directory for the TDB database
		String directory = "../MyDatabase"; 
		
		// This will open a TDB database if it already exists or create a new one
		tdbDataset = TDBFactory.createDataset(directory);
		
		// This will get the data using the name we saved it with earlier
		Model tdb = tdbDataset.getNamedModel(marineInstitute + "onto");

		onto = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF,tdb);
		
		// define our bioSample and species classes using the relevent ontology class definitions
		//bioSample = onto.getOntClass (marineInstitute + "BioSample");
		samplingSummary = onto.getOntClass (marineInstitute + "BioSample");
		species = onto.getOntClass (marineInstitute + "Species");
		
		System.out.println("Data should now be loaded successfully");
		
		// We'll open a READ transaction at this point
		tdbDataset.begin(ReadWrite.READ);
		
		return onto;
		
	}
	
	// Load the onto OntModel object from TDB
	private static OntModel loadDataFromRDF() {
		
		System.out.println("Read data from RDF");
		

		onto = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF, null );
		
		// define our bioSample and species classes using the relevent ontology class definitions
		//bioSample = onto.getOntClass (marineInstitute + "BioSample");
		samplingSummary = onto.getOntClass (marineInstitute + "BioSample");
		species = onto.getOntClass (marineInstitute + "Species");
		
		onto.read( "file:"+OUTPUT_FILE, "RDFXML" );	
		System.out.println("Data should now be loaded successfully");
	
		return onto;
		
	}
	
	// Save our onto OntModel to TDB (note that we delete any data that is already there first)
	// Not used at the moment because we are just loading/saving to RDF
	private static void saveDataToTDB() {
		
		// Step 4 - TDB
		
		System.out.println("Saving data to TDB");
		
		// Directory for the TDB database
		String directory = "../MyDatabase" ;

		// Check whether we already have tdbDataset - thsi will be the case if we loaded the data from TDB
		// iF we don't have tdbDataset create it
		if (tdbDataset == null)	{
			// This will open a TDB database if it already exists or create a new one
			tdbDataset = TDBFactory.createDataset(directory);
		} 
		// else if we do have it commit any transactions
		else {
			tdbDataset.commit();
		}
		
		// We want to write so we need to create a WRITE transaction
		tdbDataset.begin(ReadWrite.WRITE);
		
		// If data already exists in TDB we'll remove it first - otherwise when we try and save our new data
		// we'll still have all the old data in the TDB....
		Model tdb = tdbDataset.getNamedModel(marineInstitute + "onto");
		if (tdb != null) {
			tdb.removeAll();
		}
		
		
		// Add our ontology to the TDB
		tdbDataset.addNamedModel(marineInstitute + "onto", onto);
		
		// Commit the changes
		tdbDataset.commit();
	}
	
	// Execute a SPARQL query against the OntModel
	private static void executeSPARQL(String myQueryString) {
		
		System.out.println("My SPARQL query: " + myQueryString);
		
		// Execute the query and display the results
		QueryExecution qe = QueryExecutionFactory.create (myQueryString,onto);
		ResultSetFormatter.out(qe.execSelect());
		
	}
	
	// Call the methods to load our vocabulary data from remote sources
	private static void loadVocabData(boolean loadfromServer) {
		
		
		if (loadfromServer) {
			
			System.out.println("Loading vocab data from remote servers");
			loadICESVocabData();
			loadDBpediaData();
			
	        // save the vocab data to file
	        try (OutputStream myFile = new FileOutputStream(VOCAB_FILE)) {
	        	 
				onto.write(myFile, "RDFXML") ;
	 
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
		}
		else {
			System.out.println("Loading vocab data from file: " + VOCAB_FILE);
			onto.read( "file:"+VOCAB_FILE, "RDFXML" );		
		}
		

	}
	
	// Load some data from DBPedia - this is neccessary becasue I had trouble matching string literals of different types in SPARQL queries
	// Instead I add in a property which contains the scientific name of a species stripped of formatting infromation and converted to lower case.
	private static void loadDBpediaData() {
		
		System.out.println("Loading DBpedia data");
		
		//There's too much DBPedia to load in 1 go so we'll need to break our query up
		
		// First find how many records there are
		
		String myQueryString = "select (Str(count(distinct ?s)) as ?count)\r\n" + 
				"where {\r\n" + 
				"  ?s a dbo:Fish .\r\n" + 
				"  ?s dbp:binomial ?name .\r\n" + 
				"}";
		
		String dbpediaCountString = "";
		int dbpediaCount = 0;
		
		QueryExecution qe0 = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql",prefixString + " " +myQueryString);
		for (ResultSet rs = qe0.execSelect() ; rs.hasNext() ; ) {
		      QuerySolution binding = rs.nextSolution();
		      RDFNode myValue = binding.get("count");
              dbpediaCountString = myValue.toString();
		}
		//System.out.println("dbpediaCountString: " + dbpediaCountString);
		System.out.println("The following number of DBPedia fish records were found: " + dbpediaCountString);
		if (dbpediaCountString != "") {
			try {
				dbpediaCount = Integer.parseInt(dbpediaCountString);
			} catch (Exception ex) {
				dbpediaCount = 0;
				System.out.println(ex.getMessage());
			}
		}

		int dbpediaLimit = 5000;
		
		// If there is some data lets loop around and download chunks of it at a time
		if (dbpediaCount>0) {
			
			for (int i = 0; i * dbpediaLimit  < dbpediaCount; i++) {
			
			// Load the triples of fish with binomial names from dbpedia
			myQueryString = "construct {?s   dbp:binomial ?name} \r\n" + 
					"where {\r\n" + 
					"  ?s a dbo:Fish .\r\n" + 
					"  ?s dbp:binomial ?name .\r\n" + 
					"}\r\n" + 
					"order by ?s";
			
			// Add the limits and offset values to the query
			myQueryString = myQueryString + " LIMIT " + dbpediaLimit;
			if (i > 0) myQueryString = myQueryString + " OFFSET " + i * dbpediaLimit;
			//System.out.println(myQueryString);

			//System.out.println(prefixString + " " + myQueryString);
			QueryExecution qe = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql",prefixString + " " + myQueryString);
			
			// import the dbpedia data into our model
			qe.execConstruct(onto);
			qe.close() ;
			
			}
			
			
		}
		
		// Now we have the DBPedia data locally we'll add on our untyped scieitfic name property
		DatatypeProperty binomial = onto.createDatatypeProperty(dbp + "binomial");
		DatatypeProperty binomialUntyped = onto.createDatatypeProperty(marineInstitute + "binomialUntyped");
		
		List<String> dataToEdit = new ArrayList<String>();
		
		myQueryString = "select ?s   \r\n" + 
				"WHERE\r\n" + 
				"{	?s dbp:binomial ?name .\r\n" + 
				"}";
		
		QueryExecution qe2 = QueryExecutionFactory.create (prefixString + " " +myQueryString,onto);
		
		// Iterate through the results - can't update them directly through so save the IDs and we'll update them in a minute
		for (ResultSet rs = qe2.execSelect() ; rs.hasNext() ; ) {
		      QuerySolution binding = rs.nextSolution();
		      RDFNode myValue = binding.get("s");
		      dataToEdit.add(myValue.toString());
		      //System.out.println(myValue.toString());
		}
		
		// Add the new untyped scientific name property
		for (String currentRecord : dataToEdit) {

			  Individual myRec = onto.getIndividual(currentRecord);
			  if (myRec!= null &&  myRec.getPropertyValue(binomial) != null) {
			      String binomialWithType = myRec.getPropertyValue(binomial).toString();
			      String binomialWithoutType = "";
			      int matchPosition = binomialWithType.indexOf("^^");
			      if (matchPosition > -1) {
			    	  binomialWithoutType = binomialWithType.substring(0, matchPosition).toLowerCase();
			      } else {
			    	  binomialWithoutType = binomialWithType.toLowerCase();
			      }
			      myRec.addProperty(binomialUntyped, onto.createTypedLiteral(binomialWithoutType));
			  }
		}
		
	}
	
	// Call the methods to load data from the semantic ICES vocabulary server
	private static void loadICESVocabData() {
		

			loadICEScodeType("IC_GearClass");
			loadICEScodeType("IC_GearGroup");
			loadICEScodeType("IC_GearType");
			loadICEScodeType("IC_Divisions");
			loadICEScodeType("IC_Sub-areas");
			loadICEScodeType("IC_AreaTopLevel");
			loadICEScodeType("IC_WorkingGroup");
			loadICEScodeType("IC_Stock");
			loadICEScodeType("IC_Species");
			loadICEScodeType("SpecASFIS");
			loadICEScodeType("SpecWoRMS");

			
	}
	
    // Load in a particular code type from the semantic ICES vocabulary server
	// The ICES web services provide RDF data which can be read directly into our OntModel
	private static void loadICEScodeType(String codeType) {
		
		// Read data from the ICES vocabulary server
		System.out.println("Load " + codeType + " from ICES vocab server");
		
		// Read the IC_Divisions codetype first
		onto.read("https://vocab.ices.dk/services/rdf/collection/" + codeType,"RDF/XML");
		
		if(concept == null) concept = onto.getOntClass (skos + "concept");
		
		DatatypeProperty prefLabel = onto.createDatatypeProperty(skos + "prefLabel");
		DatatypeProperty binomialUntyped = onto.createDatatypeProperty(marineInstitute + "binomialUntyped");
		
		//onto.write(System.out, "N3") ;
		
		//showInstances("", concept,false);
		
		// For each code in the IC_Divisions codetype list get the data for the code
		// ( I coundn't just use the ExtendedIterator syntax diretly because reading the data for the code directly affects the list instances and causes an error)
		List<String> dataToFetch = new ArrayList<String>();
		
		for (ExtendedIterator<? extends OntResource>  bs = concept.listInstances(); bs.hasNext(); ) {
			  OntResource myRes = bs.next();
			  dataToFetch.add(myRes.toString());		      
		}
		
		for (String currentRecord : dataToFetch) {
			  onto.read(currentRecord,"RDF/XML");
			  
			  // For species we will add on an extra property to make comparisons easier later on
			  // This is the scientific name with formatting removed and converted to lower case
			  if (codeType == "IC_Species" || codeType == "SpecWoRMS" || codeType == "SpecASFIS") {
				  Individual myRec = onto.getIndividual(currentRecord);
				  if (myRec!= null &&  myRec.getPropertyValue(prefLabel) != null) {
				      String binomialWithType = myRec.getPropertyValue(prefLabel).toString();
				      String binomialWithoutType = "";
				      int matchPosition = binomialWithType.indexOf("@");
				      if (matchPosition > -1) {
				    	  binomialWithoutType = binomialWithType.substring(0, matchPosition).toLowerCase();
				      } else {
				    	  binomialWithoutType = binomialWithType.toLowerCase();
				      }
				      myRec.addProperty(binomialUntyped, onto.createTypedLiteral(binomialWithoutType));
				  }
			  }
			  
		}
		
		//showInstances("", concept,false);
		
		
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
			myQuery = new String(Files.readAllBytes(Paths.get("resources/SummaryQuery.sql")));
		} catch (IOException e) {
            System.out.println(e.getMessage());
		}
		
		return myQuery;
	}
	
	// Build a connection string to the Stockman database
    private static String getConnectionUrl() {
    	   
	    final String url = myProperties.getProperty("url");
	    final String serverName = myProperties.getProperty("serverName");
	    final String portNumber = myProperties.getProperty("portNumber");
	    final String databaseName = myProperties.getProperty("databaseName");

    	
        return url + serverName + ":" + portNumber + ";databaseName=" + databaseName ;
    }
    
    // Take data from a HashTable and build an instance of the BioSample class 
    private static boolean CreateSamplingSummary(Hashtable<String, String> myHT) {
    	
    	boolean success = false;
    	

		DatatypeProperty isOfYear = onto.createDatatypeProperty(marineInstitute + "isOfYear");
		DatatypeProperty isOfQuarter = onto.createDatatypeProperty(marineInstitute + "isOfQuarter");
		DatatypeProperty isOfSpecies = onto.createDatatypeProperty(marineInstitute + "isOfSpecies");
		DatatypeProperty isOfArea = onto.createDatatypeProperty(marineInstitute + "isOfArea");
		DatatypeProperty isOfGear = onto.createDatatypeProperty(marineInstitute + "isOfGear");
		DatatypeProperty DemSeaSchemeLengthObs = onto.createDatatypeProperty(marineInstitute + "DemSeaSchemeLengthObs");
		DatatypeProperty DemSeaSchemeAgeObs = onto.createDatatypeProperty(marineInstitute + "DemSeaSchemeAgeObs");
		DatatypeProperty DemSeaSchemeBioObs = onto.createDatatypeProperty(marineInstitute + "DemSeaSchemeBioObs");
		DatatypeProperty PelSeaSchemeLengthObs = onto.createDatatypeProperty(marineInstitute + "PelSeaSchemeLengthObs");
		DatatypeProperty PelSeaSchemeAgeObs = onto.createDatatypeProperty(marineInstitute + "PelSeaSchemeAgeObs");
		DatatypeProperty PelSeaSchemeBioObs = onto.createDatatypeProperty(marineInstitute + "PelSeaSchemeBioObs");
		DatatypeProperty PortSchemeLengthObs = onto.createDatatypeProperty(marineInstitute + "PortSchemeLengthObs");
		DatatypeProperty PortSchemeAgeObs = onto.createDatatypeProperty(marineInstitute + "PortSchemeAgeObs");
		DatatypeProperty PortSchemeBioObs = onto.createDatatypeProperty(marineInstitute + "PortSchemeBioObs");
    	
    	try {
    		if (onto != null && myHT != null)
    		{
    			// Create a new sample
    			String sampleID = myHT.get("SummaryID");
    			Individual myInd = onto.createIndividual (marineInstitute + sampleID, samplingSummary);
    			    			
    			String yearValue = myHT.get("Year");
    			if (yearValue!= null && yearValue != "") myInd.addProperty(isOfYear, onto.createTypedLiteral(yearValue));
    			
    			String quarterValue = myHT.get("Quarter");
    			if (quarterValue!= null && quarterValue != "") myInd.addProperty(isOfQuarter, onto.createTypedLiteral(quarterValue));
    			
    			String demLengthValue = myHT.get("Demersal At-Sea Scheme Length Observations");
    			if (demLengthValue!= null && demLengthValue != "") myInd.addProperty(DemSeaSchemeLengthObs, onto.createTypedLiteral(demLengthValue));
    			
    			String demAgeValue = myHT.get("Demersal At-Sea Scheme Age Observations");
    			if (demAgeValue!= null && demAgeValue != "") myInd.addProperty(DemSeaSchemeAgeObs, onto.createTypedLiteral(demAgeValue));
    			
    			String demBioValue = myHT.get("Demersal At-Sea Scheme Biological Observations");
    			if (demBioValue!= null && demBioValue != "") myInd.addProperty(DemSeaSchemeBioObs, onto.createTypedLiteral(demBioValue));
    			
    			String pelLengthValue = myHT.get("Pelagic At-Sea Scheme Length Observations");
    			if (pelLengthValue!= null && pelLengthValue != "") myInd.addProperty(PelSeaSchemeLengthObs, onto.createTypedLiteral(pelLengthValue));
    			
    			String pelAgeValue = myHT.get("Pelagic At-Sea Scheme Age Observations");
    			if (pelAgeValue!= null && pelAgeValue != "") myInd.addProperty(PelSeaSchemeAgeObs, onto.createTypedLiteral(pelAgeValue));
    			
    			String pelBioValue = myHT.get("Pelagic At-Sea Scheme Biological Observations");
    			if (pelBioValue!= null && pelBioValue != "") myInd.addProperty(PelSeaSchemeBioObs, onto.createTypedLiteral(pelBioValue));
    			
    			String portLengthValue = myHT.get("Port Sampling Scheme Length Observations");
    			if (portLengthValue!= null && portLengthValue != "") myInd.addProperty(PortSchemeLengthObs, onto.createTypedLiteral(portLengthValue));
    			
    			String portAgeValue = myHT.get("Port Sampling Scheme Age Observations");
    			if (portAgeValue!= null && portAgeValue != "") myInd.addProperty(PortSchemeAgeObs, onto.createTypedLiteral(portAgeValue));
    			
    			String portBioValue = myHT.get("Port Sampling Scheme Biological Observations");
    			if (portBioValue!= null && portBioValue != "") myInd.addProperty(PortSchemeBioObs, onto.createTypedLiteral(portBioValue));
    			
    			// Add species to sample - need to add it as an instance of the Species class, not just the aphiaid as text
    			String speciesValue = myHT.get("AphiaID");
    			if (speciesValue!= null && speciesValue != "") {
    				myInd.addProperty(isOfSpecies, onto.createIndividual(SpecWoRMS + speciesValue, concept));
    			}
    			   			
    			// Add ICES sub-division to sample - need to add it as an instance of the concept class, not just the division as text
    			String divisionValue = myHT.get("ICES Division");
    			if (divisionValue!= null && divisionValue != "") {
    				myInd.addProperty(isOfArea, onto.createIndividual(IC_Divisions + divisionValue, concept));
    			}
    			
    			String gearValue = myHT.get("Gear Code");
    			if (gearValue!= null && gearValue != "") {
    				myInd.addProperty(isOfGear, onto.createIndividual(IC_GearType + gearValue, concept));
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
                CreateSamplingSummary(myHT);
            	
                //String myTest = rs.getString("SummaryID");
                //System.out.println(myTest);
            }
            conn.close();
        } catch (Exception e) {
            System.out.println("Error: " +  e.getMessage());
        }
	}

}
