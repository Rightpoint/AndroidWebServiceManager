package com.raizlabs.android.net;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.scheme.SocketFactory;

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
	
	/**
	 * @return The {@link SocketFactory} that will be used to create HTTP Sockets.
	 */
	public SocketFactory getHttpSocketFactory();
	/**
	 * Sets the {@link SocketFactory} to use to create HTTP Sockets.
	 * @param factory The factory to use.
	 */
	public void setHttpSocketFactory(SocketFactory factory);
	/**
	 * @return The {@link SocketFactory} that will be used to create HTTPS Sockets.
	 */
	public SocketFactory getHttpsSocketFactory();
	/**
	 * Sets the {@link SocketFactory} to use to create HTTPS Sockets.
	 * @param factory The factory to use.
	 */
	public void setHttpsSocketFactory(SocketFactory factory);
	
	
	
	/**
	 * @return The timeout for establishing a connection (in milliseconds)
	 * @see #setConnectionTimeout(int)
	 */
	public int getConnectionTimeout();
	/**
	 * Sets the timeout for establishing a connection. Setting this to zero
	 * means a timeout is not used.
	 * @param timeoutMillis The timeout value in milliseconds
	 */
	public void setConnectionTimeout(int timeoutMillis);


	/**
	 * @return The timeout for reading data from the connection (in milliseconds)
	 */
	public int getReadTimeout();
	/**
	 * Sets the timeout for establishing a connection. Setting this to zero
	 * means a timeout is not used.
	 * @param timeoutMillis The timeout value in milliseconds.
	 */
	public void setReadTimeout(int timeoutMillis);
	
}
