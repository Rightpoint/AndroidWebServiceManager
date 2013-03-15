package com.raizlabs.net.caching;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;
import android.content.SharedPreferences;

import com.raizlabs.concurrent.ConcurrencyUtils;
import com.raizlabs.concurrent.Prioritized.Priority;
import com.raizlabs.events.Event;
import com.raizlabs.events.EventListener;
import com.raizlabs.net.requests.RequestBuilder;
import com.raizlabs.net.requests.WebServiceRequest;
import com.raizlabs.net.webservicemanager.ResultInfo;
import com.raizlabs.net.webservicemanager.WebServiceManager;

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
	 * Map of Events for download completion, containing all listeners.
	 */
	private ConcurrentHashMap<Key, Event<File>> downloadEvents;
	/**
	 * Set of keys which are currently downloading.
	 */
	private HashSet<Key> currentDownloads;

	private CompletedDownloadManager completedDownloads;
	private WebServiceManager webServiceManager;

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
		downloadEvents = new ConcurrentHashMap<Key, Event<File>>();
		currentDownloads = new HashSet<Key>();
		completedDownloads = new CompletedDownloadManager(name, context);
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

		synchronized (this) {
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
	 * @param completionListener An {@link EventListener} to call when the file is retrieved. This will be called
	 * with null if the download fails.
	 * @return True if the data was already cached and the listener was already called.
	 */
	public boolean getFile(final RequestBuilder request, final EventListener<File> completionListener) {
		return getFile(request, completionListener, Priority.NORMAL);
	}

	/**
	 * Retrieves the file for the given request with given priority, calling the given listener when it is
	 * retrieved. If the file is already in the cache, the listener will be called before this function
	 * returns on the same thread, otherwise it will be retrieved asynchronously.
	 * @param request The {@link RequestBuilder} to execute to get the file.
	 * @param completionListener An {@link EventListener} to call when the file is retrieved. This will be called
	 * with null if the download fails.
	 * @param priority The priority of the download. See {@link Priority} for pre-defined values.
	 * @return True if the data was already cached and the listener was already called.
	 */
	public boolean getFile(final RequestBuilder request, final EventListener<File> completionListener, int priority) {
		return getFile(request, completionListener, false, priority);
	}
	
	/**
	 * Retrieves the file for the given request with normal priority, calling the given listener when it is
	 * retrieved. If the file is already in the cache, the listener will be called before this function
	 * returns on the same thread, otherwise it will be retrieved asynchronously.
	 * @param request The {@link RequestBuilder} to execute to get the file.
	 * @param completionListener An {@link EventListener} to call when the file is retrieved. This will be called
	 * with null if the download fails.
	 * @param forceDownload True to force the download, even if it is already cached.
	 * @return True if the data was already cached and the listener was already called.
	 */
	public boolean getFile(final RequestBuilder request, final EventListener<File> completionListener, boolean forceDownload) {
		return getFile(request, completionListener, forceDownload, Priority.NORMAL);
	}


	/**
	 * Retrieves the file for the given request with given priority, calling the given listener when it is
	 * retrieved. If the file is already in the cache, the listener will be called before this function
	 * returns on the same thread, otherwise it will be retrieved asynchronously.
	 * @param request The {@link RequestBuilder} to execute to get the file.
	 * @param completionListener An {@link EventListener} to call when the file is retrieved. This will be called
	 * with null if the download fails.
	 * @param forceDownload True to force the download, even if it is already cached.
	 * @param priority The priority of the download. See {@link Priority} for pre-defined values.
	 * @return True if the data was already cached and the listener was already called.
	 */
	public boolean getFile(final RequestBuilder request, final EventListener<File> completionListener, final boolean forceDownload, final int priority) {
		// Get the key for the request
		final Key key = getKeyForRequest(request);
		// Get the local file we will find or store the file at
		final File localFile = getFileForKey(key);

		// Synchronize on this for thread safety
		// Don't want to try to download the same file twice etc
		synchronized (this) {
			// If it's being downloaded, subscribe the given completion listener to
			// the event for the download
			if (isDownloading(key)) {
				if (completionListener != null) subscribeListener(key, completionListener);
				return false;
			}

			// Otherwise, if it's downloaded, and we aren't forcing a download,
			// call the listener
			if (isDownloaded(localFile) && !forceDownload && isFresh(request, localFile)) {
				if (completionListener != null) {
					completionListener.onEvent(request, localFile);
				}
				return true;
			} else if (localFile.exists()) {
				// If the local file exists, delete it
				localFile.delete();
			}
			
			// Indicate that we are now downloading the file
			indicateDownloading(key, completionListener);
			// Set up an event listener to handle the response from the WebServiceManager
			EventListener<ResultInfo<Boolean>> listener = new EventListener<ResultInfo<Boolean>>() {
				@Override
				public void onEvent(Object sender, ResultInfo<Boolean> result) {
					// If the request fails, delete the file and raise completion with no file
					if (result == null || !result.getResult()) {
						localFile.delete();
						onDownloadComplete(key, request, null);
					} else {
						// Otherwise, call the listener with the local file
						onDownloadComplete(key, request, localFile);
					}
				}
			};
			// Execute the request in the background with the specified priority
			final WebServiceRequest<Boolean> download = getRequest(request, localFile);
			webServiceManager.doRequestInBackground(download, listener, priority);
		}
		return false;
	}
	
	private static class RequestLock {
		public File file;
		public boolean completed = false;
		
		public EventListener<File> completionListener = new EventListener<File>() {
			@Override
			public void onEvent(Object sender, File args) {
				RequestLock requestLock = RequestLock.this;
				synchronized (requestLock) {
					requestLock.file = args;
					requestLock.completed = true;
					requestLock.notifyAll();
				}
			}
		};
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
		getFile(request, requestLock.completionListener);
		
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
		getFile(request, requestLock.completionListener, priority);
		
		return waitForResult(requestLock);
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
				completedDownloads.onDownloadInvalidated(file);
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
	
	private boolean isFresh(RequestBuilder request, File file) {
		return isFresh(request, completedDownloads.getAge(file));
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
	 * @param localFile The file where the result is stored
	 */
	protected void onDownloadComplete(Key key, RequestBuilder request, File localFile) {
		// Regardless, raise the completion event
		// Synchronize back on the WebFileCache so that we do not raise the event
		// while someone is subscribing
		synchronized (this) {
			currentDownloads.remove(key);
			completedDownloads.onDownloadComplete(localFile);
			
			// Remove the event from the downloads, so no one can subscribe anymore
			Event<File> completionEvent = downloadEvents.remove(key);
			// Notify all listeners
			if (completionEvent != null) {
				completionEvent.raiseEvent(request, localFile);
				completionEvent.clear();
			}
		}
	}

	/**
	 * Call to indicate that that an item is downloading.
	 * @param key The key of the item that is downloading.
	 * @param listener An optional listener to subscribe to the completion event.
	 */
	protected void indicateDownloading(Key key, EventListener<File> listener) {		
		synchronized (this) {
			currentDownloads.add(key);
			completedDownloads.onDownloadInvalidated(getFileForKey(key));

			// Create a new event to put in the list
			Event<File> event = getEventForKey(key);
			// Subscribe the current completion listener
			if (listener != null) {
				event.addListener(listener);
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
	protected boolean subscribeListener(Key key, EventListener<File> listener) {
		Event<File> event = getEventForKey(key);
		if (listener != null) {
			event.addListener(listener);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Gets the download completion event for the item with the given key,
	 * creating one if it doesn't currently exist
	 * @param key
	 * @return
	 */
	private Event<File> getEventForKey(Key key) {
		return ConcurrencyUtils.putIfAbsent(downloadEvents, key, new Event<File>());
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

	private static class CompletedDownloadManager{
		private static final String PREFERENCES_NAME_FORMAT = "com.raizlabs.net.caching.WebFileCache:%s";
		public static final long VALUE_NOT_DOWNLOADED = Long.MIN_VALUE;
		private SharedPreferences preferences;
		private HashMap<File, Long> completedDownloads;

		public CompletedDownloadManager(String name, Context context) {
			String prefsName = String.format(PREFERENCES_NAME_FORMAT, name);
			preferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
			completedDownloads = new HashMap<File, Long>();
			loadFromPreferences();
		}

		private void loadFromPreferences() {
			synchronized (completedDownloads) {
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
		}

		public void onDownloadInvalidated(File file) {
			synchronized (completedDownloads) {
				preferences.edit().putLong(file.getAbsolutePath(), VALUE_NOT_DOWNLOADED).commit();
				completedDownloads.remove(file);
			}
		}

		public void onDownloadComplete(File file) {
			final long completedTime = System.currentTimeMillis();
			synchronized (completedDownloads) {
				preferences.edit().putLong(file.getAbsolutePath(), completedTime).commit();
				completedDownloads.put(file, completedTime);
			}
		}

		public boolean isDownloaded(File file) {
			synchronized (completedDownloads) {
				Long timeCompleted = completedDownloads.get(file);
				return (timeCompleted != null) && (timeCompleted.longValue() != VALUE_NOT_DOWNLOADED);
			}
		}
		
		/**
		 * Gets the time since the given {@link File} was downloaded.
		 * @param file The file to get the age of.
		 * @return The time since the file was downloaded in milliseconds, or
		 * {@link #VALUE_NOT_DOWNLOADED} if the File was not downloaded.
		 */
		public long getAge(File file) {
			synchronized (completedDownloads) {
				Long timeCompleted = completedDownloads.get(file);
				if (timeCompleted != null && timeCompleted.longValue() != VALUE_NOT_DOWNLOADED) {
					return System.currentTimeMillis() - timeCompleted;
				} else {
					return VALUE_NOT_DOWNLOADED;
				}
			}
		}
	}

}
