package com.raizlabs.webservicemanager.caching;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;

import com.raizlabs.coreutils.collections.MappableSet;
import com.raizlabs.coreutils.concurrent.ConcurrencyUtils;
import com.raizlabs.coreutils.concurrent.Prioritized.Priority;
import com.raizlabs.coreutils.functions.Delegate;
import com.raizlabs.webservicemanager.requests.RequestBuilder;
import com.raizlabs.webservicemanager.requests.WebServiceRequest;
import com.raizlabs.webservicemanager.webservicemanager.ResultInfo;
import com.raizlabs.webservicemanager.webservicemanager.WebServiceManager;
import com.raizlabs.webservicemanager.webservicemanager.WebServiceRequestListener;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A class which caches the results of {@link WebServiceRequest}s into local {@link File}s.
 * Once a request has been executed, subsequent calls will reuse the data stored in the
 * {@link File} again instead of re-performing the request if it is still considered
 * to be fresh data.
 * 
 * @author Dylan James
 *
 * @param <Key> The type of object that will be used as the key to distinguish individual
 * requests.
 */
public abstract class WebFileCache<Key> {
	/**
	 * Class which contains the result of an asynchronous {@link WebFileCache} request.	 *
	 */
	public interface WebFileCacheResult {
		/**
		 * @return True if the request has completed
		 */
		public boolean isCompleted();
		/**
		 * Cancels this request
		 */
		public void cancel();
		/**
		 * @return True if the request has started executing.
		 */
		public boolean isStarted();
		
		public void addCacheListener(CacheListener listener);
		public boolean removeCacheListener(CacheListener listener);
	}
	
	public interface CacheResult {
		public File getResultFile();
	}
	
	private static class CacheResultImplementation implements CacheResult {
		private File resultFile;

		public CacheResultImplementation(File resultFile) {
			this.resultFile = resultFile;
		}
		
		@Override
		public File getResultFile() {
			return resultFile;
		}
	}
	
	public interface FailureInfo {
		
	}
	
	private class FailureInfoImp implements FailureInfo {
		
	}
	
	public interface CacheListener {
		public void onCacheResult(CacheResult result);
		public void onCacheFailure(FailureInfo info);
	}
	
	private static class CacheListenerSet extends MappableSet<CacheListener> {
		public void onResult(final CacheResult result) {
			map(new Delegate<CacheListener>() {
				@Override
				public void execute(CacheListener listener) {
					listener.onCacheResult(result);
				}
			});
		}
		
		public void onFailure(final FailureInfo info) {
			map (new Delegate<CacheListener>() {
				@Override
				public void execute(CacheListener listener) {
					listener.onCacheFailure(info);
				}
			});
		}
	}

	private static class BasicWebFileCacheResult implements WebFileCacheResult {
		boolean isCompleted;
		WebServiceRequest<Boolean> request;
		CacheListenerSet listeners;

		public BasicWebFileCacheResult() {
			this.isCompleted = false;
			this.listeners = new CacheListenerSet();
		}

		void setCompleted(boolean completed) {
			synchronized (this) {
				this.isCompleted = completed;
			}
		}

		void onCompleted(CacheResult result) {
			synchronized (this) {
				setCompleted(true);
				listeners.onResult(result);
			}
		}
		
		void onFailed(FailureInfo info) {
			synchronized (this) {
				setCompleted(true);
				listeners.onFailure(info);
			}
		}

		public boolean isCompleted() {
			synchronized (this) {
				return isCompleted;
			}
		}

		@Override
		public void cancel() {
			if (request != null) {
				request.cancel();
			}
		}

		@Override
		public boolean isStarted() {
			if (request != null) {
				return request.isStarted();
			} else {
				return false;
			}
		}

		@Override
		public void addCacheListener(CacheListener listener) {
			if (listener != null) {
				this.listeners.add(listener);
			}
		}

		@Override
		public boolean removeCacheListener(CacheListener listener) {
			return this.listeners.remove(listener);
		}
	}

