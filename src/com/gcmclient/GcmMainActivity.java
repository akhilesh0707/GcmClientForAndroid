package com.gcmclient;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;



import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

public class GcmMainActivity extends Activity 
{
	static final String TAG = "com.darwinsys.gcmdemo";
	private static final String TAG_CONFORMANCE = "RegulatoryCompliance";

	private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
	private static final String GCM_SENDER_ID = "117558675814";
	public static final String EXTRA_MESSAGE = "message";
	public static final String PROPERTY_REG_ID = "registration_id";
	private static final String PROPERTY_APP_VERSION = "appVersion";

	private Context context;
	private TextView logTv;
	private Executor threadPool = Executors.newFixedThreadPool(1);
	private GoogleCloudMessaging gcm;
	private String registrationId;

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		Log.d(TAG, "MainActivity.onCreate()");
		setContentView(R.layout.activity_main);
		logTv = (TextView) findViewById(R.id.logView);
		context = getApplicationContext();

		logTv.setText("Start\n");

		// GCM stuff
		if (checkForGcm()) {
			logTv.append("Passed checkforGCM\n");
			gcm = GoogleCloudMessaging.getInstance(this);
			registrationId = getRegistrationId(this);
			if (registrationId.isEmpty()) {
				threadPool.execute(new Runnable() 
				{
					public void run() 
					{
						final String regn = registerWithGcm();
						Log.d(TAG, "New Registration = " + regn);
						setMessageOnUiThread(regn + "\n");
					}
				});
			} 
			else {
				final String mesg = "Previous Registration =\n" + registrationId;
				Log.d(TAG, mesg);
				logTv.append(mesg + "\n");
			}
		} else {
			logTv.append("Failed checkforGCM");
		}
	}

	void setMessageOnUiThread(final String mesg) {
		runOnUiThread(new Runnable() {
			public void run() {
				logTv.append(mesg);
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "MainActivity.onResume()");
		checkForGcm();
	}

	boolean checkForGcm() {
		int ret = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if (ConnectionResult.SUCCESS == ret) {
			Log.d(TAG, "MainActivity.checkForGcm(): SUCCESS");
			return true;
		} else {
			Log.d(TAG, "MainActivity.checkForGcm(): FAILURE");
			if (GooglePlayServicesUtil.isUserRecoverableError(ret)) {
				GooglePlayServicesUtil.getErrorDialog(ret, this,
						PLAY_SERVICES_RESOLUTION_REQUEST).show();
			} else {
				Toast.makeText(this,
						"Google Message Not Supported on this device; you will not get update notifications!!",
						Toast.LENGTH_LONG).show();
				Log.d(TAG_CONFORMANCE, "User accepted to run the app without update notifications!");
			}
			return false;
		}
	}

	private String getRegistrationId(Context context) {
		final SharedPreferences prefs = getSharedPreferences(GcmMainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
		String registrationId = prefs.getString(PROPERTY_REG_ID, "");
		if (registrationId.isEmpty()) {
			Log.i(TAG, "Registration not found.");
			return "";
		}
		int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
		int currentVersion = getAppVersion(context);
		if (registeredVersion != currentVersion) {
			Log.i(TAG, "App version changed.");
			return "";
		}
		return registrationId;
	}

	private static int getAppVersion(Context context) {
		try {
			return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e) {
			// This is a "CANTHAPPEN", so why is it a checked exception?
			final String mesg = "CANTHAPPEN: could not get my own package name!?!?: " + e;
			Log.wtf(TAG, mesg);
			throw new RuntimeException(mesg);
		}
	}

	private String registerWithGcm() {
		String mesg = "";
		try {
			if (gcm == null) {
				gcm = GoogleCloudMessaging.getInstance(context);
			}
			registrationId = gcm.register(GCM_SENDER_ID);
			mesg = "Device registered! My registration =/n" + registrationId;

			sendRegistrationIdToMyServer();

			storeRegistrationId(context, registrationId);
		} catch (IOException ex) {
			mesg = "Error :" + ex.getMessage();
			Toast.makeText(context, mesg, Toast.LENGTH_LONG).show();
			throw new RuntimeException(mesg);
		}
		return mesg;

	}

	private void sendRegistrationIdToMyServer()
	{
			
	}

	private void storeRegistrationId(Context context, String regId) {
		final SharedPreferences prefs = getSharedPreferences(GcmMainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
		int appVersion = getAppVersion(context);
		Log.i(TAG, "Saving regId on app version " + appVersion);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(PROPERTY_REG_ID, regId);
		editor.putInt(PROPERTY_APP_VERSION, appVersion);
		editor.commit();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
