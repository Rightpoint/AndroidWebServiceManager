package com.raizlabs.net;

import org.apache.http.client.HttpClient;

/**
 * Interface which provides {@link HttpClient}s.
 * 
 * @author Dylan James
 *
 */
public interface HttpClientProvider {
	/**
	 * Sets the maximum number of connections that this {@link HttpClientProvider}
	 * will provide.
	 * @param maxConnections The maximum number of connections.
	 */
	public void setMaxConnections(int maxConnections);
	/**
	 * Gets an {@link HttpClient} from this {@link HttpClientProvider}.
	 * @return The obtained {@linkd HttpClient}.
	 */
	public HttpClient getClient();
}
