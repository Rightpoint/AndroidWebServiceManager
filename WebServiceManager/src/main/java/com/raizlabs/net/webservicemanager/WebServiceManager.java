package com.raizlabs.net.webservicemanager;

import android.os.Process;
import android.util.Log;

import com.raizlabs.coreutils.concurrent.Prioritized;
import com.raizlabs.coreutils.concurrent.Prioritized.Priority;
import com.raizlabs.net.Constants;
import com.raizlabs.net.HttpClientProvider;
import com.raizlabs.net.HttpMethod;
import com.raizlabs.net.RequestExecutionPool;
import com.raizlabs.net.requests.WebServiceRequest;
import com.raizlabs.net.requests.WebServiceRequest.CancelListener;
import com.raizlabs.net.ssl.SimpleSSLSocketFactory;
import com.raizlabs.net.ssl.TrustManager;
import com.raizlabs.net.ssl.TrustManagerFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

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
	public void setRequestExecutionQueue(RequestExecutionPool queue) {
		this.requestQueue = queue;
		onConnectionTimeoutChanged(getConnectionTimeout());
		onReadTimeoutChanged(getReadTimeout());
	}

	private Semaphore connectionSemaphore;
	private ThreadPoolExecutor backgroundPoolExecutor;
	
	private SimpleSSLSocketFactory sslSocketFactory;
	
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
				connectionSemaphore.acquireUninterruptibly(-deltaConnections);
			}
		}
		
		this.maxConnections = maxConnections;
		if (getRequestExectionQueue() != null && getRequestExectionQueue().getClientProvider() != null) {
			getRequestExectionQueue().getClientProvider().setMaxConnections(maxConnections);
		}
		if (backgroundPoolExecutor != null) {
			backgroundPoolExecutor.setMaximumPoolSize(maxConnections);
			backgroundPoolExecutor.setCorePoolSize(maxConnections);
		}
	}

	
	private int connectionTimeout;
	/**
	 * @return The timeout for establishing a connection (in milliseconds)
	 * @see #setConnectionTimeout(int)
	 */
	public int getConnectionTimeout() { return connectionTimeout; }
	/**
	 * Sets the timeout for establishing a connection. Setting this to zero
	 * means a timeout is not used.
	 * @param timeoutMillis The timeout value in milliseconds
	 */
	public void setConnectionTimeout(int timeoutMillis) { 
		connectionTimeout = timeoutMillis;
		onConnectionTimeoutChanged(timeoutMillis);
	}
	
	/**
	 * Called when the connection timeout may have changed and may need to be 
	 * updated. This may be called more often than when it changes.
	 * @param timeoutMillis The new connection timeout
	 */
	protected void onConnectionTimeoutChanged(int timeoutMillis) {
		RequestExecutionPool requestPool = getRequestExectionQueue();
		if (requestPool != null) {
			HttpClientProvider clientProvider = requestPool.getClientProvider();
			if (clientProvider != null) clientProvider.setConnectionTimeout(timeoutMillis);
		}
	}
	
	
	private int readTimeout;
	/**
	 * @return The timeout for reading data from the connection (in milliseconds)
	 */
	public int getReadTimeout() { return readTimeout; }
	/**
	 * Sets teh timeout for establishing a connection. Setting this to zero
	 * means a timeout is not used.
	 * @param timeoutMillis The timeout value in milliseconds.
	 */
	public void setReadTimeout(int timeoutMillis) { 
		readTimeout = timeoutMillis;
		onReadTimeoutChanged(timeoutMillis);
	}
	
	/**
	 * Called when the read timeout may have changed and may need to be updated.
	 * This may be called more often than when it changes.
	 * @param timeoutMillis The new read timeout
	 */
	protected void onReadTimeoutChanged(int timeoutMillis) {
		RequestExecutionPool requestPool = getRequestExectionQueue();
		if (requestPool != null) {
			HttpClientProvider clientProvider = requestPool.getClientProvider();
			if (clientProvider != null) clientProvider.setReadTimeout(timeoutMillis);
		}
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
		init(maxConnections);
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
		init(DEFAULT_MAX_CONNECTIONS);
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
		init(maxConnections);
		queue.getClientProvider().setMaxConnections(maxConnections);
		this.defaultRequestMode = RequestMode.HttpClient;
	}
	
	private void init(int maxConnections) {
		backgroundPoolExecutor = createBackgroundThreadPool(maxConnections);
		setMaxConnections(maxConnections);
		
		setConnectionTimeout(Constants.Defaults.ConnectionTimeoutMillis);
		setReadTimeout(Constants.Defaults.ReadTimeoutMillis);
	}
	
	/**
	 * Called to get the {@link ThreadPoolExecutor} to use to execute background
	 * requests. 
	 * @param maxConnections The maximum number of connections allowed.
	 * @return The {@link ThreadPoolExecutor} to use to execute background requests.
	 */
	protected ThreadPoolExecutor createBackgroundThreadPool(int maxConnections) {
		final BlockingQueue<Runnable> queue = new PriorityBlockingQueue<Runnable>();
		// Keep 1 thread alive at all times, keep idle threads alive for 3 seconds
		ThreadPoolExecutor executor = new ThreadPoolExecutor(maxConnections, maxConnections, 3, TimeUnit.SECONDS, queue);
		executor.setThreadFactory(new ThreadFactory() {
			@Override
			public Thread newThread(final Runnable r) {
				return new Thread(new Runnable() {
					@Override
					public void run() {
						Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
						r.run();	
					}
				});
			}
		});
		return executor;
	}

	private void beginConnection() {
		connectionSemaphore.acquireUninterruptibly();
	}

	private void endConnection() {
		connectionSemaphore.release();
	}
	
	/**
	 * Sets the {@link TrustManager} to use to verify SSL connections.
	 * @see TrustManagerFactory
	 * @param manager The manager to use to verify SSL connections.
	 */
	public void setTrustManager(TrustManager manager) {
		// Default to the default trust manager if nothing else was given
		if (manager == null) {
			manager = TrustManagerFactory.getDefaultTrustManager(null);
		}
		// Set the SSL Socket Factory to use this manager
		if (sslSocketFactory == null) {
			sslSocketFactory = new SimpleSSLSocketFactory(manager);
		} else {
			sslSocketFactory.setTrustManager(manager);
		}
		// Bind the socket factory
		getRequestExectionQueue().getClientProvider().setHttpsSocketFactory(sslSocketFactory);
		HttpsURLConnection.setDefaultHostnameVerifier(manager);
		HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketFactory.getSSLContext().getSocketFactory());
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
		return doRequest(request, null);
	}

	/**
	 * Performs the given {@link WebServiceRequest} using the given {@link RequestMode}
	 * and returns the result.
	 * @param request The {@link WebServiceRequest} to execute.
	 * @param mode The {@link RequestMode} to use, or null to use the default.
	 * @return The result of the request.
	 */
	public <ResultType> ResultInfo<ResultType> doRequest(WebServiceRequest<ResultType> request, RequestMode mode) {
		if (mode == null) {
			mode = defaultRequestMode;
		}

		switch(mode) {
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
	public <ResultType> ResultInfo<ResultType> doRequestViaClient(final WebServiceRequest<ResultType> request) {
		HttpResponse response = null;
		final HttpUriRequest httpRequest = request.getHttpUriRequest();

		// If the request isn't cancelled, run it
		if (!request.isCancelled()) {
			// Wait for a connection to be available.
			beginConnection();
			
			boolean isCancelled = false;
			// Lock on the status lock so that we know the status won't change
			synchronized (request.getStatusLock()) {
				// Indicate whether the request has been cancelled
				isCancelled = request.isCancelled();
				if (!isCancelled) {
					// If it hasn't been cancelled, we're about to start it, so tell it
					request.onStart();

					// Listen for future cancels
					CancelListener<ResultType> cancelListener = new CancelListener<ResultType>() {
						@Override
						public void onCancel(WebServiceRequest<ResultType> request) {
							// If the request is cancelled, abort the request. This may be called
							// asynchronously
							requestQueue.abortRequest(httpRequest);
							// Remove this listener so we don't get called twice
							request.removeOnCancelListener(this);
						}
					};
					request.addOnCancelListener(cancelListener);
				}
			}
			
			// If the request wasn't cancelled, execute it
			if (!isCancelled) {
				try {
					// Execute the request
					response = requestQueue.doRequest(httpRequest);
				} finally {
					// Free the connection
					endConnection();
				}
			} else {
				// Free the connection
				endConnection();
			}
		}
		
		// Translate the response, or null if we didn't get one
		ResultType result = request.translateHTTPResponse(response, HttpMethod.fromName(httpRequest.getMethod()));
		BasicResultInfo<ResultType> resultInfo = new BasicResultInfo<ResultType>(result, new Date(), response);
		// If the request was cancelled, indicate it in the result info
		if (request.isCancelled()) {
			resultInfo.setCancelled(true);
		}
		// Return the result info
		return resultInfo;
	}

	/**
	 * Performs the given {@link WebServiceRequest} using {@link RequestMode#HttpURLConnection}
	 * and returns the result.
	 * @param request The {@link WebServiceRequest} to execute.
	 * @return The result.
	 */
	public <ResultType> ResultInfo<ResultType> doRequestViaURLConnection(final WebServiceRequest<ResultType> request) {
		HttpURLConnection outerConnection = null;
		ResultInfo<ResultType> resultInfo = null;
		
		boolean needsRetry = false;
		try {
			// If the request hasn't been cancelled yet, start the connection
			if (!request.isCancelled()) {
				// Wait for a connection
				beginConnection();
				
				// Lock on the status lock so that we know the status won't change
				synchronized (request.getStatusLock()) {
					if (!request.isCancelled()) {
						// If it hasn't been cancelled, we're about to start it, so tell it
						request.onStart();
					}
				}
				
				// Get the connection from the request. This should not actually open
				// the connection, merely set it up.
				final HttpURLConnection connection = request.getUrlConnection();
				setupConnection(connection);
				connection.setConnectTimeout(getConnectionTimeout());
				connection.setReadTimeout(getReadTimeout());
				outerConnection = connection;
				// Double check the request is still not cancelled.
				if (!request.isCancelled()) {
					try {
						// Connect
						connection.connect();
					} catch (SocketTimeoutException e) {
						Log.e(WebServiceManager.class.getName(), "Socket timed out. Connection failed.");
					}
					
					
					boolean isCancelled = false;
					// Lock on the status lock so that we know the status won't change
					synchronized(request.getStatusLock()) {
						// Indicate whether the request has been cancelled
						isCancelled = request.isCancelled();
						if (!isCancelled) {		
							// List for future cancels
							CancelListener<ResultType> cancelListener = new CancelListener<ResultType>() {
								@Override
								public void onCancel(WebServiceRequest<ResultType> request) {
									// If cancelled, disconnect the connection. From here on, the
									// connection may be dead.
									connection.disconnect();
									request.removeOnCancelListener(this);
								}
							};
							request.addOnCancelListener(cancelListener);
						}
					}
					
					// If the request wasn't cancelled, execute it
					if (!isCancelled) {
						ResultType result = null;
						try {
							// Try to allow the request to handle the connection
							request.onConnected(connection);
							// Try to translate the connection
							result = request.translateConnection(connection);
						} catch (Exception ex) {
							// We may hit errors if the connection is closed etc.
							Log.w(getClass().getName(), "Error executing request", ex);
						}

						resultInfo = new BasicResultInfo<ResultType>(result, new Date(), connection);
					}
				}
			}
		} catch (IOException e) {
			needsRetry = onURLConnectionException(request, e);
			Log.w(getClass().getName(), "Error in a URLConnection. Retry: " + needsRetry, e);
		} finally {
			// No matter what, disconnect the connection if we have one
			if (outerConnection != null) {
				tearDownConnection(outerConnection);
				outerConnection.disconnect();
			}
			// Release the connection
			endConnection();
		}
		
		if (needsRetry) {
			return doRequestViaURLConnection(request);
		}

		// If we never created a result, create a nulled on
		if (resultInfo == null) {
			resultInfo = new FailedResultInfo<ResultType>(new Date());
		}
		
		// If the request was cancelled, indicate it in the result info
		if (request.isCancelled()) {
			resultInfo.setCancelled(true);
		}
		
		return resultInfo;
	}
	
	/**
	 * Called when an exception is caught in an {@link HttpURLConnection} request.
	 * @param request The {@link WebServiceRequest} that caused the exception.
	 * @param e The raised exception.
	 * @return True to retry the request, false to fail immediately.
	 */
	protected <ResultType> boolean onURLConnectionException(WebServiceRequest<ResultType> request, IOException e) {
		return false;
	}
	
	/**
	 * Called to set up an {@link HttpURLConnection}. This is called right
	 * before the connection is opened so any set up may be done.
	 * @param connection The connection about to be opened
	 */
	protected void setupConnection(HttpURLConnection connection) { }
	/**
	 * Called to tear down an {@link HttpURLConnection}. This is called after
	 * the data has been consumed and the connection is about to be closed.
	 * This can be used to do any final reading of things such as cookies.
	 * @param connection The connection about to be closed
	 */
	protected void tearDownConnection(HttpURLConnection connection) { }
	
	/**
	 * Performs the given {@link WebServiceRequest} on a background thread, with the normal priority,
	 * calling the given {@link WebServiceRequestListener} when completed.
	 * @param request The {@link WebServiceRequest} to execute.
	 * @param listener The {@link WebServiceRequestListener} to call when the request completes. Optional.
	 * predefined values.
	 */
	public <T> void doRequestInBackground(WebServiceRequest<T> request, WebServiceRequestListener<T> listener) {
		doRequestInBackground(request, listener, Priority.NORMAL);
	}
	
	/**
	 * Performs the given {@link WebServiceRequest} on a background thread, with the given priority,
	 * calling the given {@link WebServiceRequestListener} when completed.
	 * @param request The {@link WebServiceRequest} to execute.
	 * @param listener The {@link WebServiceRequestListener} to call when the request completes. Optional.
	 * @param priority The priority to execute the request with. See {@link Priority} for
	 * predefined values.
	 */
	public <T> void doRequestInBackground(
			WebServiceRequest<T> request,
			WebServiceRequestListener<T> listener,
			int priority) {
		
		doRequestInBackground(request, defaultRequestMode, listener, priority);
	}
	
	/**
	 * Performs the given {@link WebServiceRequest} on a background thread, with the given priority,
	 * calling the given {@link WebServiceRequestListener} when completed.
	 * @param request The {@link WebServiceRequest} to execute.
	 * @param mode The {@link RequestMode} to use to execute the request.
	 * @param listener The {@link WebServiceRequestListener} to call when the request completes. Optional.
	 * @param priority The priority to execute the request with. See {@link Priority} for
	 * predefined values.
	 */
	public <T> void doRequestInBackground(
			WebServiceRequest<T> request,
			RequestMode mode,
			WebServiceRequestListener<T> listener,
			int priority) {
		backgroundPoolExecutor.execute(createRunnable(request, mode, listener, priority));
	}
	
	private <T> Runnable createRunnable(
			final WebServiceRequest<T> request,
			final RequestMode mode,
			final WebServiceRequestListener<T> listener,
			int priority) {
		
		return new DownloadRunnable(priority) {
			@Override
			public void run() {
				Process.setThreadPriority(getPriority());
				ResultInfo<T> result = WebServiceManager.this.doRequest(request, mode);
				if (listener != null) listener.onRequestComplete(WebServiceManager.this, result);
			}
		};
	}
	
	private static abstract class DownloadRunnable implements Comparable<DownloadRunnable>, Runnable, Prioritized {
		
		private int priority;
		
		public DownloadRunnable(int priority) {
			this.priority = priority;
		}
		
		@Override
		public int compareTo(DownloadRunnable another) {
			return Prioritized.COMPARATOR_HIGH_FIRST.compare(this, another);
		}

		@Override
		public int getPriority() {
			return priority;
		}
	}
}