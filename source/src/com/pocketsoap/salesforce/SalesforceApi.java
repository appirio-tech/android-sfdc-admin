package com.pocketsoap.salesforce;

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.http.*;
import org.apache.http.client.methods.*;
import org.apache.http.entity.*;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.type.TypeReference;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xmlpull.v1.XmlSerializer;

import android.content.Intent;
import android.net.Uri;
import android.util.*;


/**
 * This class exposes all the API calls we want to make to Salesforce.com
 */
public class SalesforceApi extends Http {

	public static final String EXTRA_SERVER = "SVR";
	public static final String EXTRA_SID = "SID";

	public SalesforceApi(Intent i) throws URISyntaxException {
		this(i.getStringExtra(EXTRA_SID), new URI(i.getStringExtra(EXTRA_SERVER)));
	}
	
	public SalesforceApi(String sid, URI instance) {
		this.sessionId = sid;
		this.instance = instance;
		this.restRoot = instance.resolve("/services/data/v21.0/");
		this.soapUri = instance.resolve("/services/Soap/u/21.0");
	}
	
	private final String sessionId;
	private final URI instance;
	private final URI restRoot;
	private final URI soapUri;
	private Boolean hasUserFeed;
	
	public URI getInstanceUri() {
		return instance;
	}
	
	public URI getRestRootUri() {
		return restRoot;
	}
	
	/** @returns the User SObjects primary resource from the REST API */
	public UserResource getUserResource() throws IOException {
		return getJson(restRoot.resolve("sobjects/user"), UserResource.class);
	}
	
	/** @return User details about the recently accessed users, or a default list if there are no recents */
	public List<User> getRecentUsers() throws IOException {
		// there's only Id & Name in the recents list, so we need to get that, and then use the Ids in a query.
		UserResource ur = getUserResource();
		String soql;
		if (ur.recentItems == null || ur.recentItems.size() == 0) {
			soql = buildUserSoqlQuery("limit 10");
		} else {
			StringBuilder ids = new StringBuilder();
			for (SObjectBasic b : ur.recentItems)
				ids.append("'").append(b.Id).append("',");
			ids.deleteCharAt(ids.length()-1);
			soql = buildUserSoqlQuery("where id in (", ids.toString(), ") order by systemmodstamp desc");
		}
		return userSoqlQuery(soql);
	}

	/** @return a list of users that have the searchTerm in their name */
	public List<User> userSearch(String searchTerm, int limit) throws IOException {
		String soql = buildUserSoqlQuery("where name like '%", searchTerm, "%' limit ", String.valueOf(limit));
		return userSoqlQuery(soql);
	}

	/** makes a POST request to create an SObject, we do this via the SOAP API, so that we can set the email header */
	public SaveResult createSObject(final String type, final Map<String, Object> props) throws IOException {
		SoapProducer rp = new SoapProducer(sessionId) {
			@Override
			protected void writeBody(XmlSerializer x) throws IOException {
				x.startTag(PARTNER_NS, "create");
				x.startTag(PARTNER_NS, "sobject");
				writeElem(x, PARTNER_NS, "type", type);
				for (Map.Entry<String, Object> f : props.entrySet()) {
					Object v = f.getValue();
					if (v == null) continue;
					writeElem(x, PARTNER_NS, f.getKey(), v.toString());
				}
				x.endTag(PARTNER_NS, "sobject");
				x.endTag(PARTNER_NS, "create");
			}
		};
		SaveResultHandler handler = new SaveResultHandler();
		postSoapRequest(rp, handler);
		if (handler.hasSeenSoapFault())
			throw new IOException(handler.getFaultString());
		return handler.getResults().get(0);
	}

	/** makes a PATCH request to update an SObject */
	public void patchSObjectJson(String type, String id, final Map<String, Object> props) throws IOException {
		ContentProducer json = new ContentProducer() {
			public void writeTo(OutputStream os) throws IOException {
				mapper.writeValue(os, props);
			}
		};
		URI uri = restRoot.resolve("sobjects/" + type + "/" + id);
		EntityTemplate jsonEntity = new EntityTemplate(json);
		jsonEntity.setContentType("application/json");
		this.patchWithJsonResponse(uri, jsonEntity, getStandardHeaders(), User.class);
	}

