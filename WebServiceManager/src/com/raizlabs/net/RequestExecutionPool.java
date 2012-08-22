package com.raizlabs.net;

import java.io.IOException;
import java.util.HashSet;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

/**
 * A class which maintains a set of executing requests.
 * @author Dylan James
 *
 */
public class RequestExecutionPool {

	private HttpClientProvider clientProvider;
	/**
	 * @return The {@link HttpClientProvider} which is being used to obtain
	 * clients.
	 */
	public HttpClientProvider getClientProvider() { return clientProvider; }
	/**
	 * Sets the {@link HttpClientProvider} to use to obtain clients to
	 * execute requests.
	 * @param clientProvider
	 */
	public void setClientProvider(HttpClientProvider clientProvider) { this.clientProvider = clientProvider; }
	
	/**
	 * The set of the currently executing requests
	 */
	private HashSet<HttpUriRequest> pendingRequests;
	
	/**
	 * Constructs a new {@link RequestExecutionPool} with default parameters.
	 */
	public RequestExecutionPool() {
		this.clientProvider = new BasicHttpClientProvider();
		init();
	}
	
	/**
	 * Constructs a new {@link RequestExecutionPool} with the given number of
	 * maximum connections.
	 * @param maxConnections
	 */
	public RequestExecutionPool(int maxConnections) {
		this.clientProvider = new BasicHttpClientProvider(maxConnections);
		init();
	}
	
	/**
	 * Constructs a new {@link RequestExecutionPool} which uses the given
	 * {@link HttpClientProvider} to obtain it's clients.
	 * @param clientProvider The {@link HttpClientProvider} to obtain
	 * clients from.
	 */
	public RequestExecutionPool(HttpClientProvider clientProvider) {
		this.clientProvider = clientProvider;
		init();
	}
	
	private void init() {
		pendingRequests = new HashSet<HttpUriRequest>();
	}
	
	/**
	 * Executes the given {@link HttpUriRequest} utilizing the current
	 * {@link HttpClientProvider}.
	 * @param request The {@link HttpUriRequest} to execute.
	 * @return The {@link HttpResponse} resulting from the execution, or
	 * null if something goes wrong.
	 * @see #doRequestOrThrow(HttpUriRequest)
	 */
	public HttpResponse doRequest(HttpUriRequest request) {
		addRequest(request);
		HttpContext context = new BasicHttpContext();
		try {
			return getClientProvider().getClient().execute(request, context);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			removeRequest(request);
		}
		return null;
	}
	
	/**
	 * Executes the given {@link HttpUriRequest} utilizing the current
	 * {@link HttpClientProvider}, throwing any encountered exception.
	 * @param request The {@link HttpUriRequest} to execute.
	 * @return The {@link HttpResponse} resulting from the execution.
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public HttpResponse doRequestOrThrow(HttpUriRequest request) throws ClientProtocolException, IOException {
		addRequest(request);
		HttpResponse response = clientProvider.getClient().execute(request);
		removeRequest(request);
		return response;
	}
	
	private void addRequest(HttpUriRequest request) {
		synchronized (pendingRequests) {
			pendingRequests.add(request);
		}
	}
	
	private boolean removeRequest(HttpUriRequest request) {
		synchronized (pendingRequests) {
			return pendingRequests.remove(request);
		}
	}
	
	/**
	 * Aborts the given {@link HttpUriRequest}.
	 * @param request
	 */
	public void abortRequest(final HttpUriRequest request) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				request.abort();
				removeRequest(request);
			}
		}).start();
	}
	
	/**
	 * Aborts all currently executing requests.
	 */
	public void abortAllRequests() {
		synchronized (pendingRequests) {
			for (HttpUriRequest request : pendingRequests) {
				request.abort();
			}
			pendingRequests.clear();
		}
	}
}
