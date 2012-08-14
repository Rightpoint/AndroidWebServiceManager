package com.raizlabs.net.webservicemanager;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Date;
import java.util.concurrent.Semaphore;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;

import com.raizlabs.events.SimpleEventListener;
import com.raizlabs.net.HttpMethod;
import com.raizlabs.net.RequestExecutionPool;
import com.raizlabs.net.requests.WebServiceRequest;

/**
 * Class which executes requests and manages a set of maximum connections.
 * 
 * @author Dylan James
 *
 */
public class WebServiceManager {
	private static final int DEFAULT_MAX_CONNECTIONS = 5;

	private RequestExecutionPool requestQueue;
	/**
	 * @return The {@link RequestExecutionPool} which is used for executing
	 * {@link HttpUriRequest} ({@link RequestMode#HttpClient})
	 */
	public RequestExecutionPool getRequestExectionQueue() { return requestQueue; }
	/**
	 * Sets the {@link RequestExecutionPool} to be used to execute
	 * {@link HttpUriRequest} ({@link RequestMode#HttpClient})
	 * @param queue
	 */
	public void setRequestExecutionQueue(RequestExecutionPool queue) { this.requestQueue = queue; }

	private Semaphore connectionSemaphore;
	
	private int maxConnections;
	/**
	 * @return The current maximum number of allowed connections.
	 */
	public int getMaxConnection() { return maxConnections; }
	/**
	 * Sets the maximum number of concurrent connections. This will return
	 * immediately if increasing, but will block until adequate connections
	 * have finished if decreasing. This method is synchronized, so
	 * this may also block if another thread is waiting on a decrease.
	 * @param maxConnections The new number of maximum concurrent connections.
	 */
	public synchronized void setMaxConnections(int maxConnections) {
		if (connectionSemaphore == null) {
			connectionSemaphore = new Semaphore(maxConnections);
		} else {
			int deltaConnections = maxConnections - this.maxConnections;
			if (deltaConnections > 0) {
				connectionSemaphore.release(deltaConnections);
			} else if (deltaConnections < 0) {
				connectionSemaphore.acquireUninterruptibly(deltaConnections);
			}
		}
		
		this.maxConnections = maxConnections;
	}

	private RequestMode defaultRequestMode;
	/**
	 * Sets the {@link RequestMode} which will be used by default when
	 * no method is specified.
	 * <br><br>
	 * @see #doRequest(WebServiceRequest)
	 * @param mode The default {@link RequestMode}.
	 */
	public void setDefaultRequestMode(RequestMode mode) {
		this.defaultRequestMode = mode;
	}

	/**
	 * Constructs a new {@link WebServiceManager} with default values.
	 */
	public WebServiceManager() {
		this(new RequestExecutionPool(DEFAULT_MAX_CONNECTIONS));
	}

	/**
	 * Constructs a new {@link WebServiceManager} with the given number
	 * of maximum concurrent connections.
	 * @param maxConnections The maximum number of concurrent connections.
	 */
	public WebServiceManager(int maxConnections) {
		this(new RequestExecutionPool(maxConnections));
		setMaxConnections(maxConnections);
	}

	/**
	 * Constructs a new {@link WebServiceManager} which uses the given
	 * {@link RequestExecutionPool} for executing {@link HttpUriRequest}s.
	 * <br><br>
	 * @see #setRequestExecutionQueue(RequestExecutionPool)
	 * @param queue The {@link RequestExecutionPool} for executing
	 * {@link HttpUriRequest}s.
	 */
	public WebServiceManager(RequestExecutionPool queue) {
		setRequestExecutionQueue(queue);
		setMaxConnections(DEFAULT_MAX_CONNECTIONS);
		this.defaultRequestMode = RequestMode.HttpClient;
	}

	/**
	 * Constructs a new {@link WebServiceManager} which only permits the
	 * given number of maximum concurrent connections and uses the given
	 * {@link RequestExecutionPool} for executing {@link HttpUriRequest}s.
	 * @param maxConnections The maximum number of concurrent connections.
	 * @param queue The {@link RequestExecutionPool} for executing
	 * {@link HttpUriRequest}s.
	 */
	public WebServiceManager(int maxConnections, RequestExecutionPool queue) {
		setRequestExecutionQueue(queue);
		setMaxConnections(maxConnections);
		queue.getClientProvider().setMaxConnections(maxConnections);
		this.defaultRequestMode = RequestMode.HttpClient;
	}

