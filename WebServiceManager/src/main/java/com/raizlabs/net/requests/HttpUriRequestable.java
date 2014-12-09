package com.raizlabs.net.requests;

import org.apache.http.client.methods.HttpUriRequest;

/**
 * Interface for a class which can provide an {@link HttpUriRequest}.
 * @author Dylan James
 *
 */
public interface HttpUriRequestable {
	/**
	 * @return The {@link HttpUriRequest}.
	 */
	public HttpUriRequest getHttpUriRequest();
}
