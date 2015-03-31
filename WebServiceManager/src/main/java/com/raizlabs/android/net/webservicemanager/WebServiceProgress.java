package com.raizlabs.android.net.webservicemanager;

/**
 * Interface which provides the progress of a web service request.
 * 
 * @author Dylan James
 */
public interface WebServiceProgress {
	/**
	 * @return The current progress value, or -1 if unknown.
	 */
	long getCurrentProgress();
	/**
	 * @return The maximum progress value, or -1 if unknown.
	 */
	long getMaximumProgress();
}