	/**
	 * Class which manages a set of locks for keys of a given type.
	 * @param <KeyType> The type of key which locks will be mapped to.
	 */
	private static class LockManager<KeyType> {
		private HashMap<KeyType, Object> locks;

		public LockManager() {
			locks = new HashMap<KeyType, Object>();
		}

		public Object getLockForKey(KeyType key) {
			Object lock = locks.get(key);
			if (lock == null) {
				synchronized (this) {
					lock = locks.get(key);
					if (lock == null) {
						lock = new Object();
						locks.put(key, lock);
					}
				}
			}

			return lock;
		}
	}


	/**
	 * Map of Events for download completion, containing all listeners.
	 */
	private ConcurrentHashMap<Key, CacheListenerSet> cacheListeners;
	/**
	 * Set of keys which are currently downloading.
	 */
	private HashSet<Key> currentDownloads;

	private LockManager<Key> lockManager;

	private CompletedDownloadManager completedDownloads;
	private WebServiceManager webServiceManager;

	private Handler backgroundHandler;
	/**
	 * @return A {@link Handler} which can be used to do background work
	 */
	Handler getBackgroundHandler() { return backgroundHandler; }

	/**
	 * Constructs a new {@link WebFileCache} with the given parameters.
	 * @param name The name of the {@link WebFileCache}. This should be unique
	 * across the application to avoid collisions.
	 * @param webManager The {@link WebServiceManager} to use to perform web requests.
	 * @param context A {@link Context} to use to access resources. This is only used
	 * for initialization and will not be stored.
	 */
	public WebFileCache(String name, WebServiceManager webManager, Context context) {
		this.webServiceManager = webManager;
		cacheListeners = new ConcurrentHashMap<Key, CacheListenerSet>();
		currentDownloads = new HashSet<Key>();

		HandlerThread handlerThread = new HandlerThread("WebFileCache(" + name + ") Background");
		handlerThread.start();
		backgroundHandler = new Handler(handlerThread.getLooper());

		completedDownloads = new CompletedDownloadManager(name, context, backgroundHandler);

		lockManager = new LockManager<Key>();
	}

	/**
	 * Gets the lock which should be used for the synchronization of the status
	 * of the given key.
	 * @param key The key to get the lock for.
	 * @return The lock to use for the status of the key.
	 */
	protected Object getLockForKey(Key key) {
		return lockManager.getLockForKey(key);
	}

	/**
	 * Retrieves the file for the given request immediately if it is already cached.
	 * @param request The {@link RequestBuilder} to obtain the file for.
	 * @param freshOnly True if the data should only be returned if it is considered
	 * fresh. False to ignore the age.
	 * @return The local file if the request is already cached locally, otherwise null.
	 */
	public File getFileIfCached(RequestBuilder request, boolean freshOnly) {
		// Key the files by the URI
		final Key key = getKeyForRequest(request);
		// Get the local file we will find or store the file at
		final File localFile = getFileForKey(key);

		synchronized (getLockForKey(key)) {
			if (isDownloaded(localFile)) {
				if (freshOnly) {
					if (!isFresh(request, localFile)) {
						return null;
					}
				}
				return localFile;
			}
		}
		return null;
	}


	///////////////
	// Retrieval //
	///////////////
	/**
	 * Retrieves the file for the given request with normal priority, calling the given listener when it is
	 * retrieved. If the file is already in the cache, the listener will be called before this function returns
	 * on the same thread, otherwise it will be retrieved asynchronously.
	 * @param request The {@link RequestBuilder} to execute to get the file.
	 * @param cacheListener A listener to call when the file is retrieved. This listener will be 
	 * automatically added to the returned {@link WebFileCacheResult}.
	 * @return An {@link WebFileCacheResult} object which provides access to the request status and result.
	 */
	public WebFileCacheResult getFile(final RequestBuilder request, CacheListener cacheListener) {
		return getFile(request, cacheListener, Priority.NORMAL);
	}

