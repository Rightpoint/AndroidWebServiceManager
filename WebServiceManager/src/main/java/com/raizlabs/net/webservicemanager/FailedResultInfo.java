package com.raizlabs.net.webservicemanager;

import java.util.Date;

/**
 * {@link ResultInfo} implementation for a request that totally failed and doesn't
 * have any sort of response. Useful for connection failures, invalid parameters, etc.
 * @author Dylan James
 *
 * @param <ResultType>
 */
public class FailedResultInfo<ResultType> implements ResultInfo<ResultType>{

	private Date requestDate;
	private boolean cancelled;
	
	public FailedResultInfo() {
		cancelled = false;
	}
	
	public FailedResultInfo(Date requestDate) {
		this.requestDate = requestDate;
	}
	
	@Override
	public Date getRequestDate() {
		return requestDate;
	}

	@Override
	public ResultType getResult() {
		return null;
	}

	@Override
	public int getResponseCode() {
		return -1;
	}

	@Override
	public String getResponseMessage() {
		return null;
	}

	@Override
	public boolean isStatusOK() {
		return false;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}
	
	@Override
	public boolean wasCancelled() {
		return cancelled;
	}
}
