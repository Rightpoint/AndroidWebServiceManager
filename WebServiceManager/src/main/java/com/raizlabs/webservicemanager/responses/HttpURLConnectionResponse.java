package com.raizlabs.webservicemanager.responses;

import android.text.TextUtils;

import com.raizlabs.webservicemanager.HttpMethod;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

/**
 * {@link Response} implementation which wraps an {@link HttpURLConnection}.
 * @author Dylan James
 *
 */
public class HttpURLConnectionResponse extends BaseResponse {
	private HttpURLConnection connection;
	
	/**
	 * Creates an {@link HttpURLConnectionResponse} from the given
	 * {@link HttpURLConnection}.
	 * @param connection The actual connection.
	 */
	public HttpURLConnectionResponse(HttpURLConnection connection) {
		this.connection = connection;
	}

	@Override
	public boolean containsHeader(String name) {
		return connection != null && !TextUtils.isEmpty(connection.getHeaderField(name));
	}

	@Override
	public String getHeaderValue(String name) {
		return connection == null ? null : connection.getHeaderField(name);
	}

	@Override
	public int getResponseCode() {
		try {
			return connection.getResponseCode();
		} catch (Exception e) {
			return -1;
		}
	}

	@Override
	public String getResponseMessage() {
		try {
			return connection.getResponseMessage();
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public String getContentEncoding() {
		return connection == null ? null : connection.getContentEncoding();
	}

	@Override
	public long getContentLength() {
		return connection == null ? null : connection.getContentLength();
	}

	@Override
	public String getContentType() {
		return connection == null ? null : connection.getContentType();
	}

	@Override
	public InputStream getContentStream() throws IOException {
		return connection == null ? null : connection.getInputStream();
	}
	
	@Override
	public HttpMethod getRequestMethod() {
		return connection == null ? null : HttpMethod.fromName(connection.getRequestMethod());
	}

	@Override
	public void close() {
		if (connection != null) {
			connection.disconnect();
		}
	}
}