	/**
	 * Retrieves the file for the given request with given priority, calling the given listener when it is
	 * retrieved. If the file is already in the cache, the listener will be called before this function
	 * returns on the same thread, otherwise it will be retrieved asynchronously.
	 * @param request The {@link RequestBuilder} to execute to get the file.
	 * @param cacheListener A listener to call when the file is retrieved. This listener will be 
	 * automatically added to the returned {@link WebFileCacheResult}.
	 * @param priority The priority of the download. See {@link Priority} for pre-defined values.
	 * @return An {@link WebFileCacheResult} object which provides access to the request status and result.
	 */
	public WebFileCacheResult getFile(final RequestBuilder request, CacheListener cacheListener, int priority) {
		return getFile(request, cacheListener, false, priority);
	}

	/**
	 * Retrieves the file for the given request with normal priority, calling the given listener when it is
	 * retrieved. If the file is already in the cache, the listener will be called before this function
	 * returns on the same thread, otherwise it will be retrieved asynchronously.
	 * @param request The {@link RequestBuilder} to execute to get the file.
	 * cacheListener
	 * @param forceDownload True to force the download, even if it is already cached.
	 * @return An {@link WebFileCacheResult} object which provides access to the request status and result.
	 */
	public WebFileCacheResult getFile(final RequestBuilder request, CacheListener cacheListener, boolean forceDownload) {
		return getFile(request, cacheListener, forceDownload, Priority.NORMAL);
	}


	/**
	 * Retrieves the file for the given request with given priority, calling the given listener when it is
	 * retrieved. If the file is already in the cache, the listener will be called before this function
	 * returns on the same thread, otherwise it will be retrieved asynchronously.
	 * @param request The {@link RequestBuilder} to execute to get the file.
	 * cacheListener
	 * @param forceDownload True to force the download, even if it is already cached.
	 * @param priority The priority of the download. See {@link Priority} for pre-defined values.
	 * @return An {@link WebFileCacheResult} object which provides access to the request status and result.
	 */
	public WebFileCacheResult getFile(final RequestBuilder request, CacheListener cacheListener, final boolean forceDownload, final int priority) {
		// Get the key for the request
		final Key key = getKeyForRequest(request);
		// Get the local file we will find or store the file at
		final File localFile = getFileForKey(key);

		final BasicWebFileCacheResult requestInfo = new BasicWebFileCacheResult();
		
		// Synchronize on this for thread safety
		// Don't want to try to download the same file twice etc
		synchronized (getLockForKey(key)) {
			// If it's being downloaded, subscribe the given completion listener to
			// the event for the download
			if (isDownloading(key)) {
				subscribeListener(key, new CacheListener() {
					
					@Override
					public void onCacheResult(CacheResult result) {
						requestInfo.onCompleted(result);
					}
					
					@Override
					public void onCacheFailure(FailureInfo info) {
						requestInfo.onFailed(info);
					}
				});
				requestInfo.setCompleted(false);
				requestInfo.addCacheListener(cacheListener);
				return requestInfo;
			}

			// Otherwise, if it's downloaded, and we aren't forcing a download,
			// call the listener
			if (isDownloaded(localFile) && !forceDownload && isFresh(request, localFile)) {
				if (cacheListener != null) {
					cacheListener.onCacheResult(new CacheResultImplementation(localFile));
				}
				
				requestInfo.setCompleted(true);
				return requestInfo;
			} else if (localFile.exists()) {
				// If the local file exists, delete it
				localFile.delete();
			}
			
			// Indicate that we are now downloading the file
			indicateDownloading(key, cacheListener);
			// Set up an event listener to handle the response from the WebServiceManager
			WebServiceRequestListener<Boolean> listener = new WebServiceRequestListener<Boolean>() {
				@Override
				public void onRequestComplete(WebServiceManager manager, ResultInfo<Boolean> result) {
					// If the request fails, delete the file and raise completion with no file
					if (result == null || result.getResult() == null || !result.getResult()) {
						localFile.delete();
						FailureInfo info = new FailureInfoImp();
						onDownloadFailed(key, request, info);
						requestInfo.onFailed(info);
					} else {
						// Otherwise, call the listener with the local file
						CacheResult cacheResult = new CacheResultImplementation(localFile);
						onDownloadComplete(key, request, cacheResult);
						requestInfo.onCompleted(cacheResult);
					}
				}
			};
			// Execute the request in the background with the specified priority
			final WebServiceRequest<Boolean> download = getRequest(request, localFile);
			requestInfo.request = download;
			webServiceManager.doRequestInBackground(download, listener, priority);
			requestInfo.setCompleted(false);
		}
		return requestInfo;
	}
	
