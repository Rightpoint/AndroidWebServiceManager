package com.raizlabs.net.webservicemanager;

/**
 * Listener interface for a background web request.
 * @param <T> The type of the result.
 */
public interface WebServiceRequestListener<T> {
	/**
	 * Called when the web request completes.
	 * @param manager The manager which executed the request.
	 * @param result The result of the request.
	 */
	public void onRequestComplete(WebServiceManager manager, ResultInfo<T> result);
}