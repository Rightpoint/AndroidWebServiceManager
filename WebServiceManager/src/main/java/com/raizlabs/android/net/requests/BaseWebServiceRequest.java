package com.raizlabs.android.net.requests;

import java.net.HttpURLConnection;
import java.util.HashSet;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;

import com.raizlabs.android.coreutils.listeners.ProgressListener;
import com.raizlabs.android.net.HttpMethod;
import com.raizlabs.android.net.responses.HttpClientResponse;
import com.raizlabs.android.net.responses.HttpURLConnectionResponse;
import com.raizlabs.android.net.responses.Response;

/**
 * Base implementation that can be used as a starting point for implementing 
 * a {@link WebServiceRequest} and simplifies the implementation down to a
 * couple functions.
 * 
 * @author Dylan James
 *
 * @param <ResultType> The type of object that this request will return.
 */
public abstract class BaseWebServiceRequest<ResultType> implements WebServiceRequest<ResultType> {		
	
	/**
	 * Gets the {@link RequestBuilder} to be used to execute this request.
	 * This should be fully populated and set, as it may be executed
	 * immediately and may never be called again.
	 * @return The fully populated {@link RequestBuilder} to use to
	 * execute this request.
	 */
	protected abstract RequestBuilder getRequestBuilder();
	
	// Store the RequestBuilder so we only query it once.
	private RequestBuilder requestBuilder;
	/**
	 * @return The {@link RequestBuilder} for this request. Only built once.
	 */
	private RequestBuilder getRequest() {
		if (requestBuilder == null) {
			requestBuilder = getRequestBuilder();
		}
		return requestBuilder;
	}
	
	@Override
	public Object getStatusLock() {
		return this;
	}
	
	/**
	 * Flag that the request has started
	 */
	private boolean isStarted = false;
	@Override
	public boolean isStarted() {
		synchronized (getStatusLock()) {
			return isStarted;
		}
	}
	
	@Override
	public void onStart() {
		synchronized (getStatusLock()) {
			isStarted = true;
		}
	}
	
	/**
	 * Cancellation flag
	 */
	private boolean cancelled = false;
	/**
	 * Set of cancellation listeners that are subscribed.
	 */
	private CancelListenerSet<ResultType> cancelListeners = new CancelListenerSet<ResultType>();
	
	@Override
	public void cancel() {
		synchronized(getStatusLock()) {
			cancelled = true;
			// Call any listeners that are subscribed.
			cancelListeners.onCancel(this);
		}
	}
	
	public boolean isCancelled() {
		synchronized(getStatusLock()) {
			return cancelled;
		}
	}
	
	@Override
	public void addOnCancelListener(CancelListener<ResultType> listener) {
		synchronized(getStatusLock()) {
			cancelListeners.add(listener);
			// If we're already cancelled, call it immediately.
			if (cancelled) {
				listener.onCancel(this);
			}
		}
	}
	
	@Override
	public boolean removeOnCancelListener(CancelListener<ResultType> listener) {
		synchronized(getStatusLock()) {
			return cancelListeners.remove(listener);
		}
	}
	
	/**
	 * Set of {@link ProgressListener}s that are subscribed to progress updates.
	 */
	private HashSet<ProgressListener> progressListeners = new HashSet<ProgressListener>();
	
	@Override
	public void addProgressListener(ProgressListener listener) {
		synchronized (progressListeners) {
			progressListeners.add(listener);
		}
	}
	
	@Override
	public boolean removeProgressListener(ProgressListener listener) {
		synchronized (progressListeners) {
			return progressListeners.remove(listener);
		}
	}
	
	/**
	 * Notifies all listeners of the given progress
	 * @param currentProgress The current progress, or -1 if unknown.
	 * @param maxProgress The maximum progress, or -1 if unknown.
	 */
	protected void publishProgress(long currentProgress, long maxProgress) {
		synchronized(progressListeners) {
			for (ProgressListener listener : progressListeners) {
				listener.onProgressUpdate(currentProgress, maxProgress);
			}
		}
	}
	
	@Override
	public HttpURLConnection getUrlConnection() {
		// Get the URL Connection via the RequestBuilder
		return getRequest().getConnection();
	}

	@Override
	public HttpUriRequest getHttpUriRequest() {
		// Get the URI Request via the RequestBuilder
		return getRequest().getRequest();
	}

	/**
	 * Called to translate an {@link Response} to a ResultType object.
	 * @param response The {@link Response} to translate.
	 * @return The ResultType object represented by the {@link Response}.
	 */
	protected abstract ResultType translate(Response response);
	
	public ResultType translateHTTPResponse(HttpResponse response, HttpMethod requestMethod) {
		// Wrap the HttpResponse into a Response implementation
		// and have subclasses translate it
		final Response wrappedResponse = new HttpClientResponse(response, requestMethod);
		ResultType result = translate(wrappedResponse);
		// Close the response to free any resources
		wrappedResponse.close();
		return result;
	}
	public ResultType translateConnection(HttpURLConnection connection) {
		// Wrap the connection into a Response implementation
		// and have subclasses translate it
		final Response wrappedResponse = new HttpURLConnectionResponse(connection); 
		ResultType result = translate(wrappedResponse);
		// Close the response to free any resources
		wrappedResponse.close();
		return result;
	};
	
	@Override
	public void onConnected(HttpURLConnection connection) {
		// OnConnected, call the RequestBuilder
		getRequest().onConnected(connection);
	}

}
