package com.pocketsoap.salesforce;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Error {
	public String errorCode;
	public String message;
}