	/** performs a pasword reset on the specified userId (using the SOAP API) */
	public void resetPassword(final String userId) throws IOException {
		SoapProducer rp = new SoapProducer(sessionId) {
			@Override
			protected void writeBody(XmlSerializer x) throws IOException {
				x.startTag(PARTNER_NS, "resetPassword");
				writeElem(x, PARTNER_NS, "userId", userId);
				x.endTag(PARTNER_NS, "resetPassword");
			}
		};
		SoapFaultHandler handler = new SoapFaultHandler();
		postSoapRequest(rp, handler);
		if (handler.hasSeenSoapFault())
			throw new IOException(handler.getFaultString());
	}
	
	private <T extends DefaultHandler> void postSoapRequest(SoapProducer req, T responseHandler) throws IOException {
		EntityTemplate resetPwdRequestBody = new EntityTemplate(req);
		resetPwdRequestBody.setContentType("text/xml; charset=UTF-8");
		HttpPost post = new HttpPost(soapUri);
		post.setEntity(resetPwdRequestBody);
		post.addHeader("SOAPAction", "\"\"");
		HttpResponse res = client.execute(post);
		InputStreamReader rdr = new InputStreamReader(res.getEntity().getContent(), "UTF-8");
		try {
			Xml.parse(rdr, responseHandler);
		} catch (SAXException e) {
			throw new IOException(e.getMessage());
		} finally {
			res.getEntity().consumeContent();
		}
	}
	
	/** GETs a URI that returns binary data, like an image, don't use this for big images, fine for thumbnails */
	public byte [] getBinaryData(String uri) throws IOException, URISyntaxException {
		// see http://blog.sforce.com/sforce/2011/03/accessing-chatter-user-pics.html
		URI withSid = new URI(uri + "?oauth_token=" + Uri.encode(sessionId));
		HttpGet get = new HttpGet(withSid);
		HttpResponse res = client.execute(get);
		if (res.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
			throw new IOException("Unexpected status code of " + res.getStatusLine().getStatusCode() + " returned");
		return EntityUtils.toByteArray(res.getEntity());
	}
	
	private String buildUserSoqlQuery(String ... additional) throws IOException {
		StringBuilder q = new StringBuilder("select id,name,firstname,lastname,username,email,title,mobilePhone,phone,isActive");
		if (hasUserFeedObject())
			q.append(",smallPhotoUrl");
		q.append(" from user ");
		for (String a : additional)
			q.append(a);
		return q.toString();
	}
	
	private boolean hasUserFeedObject() throws IOException {
		if (this.hasUserFeed == null) {
			URI uf = restRoot.resolve("sobjects/userfeed");
			hasUserFeed = head(uf, getStandardHeaders()) == HttpStatus.SC_OK;
		}
		return hasUserFeed;
	}
	
	private List<User> userSoqlQuery(String soql) throws IOException {
		URI q = restRoot.resolve("query?q=" + Uri.encode(soql));
		return getJson(q, UserQueryResult.class).records;
	}
	
	private Map<String, String> getStandardHeaders() {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Accept", "application/json");
		headers.put("Authorization", "OAuth " + sessionId);
		return headers;
	}
	
	public <T> T getJson(URI uri, Class<T> responseClz) throws IOException {
		if (!uri.isAbsolute())
			uri = restRoot.resolve(uri);
		return this.getWithJsonResponse(uri, getStandardHeaders(), responseClz);
	}
	
	protected void handleErrorResponse(HttpResponse resp) throws IOException {
		int sc = resp.getStatusLine().getStatusCode();
		// It'd be better to spot this, and refresh the token, and retry the request, but i ran out of time.
		if (sc == HttpStatus.SC_UNAUTHORIZED) throw new MissingSessionException();
		List<Error> errors = mapper.readValue(resp.getEntity().getContent(), new TypeReference<List<Error>>() {});
		throw new IOException(errors.get(0).message);
	}
	
	public static class MissingSessionException extends IOException {
		
		private static final long serialVersionUID = 1L;

		MissingSessionException() {
			super("Session expired");
		}
	}
}
