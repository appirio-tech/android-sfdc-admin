package com.pocketsoap.salesforce;

import java.util.*;

import org.xml.sax.*;

/** parses one or more SaveResults from a soap response */
public class SaveResultHandler extends SoapFaultHandler {

	private List<SaveResult> results = new ArrayList<SaveResult>();
	private SaveResult sr;

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		super.startElement(uri, localName, qName, attributes);
		if (localName.equals("result"))
			sr = new SaveResult();
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		super.endElement(uri, localName, qName);
		if (sr == null) return;
		if (localName.equals("id"))
			sr.id = chars.toString();
		else if (localName.equals("success")) {
			String v = chars.toString();
			sr.success = v.equals("true") || v.equals("1");
		} else if (localName.equals("message") || localName.equals("statusCode")) {
			Error e;
			if (sr.errors == null) {
				e = new Error();
				sr.errors = new ArrayList<Error>(1);
				sr.errors.add(e);
			} else {
				e = sr.errors.get(0);
			}
			if (localName.equals("message"))
				e.message = chars.toString();
			else
				e.errorCode = chars.toString();
		} else if (localName.equals("result")) {
			results.add(sr);
			sr = null;
		}
	}

	public List<SaveResult> getResults() {
		return results;
	}	
}
