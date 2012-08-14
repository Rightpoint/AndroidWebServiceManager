package com.raizlabs.net.webservicemanager;

import org.apache.http.HttpResponse;

/**
 * Interface for a class which translates {@link HttpResponse}s into a result.
 * @author Dylan James
 *
 * @param <RequestType> The type of the Request that this translator handles
 * @param <ResultType> The type of object this translator returns
 */
public interface HttpRequestTranslator<RequestType, ResultType> {
	/**
	 * Translates the given {@link HttpResponse} into a result.
	 * @param request The Request which resulted in the response.
	 * @param response The {@link HttpResponse}
	 * @return The result.
	 */
	ResultType translateHTTPResponse(RequestType request, HttpResponse response);
}
