package com.raizlabs.android.net.webservicemanager.examples.requests;

import android.graphics.Bitmap;

import com.raizlabs.android.net.HttpMethod;
import com.raizlabs.android.net.requests.BaseWebServiceRequest;
import com.raizlabs.android.net.requests.RequestBuilder;
import com.raizlabs.android.net.responses.Response;

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
