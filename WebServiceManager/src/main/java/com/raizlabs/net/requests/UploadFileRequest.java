package com.raizlabs.net.requests;

import com.raizlabs.coreutils.listeners.ProgressListener;
import com.raizlabs.net.HttpMethod;
import com.raizlabs.net.HttpUtils;
import com.raizlabs.net.responses.Response;

import java.io.File;

/**
 * Request class which allows the uploading of a local file and returns
 * whether the response is a standard OK response. 
 * @author Dylan James
 *
 */
public class UploadFileRequest extends BaseWebServiceRequest<Boolean>{

	private RequestBuilder builder;
	
	/**
	 * Creates an {@link UploadFileRequest} from a pre-populated {@link RequestBuilder}.
	 * The file and progress listener should already be set, else use 
	 * {@link #UploadFileRequest(RequestBuilder, File, ProgressListener)}
	 * @param request The pre-populated {@link RequestBuilder}
	 */
	public UploadFileRequest(RequestBuilder request) {
		this.builder = request;
	}
	
	/**
	 * Creates an {@link UploadFileRequest} from the given data.
	 * @param request The {@link RequestBuilder} to be used as a base for the request.
	 * @param localFile The local {@link File} to upload.
	 * @param listener The {@link ProgressListener} which will be called during the upload.
	 * (May be null).
	 */
	public UploadFileRequest(RequestBuilder request, File localFile, final ProgressListener listener) {
		// "Middle man" listener which will publish progress and call any external
		// progress listener
		ProgressListener mListener = new ProgressListener() {
			@Override
			public void onProgressUpdate(long currentProgress, long maxProgress) {
				publishProgress(currentProgress, maxProgress);
				if (listener != null) {
					listener.onProgressUpdate(currentProgress, maxProgress);
				}
			}
		};
		request.setFileInput(localFile, mListener);
		this.builder = request;
	}
	
	/**
	 * Creates an {@link UploadFileRequest} which uploads the given {@link File} to the
	 * given URL using an HTTP PUT.
	 * @param url The remote location to put the file.
	 * @param localFile The local {@link File} to upload.
	 * @param listener The {@link ProgressListener} which will be called during the upload.
	 * (May be null).
	 */
	public UploadFileRequest(String url, File localFile, ProgressListener listener) {
		this(HttpMethod.Put, url, localFile, listener);
	}
	
	/**
	 * Creates an {@link UploadFileRequest} which uploads the given {@link File} to the
	 * given URL using the given {@link HttpMethod}.
	 * @param method The {@link HttpMethod} to use.
	 * @param url The remote location to put the file.
	 * @param localFile The local {@link File} to upload.
	 * @param listener The {@link ProgressListener} which will be called during the upload.
	 * (May be null).
	 */
	public UploadFileRequest(HttpMethod method, String url, File localFile, ProgressListener listener) {
		this(new RequestBuilder(method, url), localFile, listener);
	}
	
	@Override
	protected RequestBuilder getRequestBuilder() {
		return builder;
	}

	@Override
	protected Boolean translate(Response response) {
		return HttpUtils.isResponseOK(response);
	}
}
