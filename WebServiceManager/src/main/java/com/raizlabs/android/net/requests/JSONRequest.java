package com.raizlabs.android.net.requests;

import org.json.JSONObject;

import com.raizlabs.android.net.HttpMethod;
import com.raizlabs.android.net.responses.Response;

/**
 * A request which parses the result as an {@link JSONObject}.
 * @author Dylan James
 *
 */
public class JSONRequest extends BaseWebServiceRequest<JSONObject>{

	private RequestBuilder builder;
	
	/**
	 * Constructs a {@link JSONRequest} from the given {@link RequestBuilder}
	 * which will execute the request and return the content of the response 
	 * as a {@link JSONObject}.
	 * @param request The {@link RequestBuilder} to execute.
	 */
	public JSONRequest(RequestBuilder request) {
		this.builder = request;
	}
	
	/**
	 * Constructs a {@link JSONRequest} which will do an HTTP GET at the
	 * given URL and return the content of the response as a {@link JSONObject}
	 * @param url
	 */
	public JSONRequest(String url) {
		this.builder = new RequestBuilder(HttpMethod.Get, url);
	}
	
	@Override
	protected RequestBuilder getRequestBuilder() {
		return builder;
	}

	@Override
	protected JSONObject translate(Response response) {
		return response.getContentAsJSON();
	}

}
