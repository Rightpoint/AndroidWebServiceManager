package com.raizlabs.net;

import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

/**
 * A basic implementation of {@link HttpClientProvider} with multiple hooks for
 * extending or modifying built in behavior.
 * @author Dylan James
 *
 */
public class BasicHttpClientProvider implements HttpClientProvider{
	
	private int maxConnections;
	/**
	 * @return The current maximum number of connections.
	 * @see #setMaxConnections(int)
	 */
	public int getMaxConnections() { return maxConnections; }
	public void setMaxConnections(int connections) { this.maxConnections = connections; }

	private ProtocolVersion protocolVersion;
	/**
	 * Gets the {@link ProtocolVersion} which will be used to construct the
	 * connection parameters.
	 * <br><br>
	 * @see #getConnectionParams()
	 * @see #setProtocolVersion(ProtocolVersion)
	 * @return The {@link ProtocolVersion}
	 */
	public ProtocolVersion getProtocolVersion() { return protocolVersion; }
	/**
	 * Sets the {@link ProtocolVersion} which will be used to construct the
	 * connection parameters.
	 * <br><br>
	 * @see #getConnectionParams()
	 * @see #getProtocolVersion()
	 * @param version The {@link ProtocolVersion} to use.
	 */
	public void setProtocolVersion(ProtocolVersion version) { this.protocolVersion = version; }
	
	/**
	 * The {@link HttpClient} we will be reusing.
	 */
	private HttpClient client;
	
	/**
	 * Constructs a {@link BasicHttpClientProvider} with default values.
	 */
	public BasicHttpClientProvider() {
		this(5);
	}
	
	/**
	 * Constructs a {@link BasicHttpClientProvider} with the given number
	 * of maximum connections.
	 * @param maxConnections The maximum number of connections to allow.
	 */
	public BasicHttpClientProvider(int maxConnections) {
		this.maxConnections = maxConnections;
		this.protocolVersion = HttpVersion.HTTP_1_1;
	}
	
	/**
	 * Does some initialization and population of this {@link BasicHttpClientProvider}
	 * which needs to be run before a client is given. This will be called lazily, but
	 * you may call this earlier to prevent start up delays.
	 */
	public void init() {
		// Populate our params, and construct a client from them
		HttpParams connParams = getConnectionParams();
		SchemeRegistry schemeRegistry = getSchemeRegistry();
		ClientConnectionManager connManager = getClientConnectionManager(connParams, schemeRegistry);
		
		client = createClient(connManager, connParams);
	}

	/**
	 * Called to create the {@link HttpClient} we will be using. This may be
	 * overriden by subclasses to use different implementations.
	 * <br><br>
	 * @see #getConnectionParams()
	 * @see #getClientConnectionManager(HttpParams, SchemeRegistry)
	 * @param connManager The {@link ClientConnectionManager} which should be
	 * used to construct the {@link HttpClient}
	 * @param params The {@link HttpParams} which should be provided to the
	 * {@link HttpClient} 
	 * @return The created {@link HttpClient} which will be provided to callers
	 * of {@link #getClient()}
	 */
	protected HttpClient createClient(ClientConnectionManager connManager, HttpParams params) {
		return new DefaultHttpClient(connManager, params);
	}
	
	/**
	 * Called to get the {@link HttpParams} which will be used as the connection
	 * parameters for the created {@link HttpClient}. Subclasses may override
	 * this to provide different params.
	 * <br><br>
	 * @see #createClient(ClientConnectionManager, HttpParams)
	 * @return The {@link HttpParams} which should be sent to the {@link HttpClient}.
	 */
	protected HttpParams getConnectionParams() {
		HttpParams params = new BasicHttpParams();
		ConnManagerParams.setMaxTotalConnections(params, getMaxConnections());
		HttpProtocolParams.setVersion(params, getProtocolVersion());
		return params;
	}
	
	/**
	 * Called to get the {@link ClientConnectionManager} which will be provided
	 * to the {@link HttpClient}. Subclasses may override this to provide a
	 * different manager.
	 * <br><br>
	 * @see #createClient(ClientConnectionManager, HttpParams)
	 * @param params The {@link HttpParams} which should be inserted into the
	 * manager.
	 * @param schemeRegistry The {@link SchemeRegistry} that should be given
	 * to the manager.
	 * @return The {@link ClientConnectionManager} which should be sent to
	 * the {@link HttpClient}.
	 */
	protected ClientConnectionManager getClientConnectionManager(HttpParams params, SchemeRegistry schemeRegistry) {
		return null;
	}
	
	/**
	 * Called to get the {@link SchemeRegistry} which should be provided to
	 * the {@link ClientConnectionManager} for the {@link HttpClient}.
	 * Subclasses may override this to provide a different registry.
	 * <br><br>
	 * @see #getClientConnectionManager(HttpParams, SchemeRegistry)
	 * @see #createClient(ClientConnectionManager, HttpParams)
	 * @return The {@link SchemeRegistry} which should be provided to the
	 * {@link ClientConnectionManager}.
	 */
	protected SchemeRegistry getSchemeRegistry() {
		SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("http", getHttpSocketFactory(), 80));
		schemeRegistry.register(new Scheme("https", getHttpsSocketFactory(), 443));
		return schemeRegistry;
	}
	
	/**
	 * Called to get the {@link SocketFactory} for HTTP calls, which
	 * will be registered with the {@link SchemeRegistry}.
	 * <br><br>
	 * @see #getSchemeRegistry()
	 * @return The {@link SocketFactory} to be registered with the
	 * {@link SchemeRegistry} for HTTP calls.
	 */
	protected SocketFactory getHttpSocketFactory() {
		return PlainSocketFactory.getSocketFactory();
	}
	
	/**
	 * Called to get the {@link SocketFactory} for HTTPS calls, which
	 * will be registered with the {@link SchemeRegistry}.
	 * <br><br>
	 * @see #getSchemeRegistry()
	 * @return The {@link SocketFactory} to be registered with the
	 * {@link SchemeRegistry} for HTTPS calls.
	 */
	protected SocketFactory getHttpsSocketFactory() {
		return PlainSocketFactory.getSocketFactory();
	}
	
	@Override
	public HttpClient getClient() {
		if (client == null) {
			init();
		}
		return client;
	}
}
