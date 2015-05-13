package com.raizlabs.webservicemanager.webservicemanager.examples.requests;

import android.graphics.Bitmap;

import com.raizlabs.webservicemanager.HttpMethod;
import com.raizlabs.webservicemanager.requests.BaseWebServiceRequest;
import com.raizlabs.webservicemanager.requests.RequestBuilder;
import com.raizlabs.webservicemanager.responses.Response;

public class ManualGetLogo extends BaseWebServiceRequest<Bitmap>{

	@Override
	protected RequestBuilder getRequestBuilder() {
		return new RequestBuilder(HttpMethod.Get,
				"http://www.raizlabs.com/cms/wp-content/uploads/2011/06/raizlabs-logo-sheetrock.png");
	}

	@Override
	protected Bitmap translate(Response response) {
		return response.getContentAsBitmap(null, null);
	}

}
