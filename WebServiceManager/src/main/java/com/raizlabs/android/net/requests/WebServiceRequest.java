package com.raizlabs.android.net.requests;

import java.net.HttpURLConnection;

import org.apache.http.HttpResponse;

import com.raizlabs.android.coreutils.collections.MappableSet;
import com.raizlabs.android.coreutils.functions.Delegate;
import com.raizlabs.android.coreutils.listeners.ProgressListener;
import com.raizlabs.android.net.HttpMethod;

/**
 * An interface for a generic Web Service Request which can be requested in
 * multiple ways and then returns a result from the response data.
 * <br><br>
 * Also supports storing a cancelled state and alerting listeners when it is
 * cancelled.
 * <br><br>
 * @see BaseWebServiceRequest for easier implementation.
 * @author Dylan James
 *
 * @param <ResultType> The type that this will return after the request is complete.
 */
public interface WebServiceRequest<ResultType> extends HttpUriRequestable, UrlConnectionRequestable {
	public interface CancelListener<ResultType> {
		public void onCancel(WebServiceRequest<ResultType> request);
	}
	
	static class CancelListenerSet<ResultType> extends MappableSet<CancelListener<ResultType>> {
		public void onCancel(final WebServiceRequest<ResultType> request) {
			map(new Delegate<CancelListener<ResultType>>() {
				@Override
				public void execute(CancelListener<ResultType> listener) {
					listener.onCancel(request);
				}
			});
		}
	}
	
	/**
	 * Called when the {@link HttpURLConnection} is connected, allowing
	 * data to be written to the output stream etc.
	 * @param connection The {@link HttpURLConnection} which has been
	 * connected.
	 */
	void onConnected(HttpURLConnection connection);
	/**
	 * Translates the established {@link HttpURLConnection} into 
	 * a ResultType object.
	 * @param connection The {@link HttpURLConnection} to get
	 * data from.
	 * @return A ResultType object representing this response.
	 */
	ResultType translateConnection(HttpURLConnection connection);
	
	/**
	 * Translates the given {@link HttpResponse} into a ResultType
	 * object.
	 * @param response The {@link HttpResponse} which was the result
	 * of this request.
	 * @param requestMethod The {@link HttpMethod} which was used to
	 * request this request.
	 * @return A ResultType object representing this response.
	 */
	ResultType translateHTTPResponse(HttpResponse response, HttpMethod requestMethod);
	
	/**
	 * Gets the object which is used as the lock for the status of this request.
	 * Changes to the status of this request cannot be made without holding this
	 * lock, including starting and canceling the request. Holding this lock
	 * guarantees that the status will not change.
	 * @return The lock to use for status changes.
	 */
	Object getStatusLock();
	
	/**
	 * Called to indicate that this request is being started.
	 */
	void onStart();
	
	/**
	 * @return True if this request has started.
	 */
	boolean isStarted();
	
	/**
	 * Marks this {@link WebServiceRequest} as cancelled and alerts any listeners.
	 * <br><br>
	 * @see #isCancelled()
	 * @see #addOnCancelListener(SimpleEventListener)
	 */
	void cancel();
	/**
	 * @return True if this {@link WebServiceRequest} has been cancelled.
	 * <br><br>
	 * @see #cancel()
	 */
	boolean isCancelled();
	/**
	 * Adds a listener which will be called if this {@link WebServiceRequest} is
	 * cancelled. This listener is called immediately if this request has
	 * already been cancelled.
	 * <br><br>
	 * @see #cancel()
	 * @see #removeOnCancelListener(CancelListener)
	 * @param listener The {@link CancelListener} to call on cancellation.
	 */
	void addOnCancelListener(CancelListener<ResultType> listener);
	/**
	 * Removes the given {@link Delegate} from being notified of
	 * cancellations.
	 * @param listener The {@link CancelListener} to remove.
	 * @return True if the listener was removed, false if it was not found.
	 */
	boolean removeOnCancelListener(CancelListener<ResultType> listener);
	
	/**
	 * Adds a listener which will be called when the progress is updated.
	 * <br>NOTE: Some requests may not publish progress.
	 * @param listener The {@link ProgressListener} which will be called
	 * on progress updates.
	 */
	void addProgressListener(ProgressListener listener);
	/**
	 * Removes the given {@link ProgressListener} from being notified
	 * of progress updates.
	 * @param listener The {@link ProgressListener} to remove.
	 * @return True if the listener was removed, false if it was not found.
	 */
	boolean removeProgressListener(ProgressListener listener);
}
