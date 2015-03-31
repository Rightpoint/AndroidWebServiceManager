package com.raizlabs.android.net.webservicemanager;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Date;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;

import com.raizlabs.android.net.HttpUtils;

import android.util.Log;

/**
 * Class which implements {@link ResultInfo} with basic functionality to
 * allow easy wrapping of {@link HttpURLConnection}s and
 * {@link HttpResponse}s.
 * @author Dylan James
 *
 * @param <ResultType> The type of the result which will be returned
 */
public class BasicResultInfo<ResultType> implements ResultInfo<ResultType>{
	Date RequestDate;
	public Date getRequestDate() { return RequestDate; }
	ResultType Result;
	public ResultType getResult() { return Result; }
	
	int ResponseCode = -1;
	public int getResponseCode() { return ResponseCode; }
	String ResponseMessage;
	public String getResponseMessage() { return ResponseMessage; }
	
	public boolean isStatusOK() { return HttpUtils.isResponseOK(ResponseCode); }
	
	boolean cancelled;
	/**
	 * @return True if the request was cancelled.
	 */
	public boolean wasCancelled() { return cancelled; }
	public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
	
	/**
	 * Creates a {@link BasicResultInfo} by wrapping the given result and
	 * {@link HttpURLConnection}.
	 * @param result The result of the request.
	 * @param requestDate The {@link Date} the request was completed.
	 * @param connection The {@link HttpURLConnection} resulting from the request.
	 * @throws IOException If there was an exception with the connection.
	 */
	public BasicResultInfo(ResultType result, Date requestDate, HttpURLConnection connection) throws IOException {
		this(result, requestDate);
		wrap(connection);
	}
	
	/**
	 * Creates a {@link BasicResultInfo} by wrapping the given result and
	 * {@link HttpResponse}.
	 * @param result The result of the request.
	 * @param requestDate The {@link Date} the request was completed.
	 * @param response The {@link HttpResponse} resulting from the request.
	 */
	public BasicResultInfo(ResultType result, Date requestDate, HttpResponse response) {
		this(result, requestDate);
		wrap(response);
	}
	
	
	private BasicResultInfo(ResultType result, Date requestDate) {
		this.Result = result;
		this.RequestDate = requestDate;
		this.cancelled = false;
	}
	
	
	private void wrap(HttpURLConnection conn) throws IOException {
		if (conn != null) {
			try {
				this.ResponseCode = conn.getResponseCode();
				this.ResponseMessage = conn.getResponseMessage();
			} catch (IOException e) {
				if (BuildConfig.DEBUG) {
					Log.w(getClass().getName(), "IO Exception when wrapping URLConnection: " + e.getMessage());
				}
				throw e;
			}
		}
	}
	
	private void wrap(HttpResponse response) {
		if (response != null && response.getStatusLine() != null) {
			StatusLine status = response.getStatusLine();
			if (status != null) {
				this.ResponseCode = status.getStatusCode();
				this.ResponseMessage = status.getReasonPhrase();
			}
		}
	}
}
