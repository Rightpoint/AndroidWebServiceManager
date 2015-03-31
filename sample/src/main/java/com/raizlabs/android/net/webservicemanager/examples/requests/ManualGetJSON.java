package com.raizlabs.android.net.webservicemanager.examples.requests;

import org.json.JSONObject;

import com.raizlabs.android.net.HttpMethod;
import com.raizlabs.android.net.requests.BaseWebServiceRequest;
import com.raizlabs.android.net.requests.RequestBuilder;
import com.raizlabs.android.net.responses.Response;

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
