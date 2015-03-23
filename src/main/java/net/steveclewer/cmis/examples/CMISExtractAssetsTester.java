package net.steveclewer.cmis.examples;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

import org.alfresco.cmis.client.AlfrescoDocument;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.client.api.QueryResult;
import org.apache.chemistry.opencmis.client.api.Repository;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;

/**
 * Extract Assets tester.
 */
public class CMISExtractAssetsTester extends ExampleBase {
	
	public static final String ZIP_TYPE = "application/zip";
	
	public static final String TEXT_TYPE = "text/plain";
	
	public static final String STATUS_PENDING = "PENDING";
	
	public static final String STATUS_COMPLETE = "COMPLETE";
	
	public static final String EXTRACT_ASSETS_PATH ="/Data Dictionary/Jobs/Extract Assets";
	
	public static final boolean ADD_JOBS_AFTER_UPLOADS = true;
	
    private String serviceUrl = "http://localhost:8082/alfresco/cmisatom"; // Uncomment for Atom Pub binding
    //private String serviceUrl = "http://localhost:8080/alfresco/cmis"; // Uncomment for Web Services binding
    //private String serviceUrl = "http://localhost:8080/alfresco/api/-default-/public/cmis/versions/1.0/atom"; // Uncomment for Atom Pub binding
    //private String serviceUrl = "http://localhost:8080/alfresco/api/-default-/public/cmis/versions/cmisws"; // Uncomment for Web Services binding
	//private String serviceUrl = "http://jpotts.alfresco-laptop.com:8081/chemistry/browser";

    private Session session = null;

	public Session getSession() {
		if (session == null) {
			// default factory implementation
			SessionFactory factory = SessionFactoryImpl.newInstance();
			Map<String, String> params = new HashMap<String, String>();
	
			// user credentials
			params.put(SessionParameter.USER, getUser());
			params.put(SessionParameter.PASSWORD, getPassword());
	
			// connection settings
			params.put(SessionParameter.ATOMPUB_URL, serviceUrl); // Uncomment for Atom Pub binding
			params.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value()); // Uncomment for Atom Pub binding
			
			// Set the alfresco object factory. Used when using the CMIS extension for Alfresco for working with aspects
			params.put(SessionParameter.OBJECT_FACTORY_CLASS, "org.alfresco.cmis.client.impl.AlfrescoObjectFactoryImpl");

			List<Repository> repositories = factory.getRepositories(params);
			this.session = repositories.get(0).createSession();
		}

