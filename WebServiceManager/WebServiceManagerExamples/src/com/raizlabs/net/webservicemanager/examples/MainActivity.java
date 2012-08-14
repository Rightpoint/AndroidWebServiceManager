package com.raizlabs.net.webservicemanager.examples;

import java.io.File;

import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.ImageView;

import com.raizlabs.baseutils.ProgressListener;
import com.raizlabs.net.requests.DownloadFileRequest;
import com.raizlabs.net.requests.JSONRequest;
import com.raizlabs.net.webservicemanager.WebServiceManager;
import com.raizlabs.net.webservicemanager.examples.requests.GetContentWithDynamicPathAndHost;
import com.raizlabs.net.webservicemanager.examples.requests.ManualGetContent;
import com.raizlabs.net.webservicemanager.examples.requests.ManualGetJSON;
import com.raizlabs.net.webservicemanager.examples.requests.ManualGetLogo;

public class MainActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		final ImageView image = (ImageView) findViewById(R.id.imageView);

		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {

				final WebServiceManager manager = new WebServiceManager();
				ManualGetLogo logoRequest = new ManualGetLogo();
				final Bitmap logo = manager.doRequest(logoRequest).getResult();
				if (logo != null) {
					MainActivity.this.runOnUiThread(new Runnable() {

						@Override
						public void run() {
							image.setImageBitmap(logo);	
						}
					});
				}

				ManualGetContent contentRequest = new ManualGetContent();
				String content = manager.doRequest(contentRequest).getResult();
				Log.d("Content", content);

				ManualGetJSON jsonRequest = new ManualGetJSON();
				JSONObject json = manager.doRequest(jsonRequest).getResult();
				Log.d("JSON", json == null ? "null" : json.toString());

				JSONRequest simpleJSONRequest = new JSONRequest("https://raw.github.com/Raizlabs/WebServiceManager/master/WebServiceManagerTests/TestData.json");
				JSONObject simpleJSON = manager.doRequest(simpleJSONRequest).getResult();
				Log.d("Simple JSON", simpleJSON == null ? "null" : json.toString());

				GetContentWithDynamicPathAndHost mgrTests = new GetContentWithDynamicPathAndHost("https://raw.github.com", "RZWebServiceManagerTests.m");
				String mgrTestContent = manager.doRequest(mgrTests).getResult();
				Log.d("RZWebServiceManagerTests.m", mgrTestContent == null ? "null" : mgrTestContent);

				final File localFile = new File(getCacheDir(), "podcast.mp3");
				final String url = "http://s3.amazonaws.com/Raizlabs.com_CDN/podcast/AppCorner_Episode8.mp3";
				
				MainActivity.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						final ProgressDialog podcastDialog = new ProgressDialog(MainActivity.this);
						podcastDialog.setMessage("Podcast download");
						podcastDialog.setIndeterminate(false);
						podcastDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
						podcastDialog.setProgress(0);
						final DownloadFileRequest downloadFileRequest = new DownloadFileRequest(localFile, url, new ProgressListener() {
							@Override
							public void onProgressUpdate(long currentProgress, long maxProgress) {
								podcastDialog.setMax((int) maxProgress);
								podcastDialog.setProgress((int) currentProgress);
								Log.d("Podcast Download Progress", Long.toString(currentProgress) + "/" + maxProgress + "(" + currentProgress * 100 / maxProgress + ")");
							}
						});
						
						podcastDialog.setCancelable(true);
						podcastDialog.setOnCancelListener(new OnCancelListener() {
							@Override
							public void onCancel(DialogInterface dialog) {
								downloadFileRequest.cancel();
							}
						});
						podcastDialog.show();
						
						new Thread(new Runnable() {
							@Override
							public void run() {
								boolean downloadSuccess = manager.doRequest(downloadFileRequest).getResult();
								podcastDialog.dismiss();
								Log.d("Podcast Download", "Success: " + downloadSuccess);						
							}
						}).start();
					}
					
				});
				return null;
			}

		}.execute();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}   
}