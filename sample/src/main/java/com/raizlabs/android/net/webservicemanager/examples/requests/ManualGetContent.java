package com.raizlabs.android.net.webservicemanager.examples.requests;

import com.raizlabs.android.net.HttpMethod;
import com.raizlabs.android.net.requests.BaseWebServiceRequest;
import com.raizlabs.android.net.requests.RequestBuilder;
import com.raizlabs.android.net.responses.Response;

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
