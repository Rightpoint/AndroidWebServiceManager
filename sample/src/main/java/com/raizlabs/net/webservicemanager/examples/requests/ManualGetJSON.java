package com.raizlabs.net.webservicemanager.examples.requests;

import org.json.JSONObject;

import com.raizlabs.net.HttpMethod;
import com.raizlabs.net.requests.BaseWebServiceRequest;
import com.raizlabs.net.requests.RequestBuilder;
import com.raizlabs.net.responses.Response;

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
