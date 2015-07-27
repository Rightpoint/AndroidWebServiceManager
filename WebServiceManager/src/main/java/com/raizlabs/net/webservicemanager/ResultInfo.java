package com.raizlabs.net.webservicemanager;

import java.util.Date;

/**
 * Interface which provides information about the result of a web service request.
 * @author Dylan James
 *
 * @param <ResultType> The type of the result which will be returned.
 */
public interface ResultInfo<ResultType> {
	/**
	 * @return The date when the result was requested.
	 */
	Date getRequestDate();
	
	/**
	 * @return The result of the request, or null if it failed.
	 */
	ResultType getResult();
	
	/**
	 * @return The response code from the request, or -1 if it failed.
	 */
	int getResponseCode();
	
	/**
	 * @return The response message from the request, or null if it failed.
	 */
	String getResponseMessage();
	
	/**
	 * @return True if the response code of this request is a standard OK response.
	 */
	boolean isStatusOK();
	
	/**
	 * Sets whether the request was cancelled.
	 * @param cancelled
	 */
	void setCancelled(boolean cancelled);
	/**
	 * @return True if the result was cancelled.
	 */
	boolean wasCancelled();
}
