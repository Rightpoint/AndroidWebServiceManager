package com.raizlabs.android.net.requests;

import com.raizlabs.android.net.HttpMethod;
import com.raizlabs.android.net.responses.Response;

/**
 * A request which return the content of a response as a String.
 * @author Dylan James
 *
 */
public class StringRequest extends BaseWebServiceRequest<String>{

	private RequestBuilder builder;
	
	/**
	 * Creates a {@link StringRequest} which will execute the given
	 * {@link RequestBuilder} and return the content of the response as a 
	 * String.
	 * @param request The {@link RequestBuilder} to execute.
	 */
	public StringRequest(RequestBuilder request) {
		this.builder = request;
	}
	
	/**
	 * Creates a {@link StringRequest} which will perform an HTTP GET at the
	 * given URL and return the content of the response as a String.
	 * @param url The URL to perform the request to.
	 */
	public StringRequest(String url) {
		this.builder = new RequestBuilder(HttpMethod.Get, url);
	}
	
	@Override
	protected RequestBuilder getRequestBuilder() {
		return builder;
	}

	@Override
	protected String translate(Response response) {
		return response.getContentAsString();
	}

}
