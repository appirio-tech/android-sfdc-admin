package com.pocketsoap.salesforce;

import java.io.*;

import org.apache.http.entity.ContentProducer;
import org.xmlpull.v1.XmlSerializer;

import android.util.Xml;

/** base class for writing out a salesforce soap request with the session & email headers set */
abstract class SoapProducer implements ContentProducer {

	static final String SOAP_NS = "http://schemas.xmlsoap.org/soap/envelope/";
	static final String PARTNER_NS = "urn:partner.soap.sforce.com";
	
	SoapProducer(String sessionId) {
		this.sessionId = sessionId;
	}
	
	private final String sessionId;
	
	public void writeTo(OutputStream os) throws IOException {
		OutputStreamWriter w = new OutputStreamWriter(os, "UTF-8");
		XmlSerializer x = Xml.newSerializer();
		x.setOutput(w);
		x.startDocument(null, null);
		x.startTag(SOAP_NS, "Envelope");
		writeHeaders(x);
		x.startTag(SOAP_NS,"Body");
		writeBody(x);
		x.endTag(SOAP_NS, "Body");
		x.endTag(SOAP_NS, "Envelope");
		x.endDocument();
	}
	
	protected void writeElem(XmlSerializer x, String ns, String ln, String text) throws IOException {
		x.startTag(ns, ln);
		x.text(text);
		x.endTag(ns, ln);
	}
	
	protected void writeHeaders(XmlSerializer x) throws IOException {
		x.startTag(SOAP_NS, "Header");
		
		x.startTag(PARTNER_NS, "SessionHeader");
		writeElem(x, PARTNER_NS, "sessionId", sessionId);
		x.endTag(PARTNER_NS, "SessionHeader");
		
		x.startTag(PARTNER_NS, "EmailHeader");
		writeElem(x, PARTNER_NS, "triggerUserEmail", "true");
		x.endTag(PARTNER_NS, "EmailHeader");
		
		x.startTag(PARTNER_NS, "MruHeader");
		writeElem(x, PARTNER_NS, "updateMru", "true");
		x.endTag(PARTNER_NS, "MruHeader");
		
		x.endTag(SOAP_NS, "Header");
	}
	
	protected abstract void writeBody(XmlSerializer x) throws IOException;
}