		return this.session;
	}

	/**
	 * Create a zip file in the repository.
	 * @param docName Name of document
	 * @param contentType Type to add
	 */
	public Document uploadExtractionZip(final String uploadPath, final String sourceZip, final String suffix) {	
		Session session = getSession();	

		Folder zipParent = (Folder)session.getObjectByPath(uploadPath);
				
		String timeStamp = new Long(System.currentTimeMillis()).toString();
		
		String zipFileName = timeStamp + "_" + suffix;
				
		try {
			Map <String, Object> props = new HashMap<String, Object>();
			props.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");		
			props.put(PropertyIds.NAME, zipFileName);
			props.put(PropertyIds.CHANGE_TOKEN, true);
					
			File sourceFile = new File(sourceZip);		

			DataInputStream dis = new DataInputStream(new FileInputStream(sourceFile));
			byte[] bytes = new byte[(int) sourceFile.length()];
			dis.readFully(bytes);
			ContentStream zipContentStream = session.getObjectFactory().createContentStream(sourceFile.getAbsolutePath(), Long.valueOf(bytes.length), ZIP_TYPE, new ByteArrayInputStream(bytes));
			
			Document zipDoc = zipParent.createDocument(props, zipContentStream, VersioningState.MAJOR);
			
			dis.close();
			
			return zipDoc;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
    }
	
	/**
	 * Create extraction job.
	 */
	private void createExtractionJob(final String uploadPath, final Document zipDoc) {
		String jobFileName = zipDoc.getName() + "_cmisJob";
		
		try {		
			AlfrescoDocument zipDocument = (AlfrescoDocument)zipDoc;
			String zipRef = (String)zipDocument.getPropertyValue("alfcmis:nodeRef");		
	
			Map <String, Object> properties = new HashMap<String, Object>();
			properties.put(PropertyIds.OBJECT_TYPE_ID, "D:bulkload:extractAssetsJob,P:bulkload:jobStatus,P:cm:titled");		
			properties.put(PropertyIds.NAME, jobFileName);
			properties.put(PropertyIds.DESCRIPTION, "This is a test job submitted via CMIS");		
			properties.put("bulkload:jobStatusMessage", "CMIS test extraction.");
			properties.put("bulkload:jobStatus", STATUS_PENDING);
			properties.put("bulkload:compressedFilename", zipDoc.getName());
			properties.put("bulkload:compressedFilePath", "/Company Home" + uploadPath);
			properties.put("bulkload:compressedFileRef", zipRef);	
			properties.put("cm:description", "This is a test job submitted via CMIS");	
			properties.put("cm:title", "CMIS extract");
			
			String docText = "This is a sample " + TEXT_TYPE + " document called " + jobFileName;
			byte[] content = docText.getBytes();
			InputStream stream = new ByteArrayInputStream(content);
			ContentStream contentStream = session.getObjectFactory().createContentStream(jobFileName, Long.valueOf(content.length), TEXT_TYPE, stream);
				
			// Grab a reference to the folder where we want to create content
			Folder jobFolder = (Folder)session.getObjectByPath(EXTRACT_ASSETS_PATH);
			
			Document jobDoc = jobFolder.createDocument(properties, contentStream, VersioningState.MAJOR);
			System.out.println("\nZip name: " + zipDoc.getName());
			System.out.println("Job name: " + jobDoc.getName());
		} catch (Exception e)	{
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		
		CMISExtractAssetsTester extractAssetsTester = new CMISExtractAssetsTester();
		extractAssetsTester.setUser("admin");
		extractAssetsTester.setPassword("passw0rd");
		
		List<String> uploadFolders = new ArrayList<String>();
		uploadFolders.add("/Sites/steve-cpa-site/documentLibrary/wip");
		//uploadFolders.add("/Sites/steve-cpa-site/documentLibrary/wip2");
		//uploadFolders.add("/Sites/steve-cpa-site/documentLibrary/wip3");
		//uploadFolders.add("/Sites/steve-cpa-site/documentLibrary/wip4");
		
		final String testDir = "C:/Users/sclewer/Desktop/CENGAGE/zip extract test files/cpa/auto/";
		int jobCount = 1;		
	
		List<ExtractJob> extractJobs = new ArrayList<ExtractJob>();
            
		try {      
			File f = new File(testDir);
			String[] files = f.list();

			for (int i = 0; i < jobCount; i++) {
				for (String uploadFolder : uploadFolders) {
					for (String file : files) {	    
						final String sourceZip = testDir + file;
						try {
							System.out.println("\nUploading ==> " + file + " to " + uploadFolder);
							Document zipDoc = extractAssetsTester.uploadExtractionZip(uploadFolder, sourceZip, file);
							if (zipDoc != null) {
								if (ADD_JOBS_AFTER_UPLOADS) {
									ExtractJob job = new ExtractJob();
									job.setUploadFolder(uploadFolder);
									job.setZipDoc(zipDoc);	
									
									extractJobs.add(job);
								}
								else {
									extractAssetsTester.createExtractionJob(uploadFolder, zipDoc);
								}								
							}
							else {
								System.out.println("Error. No zip document created."); 
							}
						} catch (Exception e) {
							System.out.println("Error creating extraction ==> " + e.getMessage()); 
							e.printStackTrace(); 
						}	
					}
				}
			}
			
			// Now create all the pending extraction jobs in one go, if any exist.
			for (ExtractJob extractJob : extractJobs) {
				
				Document zipDoc = extractJob.getZipDoc();
				String uploadFolder = extractJob.getUploadFolder();
				
				System.out.println("\nCreating extraction job for " + zipDoc.getName() + " in folder " + uploadFolder);
				extractAssetsTester.createExtractionJob(uploadFolder, zipDoc);
			}
				
		} catch (Exception e) {
			e.printStackTrace(); 
		}		
	}
}