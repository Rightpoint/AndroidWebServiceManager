package com.raizlabs.webservicemanager.responses;

import com.raizlabs.webservicemanager.HttpMethod;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import java.io.IOException;
import java.io.InputStream;

/**
 * {@link Response} implementation which wraps an {@link HttpResponse} which
 * is usually obtained via an {@link HttpClient}.
 * @author Dylan James
 *
 */
public class HttpClientResponse extends BaseResponse {
	private HttpResponse response;
	private HttpMethod requestMethod;
	
	/**
	 * Creates an {@link HttpClientResponse} from the given response and method.
	 * @param response The actual {@link HttpResponse}.
	 * @param requestMethod The {@link HttpMethod} which was used to obtain the
	 * response.
	 */
	public HttpClientResponse(HttpResponse response, HttpMethod requestMethod) {
		this.response = response;
		this.requestMethod = requestMethod;
	}

	@Override
	public boolean containsHeader(String name) {
		return response != null && response.containsHeader(name);
	}

	@Override
	public String getHeaderValue(String name) {
		if (response != null) {
			Header header = response.getFirstHeader(name);
			if (header != null) {
				return header.getValue();
			}
		}
		
		return null;
	}

	@Override
	public int getResponseCode() {
		if (response != null && response.getStatusLine() != null) {
			return response.getStatusLine().getStatusCode();
		}
		return -1;
	}

	@Override
	public String getResponseMessage() {
		if (response != null && response.getStatusLine() != null) {
			return response.getStatusLine().getReasonPhrase();
		}
		return null;
	}
	
	@Override
	public String getContentEncoding() {
		if (response != null && response.getEntity() != null &&
				response.getEntity().getContentEncoding() != null) {
			return response.getEntity().getContentEncoding().getValue();
		}
		return null;
	}

	@Override
	public long getContentLength() {
		if (response != null && response.getEntity() != null) {
			return response.getEntity().getContentLength();
		}
		return -1;
	}

	@Override
	public String getContentType() {
		if (response != null && response.getEntity() != null &&
				response.getEntity().getContentType() != null) {
			return response.getEntity().getContentType().getValue();
		}
		return null;
	}
	
	@Override
	public InputStream getContentStream() throws IOException {
		try {
			return response.getEntity().getContent();
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public HttpMethod getRequestMethod() {
		return requestMethod;
	}

	@Override
	public void close() {
		// Try to consume the content handled by the given entity
		// Not doing this may leave the connection open and can be dangerous
		// if reusing the client.
		// See: https://groups.google.com/forum/?fromgroups=#!topic/android-developers/uL8ah41voW4
		if (response != null) {
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				try {
					entity.consumeContent();
				} catch (IOException e) { }
			}
		}
	}


}
