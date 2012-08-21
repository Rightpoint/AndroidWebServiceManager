package com.raizlabs.net.requests;

import com.raizlabs.net.webservicemanager.WebServiceManager;

/**
 * Implementation of {@link BaseWebServiceRequestAsyncTask} which allows a
 * {@link WebServiceRequest} to be provided explicitly. Subclasses will be
 * required to build the request immediately upon construction. See
 * {@link BaseWebServiceRequestAsyncTask} for easier extension.
 * 
 * @author Dylan James
 *
 * @param <ResultType>
 */
public class WebServiceRequestAsyncTask<ResultType> extends BaseWebServiceRequestAsyncTask<ResultType>{

	private WebServiceRequest<ResultType> request;
	
	/**
	 * Creates a {@link BaseWebServiceRequestAsyncTask} which will execute the
	 * given {@link WebServiceRequest} via the given {@link WebServiceManager}.
	 * @param request The {@link WebServiceRequest} to perform.
	 * @param manager The {@link WebServiceManager} to use.
	 */
	public WebServiceRequestAsyncTask(WebServiceRequest<ResultType> request, WebServiceManager manager) {
		super(manager);
		this.request = request;
	}

	@Override
	protected WebServiceRequest<ResultType> createRequest() {
		return request;
	}

}