	private static class RequestLock {
		public File file;
		public boolean completed = false;
		
		public CacheListener completionListener = new CacheListener() {
			@Override
			public void onCacheResult(CacheResult result) {
				onResult(result.getResultFile());
			}

			@Override
			public void onCacheFailure(FailureInfo info) {
				onResult(null);
			}
		};
		
		private void onResult(File file) {
			RequestLock requestLock = RequestLock.this;
			synchronized (requestLock) {
				requestLock.file = file;
				requestLock.completed = true;
				requestLock.notifyAll();
			}
		}
	}
	
	/**
	 * Retrieves the file for the given request with default priority. This call will block
	 * until the request has completed.
	 * @param request The {@link RequestBuilder} to execute to get the file.
	 * @param forceDownload True to force the download, even if it is already cached.
	 * @return The retrieved file, which may be null.
	 */
	public File getFileSynchronous(RequestBuilder request, final boolean forceDownload) {
		final RequestLock requestLock = new RequestLock();
		getFile(request, requestLock.completionListener, forceDownload);
		
		return waitForResult(requestLock);
	}
	
	/**
	 * Retrieves the file for the given request with given priority. This call will block
	 * until the request has completed.
	 * @param request The {@link RequestBuilder} to execute to get the file.
	 * @param forceDownload True to force the download, even if it is already cached.
	 * @param priority The priority of the download. See {@link Priority} for pre-defined values.
	 * @return The retrieved file, which may be null.
	 */
	public File getFileSynchronous(RequestBuilder request, final boolean forceDownload, final int priority) {
		final RequestLock requestLock = new RequestLock();
		getFile(request, requestLock.completionListener, forceDownload, priority);
		
		return waitForResult(requestLock);
	}
	
	/**
	 * Removes the download data for the given request if it exists and has
	 * completed. Does nothing if the data is currently being downloaded.
	 * @param request The request to remove the data for.
	 * @return True if data was removed, false if no data was found.
	 */
	public boolean removeFile(RequestBuilder request) {
		return removeFileForKey(getKeyForRequest(request));
	}
	
