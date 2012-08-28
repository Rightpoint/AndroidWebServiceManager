package com.raizlabs.net.requests;

import com.raizlabs.events.ProgressListener;
import com.raizlabs.net.webservicemanager.FailedResultInfo;
import com.raizlabs.net.webservicemanager.RequestMode;
import com.raizlabs.net.webservicemanager.ResultInfo;
import com.raizlabs.net.webservicemanager.WebServiceManager;
import com.raizlabs.net.webservicemanager.WebServiceProgress;
import com.raizlabs.tasks.RZAsyncTask;

/**
 * Implementation of an {@link RZAsyncTask} which performs an
 * {@link WebServiceRequest} using a given {@link WebServiceManager}
 * and returns the result. This class is abstract, and allows sub
 * classes to just implement the request creation. To simply use an
 * existing request, see {@link WebServiceRequestAsyncTask}.
 * <br><br> 
 * Allows for cancellation via {@link #cancelRequest()}, though the
 * request can always be cancelled via {@link WebServiceRequest#cancel()}.
 * <br><br>
 * By default, this will use the default {@link RequestMode} on the
 * {@link WebServiceManager}, though it can be set manually via
 * {@link #setRequestMode(RequestMode)}.
 * 
 * @author Dylan James
 *
 * @param <ResultType> The type of result from the {@link WebServiceRequest}.
 */
public abstract class BaseWebServiceRequestAsyncTask<ResultType> extends RZAsyncTask<Void, WebServiceProgress, ResultInfo<ResultType>> {
	
	private WebServiceManager webServiceManager;
	/**
	 * @return The {@link WebServiceManager} to be used to execute the request.
	 */
	protected WebServiceManager getWebServiceManager() {
		return webServiceManager;
	}
	
	private WebServiceRequest<ResultType> request;
	/**
	 * Method called to create the request. This may be called on the UI thread,
	 * so creation should be quick. Subclasses should call {@link #getRequest()}
	 * to obtain the request, as it will call this method
	 * and store the result.
	 * @return The {@link WebServiceRequest} which should be executed.
	 */
	protected abstract WebServiceRequest<ResultType> createRequest();
	/**
	 * Gets the {@link WebServiceRequest} to be executed. This calls
	 * {@link #createRequest()} and stores the result so it only needs
	 * to be called once.
	 * @return The {@link WebServiceRequest} to be executed.
	 */
	protected WebServiceRequest<ResultType> getRequest() {
		if (request == null) {
			request = createRequest();
			if (request != null) {
				request.addProgressListener(new ProgressListener() {
					final BaseWebServiceProgress progress = new BaseWebServiceProgress(-1, -1);
					@Override
					public void onProgressUpdate(long currentProgress, long maxProgress) {
						progress.currentProgress = currentProgress;
						progress.maxProgress = maxProgress;
						publishProgress(progress);
					}
				});
			}
		}
		return request;
	}

	private RequestMode requestMode;
	protected RequestMode getRequestMode() {
		return requestMode;
	}
	/**
	 * Sets the {@link RequestMode} to use to make the request via the
	 * {@link WebServiceManager}.
	 * @param mode The {@link RequestMode} to use, or null to use the default.
	 */
	public void setRequestMode(RequestMode mode) {
		this.requestMode = mode;
	}
	
	public BaseWebServiceRequestAsyncTask(WebServiceManager manager) {
		this.webServiceManager = manager;
	}

	/**
	 * Cancels this task by cancelling the {@link WebServiceRequest} and then
	 * cancelling this {@link BaseWebServiceRequestAsyncTask}. 
	 */
	public void cancelRequest() {
		if (request == null) {
			request = getRequest();
		}
		if (request != null) {
			request.cancel();
		}
		this.cancel(true);
	}
	
	@Override
	protected boolean doPreExecute() {
		// This will be instant if the request is null
		return getRequest() == null;
	}
	
	@Override
	protected ResultInfo<ResultType> doInBackground(Void... params) {
		// If we have no request, abort.
		if (getRequest() == null) {
			return new FailedResultInfo<ResultType>();
		}
		
		if (isCancelled()) {
			request.cancel();
		}
		
		return webServiceManager.doRequest(request, requestMode);
	}
}
