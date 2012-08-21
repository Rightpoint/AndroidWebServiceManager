package com.raizlabs.net.requests;

import com.raizlabs.net.webservicemanager.WebServiceProgress;

/**
 * Base implementation of a {@link WebServiceProgress}
 * 
 * @author Dylan James
 *
 */
public class BaseWebServiceProgress implements WebServiceProgress {

	long currentProgress;
	long maxProgress;
	
	public BaseWebServiceProgress(long current, long max) {
		this.currentProgress = current;
		this.maxProgress = max;
	}
	
	@Override
	public long getCurrentProgress() {
		return currentProgress;
	}

	@Override
	public long getMaximumProgress() {
		return maxProgress;
	}

}
