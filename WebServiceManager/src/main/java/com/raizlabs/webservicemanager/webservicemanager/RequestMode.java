package com.raizlabs.webservicemanager.webservicemanager;

/**
 * Enum which represents the valid request modes for the {@link WebServiceManager}.
 * 
 * @author Dylan James
 *
 */
public enum RequestMode {
	/**
	 * Indicates that requests are handled via {@link org.apache.http.client.HttpClient}s.
	 */
	HttpClient,
	/**
	 * Indicates that requests are handled via {@link java.net.HttpURLConnection}s.
	 */
	HttpURLConnection
}
