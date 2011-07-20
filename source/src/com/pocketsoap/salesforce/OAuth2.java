package com.pocketsoap.salesforce;

import java.io.IOException;
import java.net.*;
import java.util.*;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import com.pocketsoap.admin.Login;

/** OAuth2 programatic calls, we only need the call to refresh the access token */
public class OAuth2 extends Http {

	public TokenResponse refreshToken(String token, String authHost) throws IOException, URISyntaxException {
		URI tkn = new URI(authHost).resolve("/services/oauth2/token"); 
		List<NameValuePair> params = new ArrayList<NameValuePair>(5);
		params.add(new BasicNameValuePair("grant_type", "refresh_token"));
		params.add(new BasicNameValuePair("refresh_token", token));
		params.add(new BasicNameValuePair("format", "json"));
		params.add(new BasicNameValuePair("client_id", Login.CLIENT_ID));
		return postWithJsonResponse(tkn, params, null, TokenResponse.class);
	}
	
    @JsonIgnoreProperties(ignoreUnknown=true)
	public static class TokenResponse {
    	public String error;
    	public String error_description;
        public String refresh_token;
        public String access_token;
        public String instance_url;
        public String id;
	}
}
