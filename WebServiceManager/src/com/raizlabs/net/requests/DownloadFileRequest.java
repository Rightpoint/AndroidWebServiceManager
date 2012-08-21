package com.raizlabs.net.requests;

import java.io.File;

import com.raizlabs.events.ProgressListener;
import com.raizlabs.net.HttpMethod;
import com.raizlabs.net.responses.Response;

/**
 * A request which downloads a remote file and returns true if the download
 * was successful.
 * @author Dylan James
 *
 */
public class DownloadFileRequest extends BaseWebServiceRequest<Boolean> {

	private RequestBuilder builder;
	private File localFile;
	private ProgressListener progressListener;
	/**
	 * Sets the {@link ProgressListener} which will be called during the download.
	 * @param listener
	 */
	public void setProgressListener(ProgressListener listener) {
		this.progressListener = listener;
	}
	
	/**
	 * Creates a {@link DownloadFileRequest} which downloads the content at the
	 * given URL to a {@link File} at the given path.
	 * @param localPath The path to download the content to.
	 * @param url The URL to get the content at (using an HTTP GET).
	 * @param listener The {@link ProgressListener} which will be called during
	 * the download. (May be null)
	 */
	public DownloadFileRequest(String localPath, String url, ProgressListener listener) {
		this(new File(localPath), url, listener);
	}
	
	/**
	 * Creates a {@link DownloadFileRequest} which downloads the content at the
	 * given URL to the given local {@link File}.
	 * @param localFile The {@link File} to download the content to.
	 * @param url The URL to get the content at (using an HTTP GET).
	 * @param listener The {@link ProgressListener} which will be called during
	 * the download. (May be null)
	 */
	public DownloadFileRequest(File localFile, String url, ProgressListener listener) {
		this (localFile, new RequestBuilder(HttpMethod.Get, url), listener);
	}
	
	/**
	 * Creates a {@link DownloadFileRequest} which downloads the content of the
	 * response to a {@link File} at the given path. 
	 * @param localPath The path to download the content to.
	 * @param request The {@link RequestBuilder} to execute.
	 * @param listener The {@link ProgressListener} which will be called during
	 * the download. (May be null)
	 */
	public DownloadFileRequest(String localPath, RequestBuilder request, ProgressListener listener) {
		this(new File(localPath), request, listener);
	}
	
	/**
	 * Creates a {@link DownloadFileRequest} which downloads the content of the
	 * response to the given local {@link File}.
	 * @param localFile The {@link File} to download the content to.
	 * @param request The {@link RequestBuilder} to execute.
	 * @param listener The {@link ProgressListener} which will be called during
	 * the download. (May be null)
	 */
	public DownloadFileRequest(File localFile, RequestBuilder request, ProgressListener listener) {
		if (localFile == null) {
			throw new NullPointerException("Local File cannot be null");
		}
		this.localFile = localFile;
		this.builder = request;
		this.progressListener = listener;
	}
	
	@Override
	protected RequestBuilder getRequestBuilder() {
		return builder;
	}

	@Override
	protected Boolean translate(Response response) {
		// "Middle man" listener which will publish progress and call any external
		// progress listener
		ProgressListener listener = new ProgressListener() {
			@Override
			public void onProgressUpdate(long currentProgress, long maxProgress) {
				publishProgress(currentProgress, maxProgress);
				if (progressListener != null) {
					progressListener.onProgressUpdate(currentProgress, maxProgress);
				}
			}
		};
		return response.readContentToFile(localFile, listener);
	}

}
