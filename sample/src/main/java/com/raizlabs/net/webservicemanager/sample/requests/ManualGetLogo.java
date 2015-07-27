package com.raizlabs.net.webservicemanager.sample.requests;

import android.graphics.Bitmap;

import com.raizlabs.net.HttpMethod;
import com.raizlabs.net.requests.BaseWebServiceRequest;
import com.raizlabs.net.requests.RequestBuilder;
import com.raizlabs.net.responses.Response;

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
