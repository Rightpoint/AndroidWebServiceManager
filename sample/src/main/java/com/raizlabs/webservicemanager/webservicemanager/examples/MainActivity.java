package com.raizlabs.webservicemanager.webservicemanager.examples;

import java.io.File;

import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.ImageView;

import com.raizlabs.android.coreutils.listeners.ProgressListener;
import com.raizlabs.webservicemanager.requests.DownloadFileRequest;
import com.raizlabs.webservicemanager.requests.JSONRequest;
import com.raizlabs.webservicemanager.webservicemanager.WebServiceManager;
import com.raizlabs.webservicemanager.webservicemanager.examples.requests.GetContentWithDynamicPathAndHost;
import com.raizlabs.webservicemanager.webservicemanager.examples.requests.ManualGetContent;
import com.raizlabs.webservicemanager.webservicemanager.examples.requests.ManualGetJSON;
import com.raizlabs.webservicemanager.webservicemanager.examples.requests.ManualGetLogo;

public class MainActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		final ImageView image = (ImageView) findViewById(R.id.imageView);

		// Run the tests on a new thread so we don't block the UI.
		new Thread(new Runnable() {
			@Override
			public void run() {
				// Create a new WebServiceManager
				final WebServiceManager manager = new WebServiceManager();
				
				// Start a request to grab an image
				// Implemented manually for sake of example
				ManualGetLogo logoRequest = new ManualGetLogo();
				final Bitmap logo = manager.doRequest(logoRequest).getResult();
				if (logo != null) {
					// Set the image on the UI thread
					MainActivity.this.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							image.setImageBitmap(logo);	
						}
					});
				}

				// Start a request for the string content from a URL
				// Implemented manually for sake of example
				ManualGetContent contentRequest = new ManualGetContent();
				String content = manager.doRequest(contentRequest).getResult();
				Log.d("Content", content);

				// Start a request to get JSON from a URL
				// Implemented manually for sake of example
				ManualGetJSON jsonRequest = new ManualGetJSON();
				JSONObject json = manager.doRequest(jsonRequest).getResult();
				Log.d("JSON", json == null ? "null" : json.toString());

				// Start a request to get the same JSON using basic built in request
				JSONRequest simpleJSONRequest = new JSONRequest("https://raw.github.com/Raizlabs/WebServiceManager/master/WebServiceManagerTests/TestData.json");
				JSONObject simpleJSON = manager.doRequest(simpleJSONRequest).getResult();
				Log.d("Simple JSON", simpleJSON == null ? "null" : json.toString());

				// Start a request to get the string content from a URL
				// Demonstrating how the path could be defined dynamically
				GetContentWithDynamicPathAndHost mgrTests = new GetContentWithDynamicPathAndHost("https://raw.github.com", "RZWebServiceManagerTests.m");
				String mgrTestContent = manager.doRequest(mgrTests).getResult();
				Log.d("RZWebServiceManagerTests.m", mgrTestContent == null ? "null" : mgrTestContent);

				
				// Request for downloading a file
				// Store it locally in the cache directory
				final File localFile = new File(getCacheDir(), "podcast.mp3");
				final String url = "http://s3.amazonaws.com/Raizlabs.com_CDN/podcast/AppCorner_Episode8.mp3";
				
				// Run back on the UI thread so we can show a dialog with progress
				MainActivity.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						final ProgressDialog podcastDialog = new ProgressDialog(MainActivity.this);
						podcastDialog.setMessage("Podcast download");
						podcastDialog.setIndeterminate(false);
						podcastDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
						podcastDialog.setProgress(0);
						
						// Create a request to download the file
						final DownloadFileRequest downloadFileRequest = new DownloadFileRequest(localFile, url, new ProgressListener() {
							@Override
							public void onProgressUpdate(long currentProgress, long maxProgress) {
								// On update, update the dialog and print the progress to the console
								podcastDialog.setMax((int) maxProgress);
								podcastDialog.setProgress((int) currentProgress);
								Log.d("Podcast Download Progress", Long.toString(currentProgress) + "/" + maxProgress + "(" + currentProgress * 100 / maxProgress + ")");
							}
						});
						
						// Allow the dialog to be cancelled
						// When it is cancelled, cancel the request.
						// This demonstrates request cancellations.
						podcastDialog.setCancelable(true);
						podcastDialog.setOnCancelListener(new OnCancelListener() {
							@Override
							public void onCancel(DialogInterface dialog) {
								downloadFileRequest.cancel();
							}
						});
						// Now the dialog is fully set up, so display it
						podcastDialog.show();
						
						// Run the request on another thread so we don't block the UI
						new Thread(new Runnable() {
							@Override
							public void run() {
								// Execute the request
								boolean downloadSuccess = manager.doRequest(downloadFileRequest).getResult();
								// Hide the dialog
								podcastDialog.dismiss();
								// Log the result.
								Log.d("Podcast Download", "Success: " + downloadSuccess);						
							}
						}).start();
					}
				});
				}
			}).start();
		}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}   
}