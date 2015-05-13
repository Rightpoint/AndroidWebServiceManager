package com.raizlabs.webservicemanager.requests;

import com.raizlabs.webservicemanager.webservicemanager.WebServiceProgress;

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
	
	public void setCurrentProgress(long currentProgress) {
		this.currentProgress = currentProgress;
	}

	@Override
	public long getMaximumProgress() {
		return maxProgress;
	}
	
	public void setMaximumProgress(long maxProgress) {
		this.maxProgress = maxProgress;
	}

}
