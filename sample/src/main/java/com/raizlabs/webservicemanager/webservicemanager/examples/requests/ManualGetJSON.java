package com.raizlabs.webservicemanager.webservicemanager.examples.requests;

import org.json.JSONObject;

import com.raizlabs.webservicemanager.HttpMethod;
import com.raizlabs.webservicemanager.requests.BaseWebServiceRequest;
import com.raizlabs.webservicemanager.requests.RequestBuilder;
import com.raizlabs.webservicemanager.responses.Response;

public class ManualGetJSON extends BaseWebServiceRequest<JSONObject>{

	@Override
	protected RequestBuilder getRequestBuilder() {
		return new RequestBuilder(HttpMethod.Get,
				"https://raw.github.com/Raizlabs/WebServiceManager/master/WebServiceManagerTests/TestData.json");
	}

	@Override
	protected JSONObject translate(Response response) {
		return response.getContentAsJSON();
	}

}
