package com.raizlabs.net.caching;

import java.io.File;

import android.content.Context;

import com.raizlabs.net.requests.DownloadFileRequest;
import com.raizlabs.net.requests.RequestBuilder;
import com.raizlabs.net.requests.WebServiceRequest;
import com.raizlabs.net.webservicemanager.WebServiceManager;

/**
 * {@link WebFileCache} implementation which simply keys requests by their URL and
 * does simple file downloads to store the results.
 * @author Dylan James
 *
 */
public class SimpleWebFileCache extends WebFileCache<String> {
	private File cacheDir;
	private long maxAge;
	/**
	 * Creates a {@link SimpleWebFileCache} whose data never expires.
	 * @param name The name of the {@link WebFileCache}. This should be unique
	 * across the application to avoid collisions.
	 * @param webManager The {@link WebServiceManager} to use to perform web requests.
	 * @param context A {@link Context} to use to access resources. This is only used
	 * for initialization and will not be stored.
	 */
	public SimpleWebFileCache(String name, WebServiceManager webManager, Context context) {
		this(name, webManager, context, Long.MIN_VALUE);
	}
	
	/**
	 * Creates a {@link SimpleWebFileCache} whose data expires after the given age.
	 * @param name The name of the {@link WebFileCache}. This should be unique
	 * across the application to avoid collisions.
	 * @param webManager The {@link WebServiceManager} to use to perform web requests.
	 * @param context A {@link Context} to use to access resources. This is only used
	 * for initialization and will not be stored.
	 * @param maxAge The maximum allowed age of data in milliseconds. A negative value
	 * indicates that the data is always valid.
	 */
	public SimpleWebFileCache(String name, WebServiceManager webManager, Context context, long maxAge) {
		super(name, webManager, context);
		cacheDir = getCacheDir(name, context);
		this.maxAge = maxAge;
	}

	protected File getCacheDir(String name, Context context) {
		return new File(context.getCacheDir(), String.format("URLWebFileCaches/%s", name));
	}
	
	@Override
	protected WebServiceRequest<Boolean> getRequest(RequestBuilder builder, File targetFile) {
		return new DownloadFileRequest(targetFile, builder, null);
	}

	@Override
	protected String getKeyForRequest(RequestBuilder request) {
		return request.getRequest().getURI().getSchemeSpecificPart();
	}

	@Override
	protected File getFileForKey(String key) {
		return new File(cacheDir, key);
	}

	@Override
	protected boolean isFresh(RequestBuilder request, long age) {
		if (maxAge > 0) {
			return age < maxAge;
		} else {
			return true;
		}
	}
}
