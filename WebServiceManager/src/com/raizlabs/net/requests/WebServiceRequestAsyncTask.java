package com.raizlabs.net.requests;

import com.raizlabs.events.ProgressListener;
import com.raizlabs.net.webservicemanager.RequestMode;
import com.raizlabs.net.webservicemanager.ResultInfo;
import com.raizlabs.net.webservicemanager.WebServiceManager;
import com.raizlabs.net.webservicemanager.WebServiceProgress;
import com.raizlabs.tasks.RZAsyncTask;

/**
 * Implementation of an {@link RZAsyncTask} which performs an
 * {@link WebServiceRequest} using a given {@link WebServiceManager}
 * and returns the result. Also allows for cancellation via
 * {@link #cancelRequest()}, though the request can always be cancelled via 
 * {@link WebServiceRequest#cancel()}.
 * <br><br>
 * By default, this will use the default {@link RequestMode} on the
 * {@link WebServiceManager}, though it can be set manually via
 * {@link #setRequestMode(RequestMode)}.
 * 
 * @author Dylan James
 *
 * @param <ResultType> The type of result from the {@link WebServiceRequest}.
 */
public class WebServiceRequestAsyncTask<ResultType> extends RZAsyncTask<Void, WebServiceProgress, ResultInfo<ResultType>> {
	
	private WebServiceRequest<ResultType> request;
	private WebServiceManager webServiceManager;

	private RequestMode requestMode;
	/**
	 * Sets the {@link RequestMode} to use to make the request via the
	 * {@link WebServiceManager}.
	 * @param mode The {@link RequestMode} to use, or null to use the default.
	 */
	public void setRequestMode(RequestMode mode) {
		this.requestMode = mode;
	}
	
	/**
	 * Creates a {@link WebServiceRequestAsyncTask} which will execute the
	 * given {@link WebServiceRequest} via the given {@link WebServiceManager}.
	 * @param request The {@link WebServiceRequest} to perform.
	 * @param manager The {@link WebServiceManager} to use.
	 */
	public WebServiceRequestAsyncTask(WebServiceRequest<ResultType> request, WebServiceManager manager) {
		this.request = request;
		this.webServiceManager = manager;
	}

	/**
	 * Cancels this task by cancelling the {@link WebServiceRequest} and then
	 * cancelling this {@link WebServiceRequestAsyncTask}. 
	 */
	public void cancelRequest() {
		request.cancel();
		this.cancel(true);
	}
	
	@Override
	protected ResultInfo<ResultType> doInBackground(Void... params) {
		final BaseWebServiceProgress progress = new BaseWebServiceProgress(-1, -1);
		request.addProgressListener(new ProgressListener() {
			@Override
			public void onProgressUpdate(long currentProgress, long maxProgress) {
				progress.currentProgress = currentProgress;
				progress.maxProgress = maxProgress;
				publishProgress(progress);
			}
		});
		
		if (isCancelled()) {
			request.cancel();
		}
		
		return webServiceManager.doRequest(request, requestMode);
	}
}