	/**
	 * Removes the downloaded data for the given key if it exists and has
	 * completed. Does nothing if the key is currently being downloaded.
	 * @param key The key to remove the data for.
	 * @return True if data was removed, false if no data was not found.
	 */
	protected boolean removeFileForKey(Key key) {
		File file = getFileForKey(key);
		synchronized (getLockForKey(key)) {
			if (isDownloaded(file)) {
				completedDownloads.removeDownload(file);
				file.delete();
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Removes all currently completed downloads. Will not impact any files
	 * which are currently downloading.
	 */
	public void clear() {
		// Create a "copy" of this set since it will be modified as we remove things
		Set<File> files = new HashSet<File>(getDownloadedFiles());
		for (File file : files) {
			file.delete();
			completedDownloads.removeDownload(file);
		}
	}
	
	private File waitForResult(RequestLock requestLock) {
		while (!requestLock.completed) {
			synchronized (requestLock) {
				try {
					requestLock.wait();
				} catch (InterruptedException e) { }
			}
		}
		
		return requestLock.file;
	}
	
	/**
	 * Gets the {@link WebServiceRequest} to use to perform the given request and
	 * store it in the given {@link File}.
	 * @param builder The {@link RequestBuilder} to execute.
	 * @param targetFile The {@link File} to store the results in.
	 * @return A {@link WebServiceRequest} to execute.
	 */
	protected abstract WebServiceRequest<Boolean> getRequest(RequestBuilder builder, File targetFile);

	////////////////////
	// Download State //
	////////////////////
	/**
	 * Returns true if the given File is already downloaded.
	 * @param file
	 * @return
	 */
	private boolean isDownloaded(File file) {
		if (completedDownloads.isDownloaded(file)) {
			if (file.exists()) {
				return true;
			} else {
				completedDownloads.removeDownload(file);
				return false;
			}
		}
		return false;
	}

	/**
	 * Returns true if the given key is currently being downloaded.
	 * @param key
	 * @return
	 */
	private boolean isDownloading(Key key) {
		return currentDownloads.contains(key);
	}
	
	/**
	 * @return A set containing all files which are currently downloaded.
	 */
	protected Set<File> getDownloadedFiles() {
		return completedDownloads.getCompletedFiles();
	}

	private boolean isFresh(RequestBuilder request, File file) {
		synchronized (getLockForKey(getKeyForRequest(request))) {
			return isFresh(request, completedDownloads.getAge(file));
		}
	}

	/**
	 * Gets whether the given request is still considered fresh data for the
	 * given age.
	 * @param request The request the data is for.
	 * @param age The time since the data was obtained in milliseconds.
	 * @return True if the data is still fresh, false if it should be ignored
	 * and re-requested.
	 */
	protected abstract boolean isFresh(RequestBuilder request, long age);

	/**
	 * Call to indicate that a download has been completed. Calls all the listeners and removes
	 * it from the current download state.
	 * @param key The key of the item that was finished.
	 * @param request The request that was executed.
	 * @param result The cache result.
	 */
	protected void onDownloadComplete(Key key, RequestBuilder request, final CacheResult result) {
		onDownloadResult(key, result.getResultFile(), new Delegate<CacheListenerSet>() {
			@Override
			public void execute(CacheListenerSet listenerSet) {
				listenerSet.onResult(result);
			}
		});
	}
	
	protected void onDownloadFailed(Key key, RequestBuilder request, final FailureInfo info) {
		onDownloadResult(key, null, new Delegate<CacheListenerSet>() {
			@Override
			public void execute(CacheListenerSet listenerSet) {
				listenerSet.onFailure(info);
			}
		});
	}
	
	private void onDownloadResult(Key key, File file, Delegate<CacheListenerSet> listenerAction) {
		// Synchronize back on the WebFileCache so that we do not raise the event
		// while someone is subscribing
		synchronized (getLockForKey(key)) {
			currentDownloads.remove(key);
			completedDownloads.onDownloadComplete(file);

			// Remove the event from the downloads, so no one can subscribe anymore
			CacheListenerSet completionEvent = cacheListeners.remove(key);
			// Notify all listeners
			if (completionEvent != null) {
				if (listenerAction != null) {
					listenerAction.execute(completionEvent);
				}
				completionEvent.clear();
			}
		}
	}

	/**
	 * Call to indicate that that an item is downloading.
	 * @param key The key of the item that is downloading.
	 * @param listener An optional listener to subscribe to the completion event.
	 */
	protected void indicateDownloading(Key key, CacheListener listener) {		
		synchronized (getLockForKey(key)) {
			currentDownloads.add(key);
			completedDownloads.removeDownload(getFileForKey(key));

			// Create a new event to put in the list
			CacheListenerSet listenerSet = getListenerSetForKey(key);
			// Subscribe the current completion listener
			if (listener != null) {
				listenerSet.add(listener);
			}
		}
	}


	/////////////
	// Helpers //
	/////////////
	/**
	 * Subscribes the given listener to the completion event for the download with
	 * the given key
	 * @param key The key of the download
	 * @param listener The listener to subscribe
	 * @return True if the item was added, otherwise false.
	 */
	protected boolean subscribeListener(Key key, CacheListener listener) {
		CacheListenerSet listenerSet = getListenerSetForKey(key);
		if (listener != null) {
			listenerSet.add(listener);
			return true;
		}

		return false;
	}

	/**
	 * Gets the download listener set for the item with the given key,
	 * creating one if it doesn't currently exist
	 * @param key
	 * @return
	 */
	private CacheListenerSet getListenerSetForKey(Key key) {
		return ConcurrencyUtils.putIfAbsent(cacheListeners, key, new CacheListenerSet());
	}

	/**
	 * Gets the object to use as the key for the given request.
	 * @param request The {@link RequestBuilder} to obtain the key for.
	 * @return The object to be used as the key.
	 */
	protected abstract Key getKeyForRequest(RequestBuilder request);
	/**
	 * Gets the {@link File} where the data for the given key should be stored.
	 * @param key The key to obtain the {@link File} for.
	 * @return The {@link File} to store the data in.
	 */
	protected abstract File getFileForKey(Key key);

	/**
	 * Class which handles a set of download states and properties. Note that
	 * this class is not thread-safe for performance reasons. It is likely
	 * already synchronized externally or easier/cheaper to do externally
	 * than it is to do it inside here.
	 */
	private static class CompletedDownloadManager{
		private static final String PREFERENCES_NAME_FORMAT = "com.raizlabs.net.caching.WebFileCache:%s";
		public static final long VALUE_NOT_DOWNLOADED = Long.MIN_VALUE;
		private SharedPreferences preferences;
		private Editor preferencesEditor;
		private HashMap<File, Long> completedDownloads;

		private Handler backgroundHandler;

		public CompletedDownloadManager(String name, Context context, Handler backgroundHandler) {
			this.backgroundHandler = backgroundHandler;
			String prefsName = String.format(PREFERENCES_NAME_FORMAT, name);
			preferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
			preferencesEditor = preferences.edit();
			completedDownloads = new HashMap<File, Long>();
			loadFromPreferences();
		}

		private void loadFromPreferences() {
			final Map<String, ?> downloads = preferences.getAll();
			for (Entry<String, ?> entry : downloads.entrySet()) {
				final Object value = entry.getValue();
				if (value instanceof Long) {
					long timeCompleted = ((Long) value).longValue();
					if (timeCompleted != VALUE_NOT_DOWNLOADED) {
						File file = new File(entry.getKey());
						completedDownloads.put(file, timeCompleted);
					}
				}
			}
		}

		public void removeDownload(File file) {
			preferencesEditor.putLong(file.getAbsolutePath(), VALUE_NOT_DOWNLOADED);
			commitPreferences();
			completedDownloads.remove(file);
		}
		
		public void onDownloadComplete(File file) {
			if (file != null && file.exists()) {
				final long completedTime = System.currentTimeMillis();
				preferencesEditor.putLong(file.getAbsolutePath(), completedTime);
				commitPreferences();
				completedDownloads.put(file, completedTime);
			}
		}
		
		public Set<File> getCompletedFiles() {
			return completedDownloads.keySet();
		}
		
		private Runnable commitPrefsRunnable = new Runnable() {
			@Override
			public void run() {
				preferencesEditor.commit();
			}
		};

		@SuppressLint("NewApi")
		private void commitPreferences() {
			if (Build.VERSION.SDK_INT >= 9) {
				preferencesEditor.apply();
			} else {
				backgroundHandler.post(commitPrefsRunnable);
			}
		}

		public boolean isDownloaded(File file) {
			Long timeCompleted = completedDownloads.get(file);
			return (timeCompleted != null) && (timeCompleted.longValue() != VALUE_NOT_DOWNLOADED);
		}

		/**
		 * Gets the time since the given {@link File} was downloaded.
		 * @param file The file to get the age of.
		 * @return The time since the file was downloaded in milliseconds, or
		 * {@link #VALUE_NOT_DOWNLOADED} if the File was not downloaded.
		 */
		public long getAge(File file) {
			Long timeCompleted = completedDownloads.get(file);
			if (timeCompleted != null && timeCompleted.longValue() != VALUE_NOT_DOWNLOADED) {
				return System.currentTimeMillis() - timeCompleted;
			} else {
				return VALUE_NOT_DOWNLOADED;
			}
		}
	}

}
