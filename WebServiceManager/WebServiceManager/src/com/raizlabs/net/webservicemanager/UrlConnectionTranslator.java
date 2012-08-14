package com.raizlabs.net.webservicemanager;

import java.net.HttpURLConnection;
/**
 * Interface for a class which translates {@link HttpURLConnection}s into a result.
 * @author Dylan James
 *
 * @param <RequestType> The type of the Request that this translator handles
 * @param <ResultType> The type of object this translator returns
 */
public interface UrlConnectionTranslator<RequestType, ReturnType> {
	/**
	 * Translates the given {@link HttpURLConnection} into a result.
	 * @param request The Request which resulted in the response.
	 * @param connection The {@link HttpURLConnection} resulting from the
	 * request.
	 * @return The result.
	 */
	ReturnType translateConnection(RequestType request, HttpURLConnection connection);
}
