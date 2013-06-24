package com.raizlabs.net.requests;

import org.json.JSONArray;

import com.raizlabs.net.HttpMethod;
import com.raizlabs.net.responses.Response;

/**
 * A request which parses the result as an {@link JSONArray}.
 * @author Dylan James
 *
 */
public class JSONArrayRequest extends BaseWebServiceRequest<JSONArray>{

	private RequestBuilder builder;
	
	/**
	 * Constructs a {@link JSONArrayRequest} from the given {@link RequestBuilder}
	 * which will execute the request and return the content of the response 
	 * as a {@link JSONArray}.
	 * @param request The {@link RequestBuilder} to execute.
	 */
	public JSONArrayRequest(RequestBuilder request) {
		this.builder = request;
	}
	
	/**
	 * Constructs a {@link JSONArrayRequest} which will do an HTTP GET at the
	 * given URL and return the content of the response as a {@link JSONArray}
	 * @param url
	 */
	public JSONArrayRequest(String url) {
		this.builder = new RequestBuilder(HttpMethod.Get, url);
	}
	
	@Override
	protected RequestBuilder getRequestBuilder() {
		return builder;
	}

	@Override
	protected JSONArray translate(Response response) {
		return response.getContentAsJSONArray();
	}

}
