package com.raizlabs.webservicemanager.webservicemanager.examples.requests;

import com.raizlabs.webservicemanager.HttpMethod;
import com.raizlabs.webservicemanager.requests.BaseWebServiceRequest;
import com.raizlabs.webservicemanager.requests.RequestBuilder;
import com.raizlabs.webservicemanager.responses.Response;

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
