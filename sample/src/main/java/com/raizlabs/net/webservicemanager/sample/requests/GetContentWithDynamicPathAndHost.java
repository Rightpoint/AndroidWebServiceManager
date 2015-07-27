package com.raizlabs.net.webservicemanager.sample.requests;

import com.raizlabs.net.HttpMethod;
import com.raizlabs.net.requests.BaseWebServiceRequest;
import com.raizlabs.net.requests.RequestBuilder;
import com.raizlabs.net.responses.Response;

public class GetContentWithDynamicPathAndHost extends BaseWebServiceRequest<String>{

	private String host;
	private String path;
	
	public GetContentWithDynamicPathAndHost(String host, String path) {
		this.host = host;
		this.path = path;
	}
	
	@Override
	protected RequestBuilder getRequestBuilder() {
		String url = String.format("%s/Raizlabs/WebServiceManager/master/WebServiceManagerTests/%s", host, path);
		return new RequestBuilder(HttpMethod.Get, url);
	}

	@Override
	protected String translate(Response response) {
		return response.getContentAsString();
	}

}
