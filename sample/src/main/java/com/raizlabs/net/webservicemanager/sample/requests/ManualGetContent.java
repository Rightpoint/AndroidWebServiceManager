package com.raizlabs.net.webservicemanager.sample.requests;

import com.raizlabs.net.HttpMethod;
import com.raizlabs.net.requests.BaseWebServiceRequest;
import com.raizlabs.net.requests.RequestBuilder;
import com.raizlabs.net.responses.Response;

public class ManualGetContent extends BaseWebServiceRequest<String>{

	@Override
	protected RequestBuilder getRequestBuilder() {
		return new RequestBuilder(HttpMethod.Get, 
				"http://www.raizlabs.com");
	}

	@Override
	protected String translate(Response response) {
		return response.getContentAsString();
	}

}
