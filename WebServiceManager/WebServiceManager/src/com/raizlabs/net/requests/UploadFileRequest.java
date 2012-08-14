package com.raizlabs.net.requests;

import java.io.File;

import com.raizlabs.baseutils.ProgressListener;
import com.raizlabs.net.HttpMethod;
import com.raizlabs.net.HttpUtils;
import com.raizlabs.net.responses.Response;

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
	public UploadFileRequest(RequestBuilder request, File localFile, ProgressListener listener) {
		request.setFileInput(localFile, listener);
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
		this.builder = new RequestBuilder(method, url).setFileInput(localFile, listener);
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
