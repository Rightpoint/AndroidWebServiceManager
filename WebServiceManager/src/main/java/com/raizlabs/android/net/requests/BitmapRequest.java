package com.raizlabs.android.net.requests;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Rect;

import com.raizlabs.android.net.HttpMethod;
import com.raizlabs.android.net.responses.Response;

/**
 * A request which obtains a {@link Bitmap} from a request. Keep in mind that
 * it is easy to download a {@link Bitmap} which exceeds the heap size and
 * will crash, so populate the options accordingly. This should only be used
 * when the size is known ahead of time, otherwise use a different
 * {@link WebServiceRequest} which will allow getting the bounds first.
 * 
 * @author Dylan James
 *
 */
public class BitmapRequest extends BaseWebServiceRequest<Bitmap>{

	private RequestBuilder builder;
	private Rect outPadding;
	private Options options;
	
	/**
	 * Creates a {@link BitmapRequest} which executes an HTTP GET at the given
	 * URL and returns the response content decoded as a {@link Bitmap}.
	 * @param url The URL to request.
	 * @param outPadding @see {@link BitmapFactory#decodeStream(java.io.InputStream, Rect, Options)}
	 * @param options The decoding {@link Options}.
	 */
	public BitmapRequest(String url, Rect outPadding, Options options) {
		this(new RequestBuilder(HttpMethod.Get, url), outPadding, options);
	}
	
	/**
	 * Creates a {@link BitmapRequest} which executes the given
	 * {@link RequestBuilder} and returns the response content
	 * decoded as a {@link Bitmap}.
	 * @param request The {@link RequestBuilder} to execute.
	 * @param outPadding @see {@link BitmapFactory#decodeStream(java.io.InputStream, Rect, Options)}
	 * @param options The decoding {@link Options}.
	 */
	public BitmapRequest(RequestBuilder request, Rect outPadding, Options options) {
		this.builder = request;
		this.outPadding = outPadding;
		this.options = options;
	}
	
	@Override
	protected RequestBuilder getRequestBuilder() {
		return builder;
	}

	@Override
	protected Bitmap translate(Response response) {
		return response.getContentAsBitmap(outPadding, options);
	}

}
