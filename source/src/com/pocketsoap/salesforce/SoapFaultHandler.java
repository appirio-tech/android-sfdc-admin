package com.pocketsoap.salesforce;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/** parses a soap response looking to see if its a soap fault */
class SoapFaultHandler extends DefaultHandler {
	
	protected final StringBuilder chars = new StringBuilder();

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		chars.setLength(0);
		if (!seenSoapFault && localName.equals("Fault") && uri.equals(SoapProducer.SOAP_NS))
			seenSoapFault = true;
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		chars.append(ch, start, length);
	}

	protected boolean seenSoapFault;
	protected String faultCode, faultString;
	
	boolean hasSeenSoapFault() {
		return seenSoapFault;
	}
	
	String getFaultCode() {
		return faultCode;
	}
	
	String getFaultString() {
		return faultString;
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (seenSoapFault) {
			if (localName.equals("faultcode"))
				faultCode = chars.toString();
			else if (localName.equals("faultstring"))
				faultString = chars.toString();
		}
	}
}