package net.steveclewer.cmis.examples;

import org.apache.chemistry.opencmis.client.api.Document;

public class ExtractJob {

	Document zipDoc;
	
	String uploadFolder;
	
	public String getUploadFolder() {
		return uploadFolder;
	}

	public void setUploadFolder(String uploadFolder) {
		this.uploadFolder = uploadFolder;
	}

	public Document getZipDoc() {
		return zipDoc;
	}

	public void setZipDoc(Document zipDoc) {
		this.zipDoc = zipDoc;
	}

}
