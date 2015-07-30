package com.raizlabs.webservicemanager;

import com.raizlabs.webservicemanager.responses.Response;

import org.apache.http.HttpResponse;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * Class which provides some utilities for HTTP.
 * @author Dylan James
 *
 */
public class HttpUtils {
	/**
	 * Returns true if the given {@link HttpURLConnection} has a standard
	 * successful response (200-299)
	 * @param conn The {@link HttpURLConnection} to check.
	 * @return True if the response is between 200 and 299.
	 */
	public static boolean isResponseOK(HttpURLConnection conn) {
		try {
			return conn != null && isResponseOK(conn.getResponseCode());
		} catch (IOException e) {
			return false;
		}
	}
	
	/**
	 * Returns true if the given {@link Response} has a standard
	 * successful response (200-299)
	 * @param response The {@link Response} to check.
	 * @return True if the response is between 200 and 299.
	 */
	public static boolean isResponseOK(Response response) {
		return isResponseOK(response.getResponseCode());
	}
	
	/**
	 * Returns true if the given {@link HttpResponse} has a standard
	 * successful response (200-299).
	 * @param response The {@link HttpResponse} to check.
	 * @return True if the response is between 200 and 299.
	 */
	public static boolean isResponseOK(HttpResponse response) {
		return response != null &&
				response.getStatusLine() != null &&
				isResponseOK(response.getStatusLine().getStatusCode());
	}
	
	/**
	 * Returns true if the given status code is a standard
	 * successful response (200 - 299).
	 * @param statusCode The status code to check.
	 * @return True if the status code is between 200 and 299.
	 */
	public static boolean isResponseOK(int statusCode) {
		return statusCode / 100 == 2;
	}
}
