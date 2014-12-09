package com.raizlabs.net.requests;

import java.net.HttpURLConnection;

/**
 * Interface for a class which can provide an {@link HttpURLConnection}.
 * @author Dylan James
 *
 */
public interface UrlConnectionRequestable {
	/**
	 * @return The {@link HttpURLConnection}.
	 */
	public HttpURLConnection getUrlConnection();
}