	private void beginConnection() {
		connectionSemaphore.acquireUninterruptibly();
	}

	private void endConnection() {
		connectionSemaphore.release();
	}

	/**
	 * Performs the given {@link WebServiceRequest} using the default request mode
	 * and returns the result.
	 * <br><br>
	 * @see #setDefaultRequestMode(RequestMode)
	 * @param request The {@link WebServiceRequest} to execute.
	 * @return The result of the request.
	 */
	public <ResultType> ResultInfo<ResultType> doRequest(WebServiceRequest<ResultType> request) {
		switch(defaultRequestMode) {
		case HttpClient:
			return doRequestViaClient(request);
		case HttpURLConnection:
			return doRequestViaURLConnection(request);
		default:
			return null;
		}
	}

	/**
	 * Performs the given {@link WebServiceRequest} using {@link RequestMode#HttpClient}
	 * and the {@link RequestExecutionPool} and returns the result.
	 * @param request The {@link WebServiceRequest} to execute.
	 * @return The result.
	 */
	public <ResultType> ResultInfo<ResultType> doRequestViaClient(WebServiceRequest<ResultType> request) {
		HttpResponse response = null;
		final HttpUriRequest httpRequest = request.getHttpUriRequest();

		// If the request isn't cancelled, run it
		if (!request.isCancelled()) {
			// Wait for a connection to be available.
			beginConnection();
			// If the request is cancelled, abort the request. This may be called
			// asynchronously
			SimpleEventListener cancelListener = new SimpleEventListener() {
				@Override
				public void onEvent() {
					requestQueue.abortRequest(httpRequest);
				}
			};
			try {
			request.addOnCancelListener(cancelListener);
			// Execute the request
			response = requestQueue.doRequest(httpRequest);
			} finally {
				// Free the connection
				endConnection();
			}
		}
		
		// Translate the response, or null if we didn't get one
		ResultType result = request.translateHTTPResponse(response, HttpMethod.fromName(httpRequest.getMethod()));
		// Return the result
		return new ResultInfo<ResultType>(result, new Date(), response);
	}

	/**
	 * Performs the given {@link WebServiceRequest} using {@link RequestMode#HttpURLConnection}
	 * and returns the result.
	 * @param request The {@link WebServiceRequest} to execute.
	 * @return The result.
	 */
	public <ResultType> ResultInfo<ResultType> doRequestViaURLConnection(WebServiceRequest<ResultType> request) {
		HttpURLConnection outerConnection = null;
		try {
			// If the request hasn't been cancelled yet, start the connection
			if (!request.isCancelled()) {
				// Wait for a connection
				beginConnection();
				// Get the connection from the request. This should not actually open
				// the connection, merely set it up.
				final HttpURLConnection connection = request.getUrlConnection();
				outerConnection = connection;
				// Double check the request is still not cancelled.
				if (!request.isCancelled()) {
					// Connect
					connection.connect();
					
					// If cancelled, disconnect the connection. From here on, the
					// connection may be dead.
					SimpleEventListener cancelListener = new SimpleEventListener() {
						@Override
						public void onEvent() {
							connection.disconnect();
						}
					};
					request.addOnCancelListener(cancelListener);
					
					ResultType result = null;
					try {
						// Try to allow the request to handle the connection
						request.onConnected(connection);
						// Try to translate the connection
						result = request.translateConnection(connection);
					} catch (Exception ex) {
						// We may hit errors if the connection is closed etc.
					}
					
					// Return what we could retrieve
					return new ResultInfo<ResultType>(result, new Date(), connection);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			// No matter what, disconnect the connection if we have one
			if (outerConnection != null) {
				outerConnection.disconnect();
			}
			// Release the connection
			endConnection();
		}

		// Return nulled result
		return new ResultInfo<ResultType>(null, new Date(), (HttpURLConnection)null);
	}
}