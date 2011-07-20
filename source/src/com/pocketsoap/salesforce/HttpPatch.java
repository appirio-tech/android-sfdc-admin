package com.pocketsoap.salesforce;

import java.net.URI;

import org.apache.http.client.methods.HttpPost;

/** an Http method for doing Patch requests */
public class HttpPatch extends HttpPost {

	public HttpPatch() {
	}

	public HttpPatch(URI uri) {
		super(uri);
	}

	public HttpPatch(String uri) {
		super(uri);
	}

	@Override
	public String getMethod() {
		return "PATCH";
	}
}
