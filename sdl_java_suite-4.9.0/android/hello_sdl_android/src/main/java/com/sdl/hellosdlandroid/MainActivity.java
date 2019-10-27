package com.sdl.hellosdlandroid;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.here.android.mpa.common.ApplicationContext;
import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.MapEngine;
import com.here.android.mpa.common.MatchedGeoPosition;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.guidance.NavigationManager;
import com.here.android.mpa.prefetcher.MapDataPrefetcher;

import java.io.File;
import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {
	private static final String TAG = "MainActivity";

	private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;

	private static final String[] RUNTIME_PERMISSIONS = {
			Manifest.permission.ACCESS_FINE_LOCATION,
			Manifest.permission.ACCESS_COARSE_LOCATION,
			Manifest.permission.WRITE_EXTERNAL_STORAGE,
			Manifest.permission.INTERNET,
			Manifest.permission.ACCESS_WIFI_STATE,
			Manifest.permission.ACCESS_NETWORK_STATE
	};
	private boolean fetchingDataInProgress = false;
	public static int currentSpeedLimitTransformed = 0;

	public static int getSpeedLimit() {
		return currentSpeedLimitTransformed;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		//If we are connected to a module we want to start our SdlService
		if(BuildConfig.TRANSPORT.equals("MULTI") || BuildConfig.TRANSPORT.equals("MULTI_HB")) {
			SdlReceiver.queryForConnectedService(this);
		}else if(BuildConfig.TRANSPORT.equals("TCP")) {
			Intent proxyIntent = new Intent(this, SdlService.class);
			startService(proxyIntent);
		}

//		if (hasPermissions(this, RUNTIME_PERMISSIONS)) {
//			initSDK();
//		} else {
//			ActivityCompat
//					.requestPermissions(this, RUNTIME_PERMISSIONS, REQUEST_CODE_ASK_PERMISSIONS);
//		}


	}

	private void initSDK() {
		// Set path of disk cache
		String diskCacheRoot = this.getFilesDir().getPath()
				+ File.separator + ".isolated-here-maps";
		// Retrieve intent name from manifest
		String intentName = "";
		try {
			ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(),
					PackageManager.GET_META_DATA);
			Bundle bundle = ai.metaData;
			intentName = bundle.getString("INTENT_NAME");
		} catch (PackageManager.NameNotFoundException e) {
			Log.e("MainActivity",
					"Failed to find intent name, NameNotFound: " + e.getMessage());
		}
		boolean success = com.here.android.mpa.common.MapSettings.setIsolatedDiskCacheRootPath(
				diskCacheRoot, intentName);
		if (!success) {
			Toast.makeText(this, "Operation 'setIsolatedDiskCacheRootPath' was not successful",
					Toast.LENGTH_SHORT).show();
			return;
		}

		ApplicationContext appContext = new ApplicationContext(this);
		MapEngine.getInstance().init(appContext, new OnEngineInitListener() {
			@Override
			public void onEngineInitializationCompleted(Error error) {
				if (error == Error.NONE) {

					startPositioningManager();
					startNavigationManager();
					activateSpeedLimitFragment();

				} else {
					Log.e("MainActivity", " init error: " + error + ", " + error.getDetails(),
							error.getThrowable());

					new AlertDialog.Builder(MainActivity.this).setMessage(
							"Error : " + error.name() + "\n\n" + error.getDetails())
							.setTitle(R.string.engine_init_error)
							.setNegativeButton(android.R.string.cancel,
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(
												DialogInterface dialog,
												int which) {
											MainActivity.this.finish();
										}
									}).create().show();
				}
			}
		});
	}

	private void startPositioningManager() {
		boolean positioningManagerStarted = PositioningManager.getInstance().start(PositioningManager.LocationMethod.GPS_NETWORK);

		if (!positioningManagerStarted) {
			//handle error here
		}
	}

	private void stopPositioningManager() {
		PositioningManager.getInstance().stop();
	}

	private void startNavigationManager() {
		NavigationManager.Error navError = NavigationManager.getInstance().startTracking();

		if (navError != NavigationManager.Error.NONE) {
			//handle error navError.toString());
		}

	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
										   @NonNull int[] grantResults) {
		switch (requestCode) {
			case REQUEST_CODE_ASK_PERMISSIONS: {
				for (int index = 0; index < permissions.length; index++) {
					if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {

						/*
						 * If the user turned down the permission request in the past and chose the
						 * Don't ask again option in the permission request system dialog.
						 */
						if (!ActivityCompat
								.shouldShowRequestPermissionRationale(this, permissions[index])) {
							Toast.makeText(this, "Required permission " + permissions[index]
											+ " not granted. "
											+ "Please go to settings and turn on for sample app",
									Toast.LENGTH_LONG).show();
						} else {
							Toast.makeText(this, "Required permission " + permissions[index]
									+ " not granted", Toast.LENGTH_LONG).show();
						}
					}
				}

				initSDK();
				break;
			}
			default:
				super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}
	}

	private static boolean hasPermissions(Context context, String... permissions) {
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissions != null) {
			for (String permission : permissions) {
				if (ActivityCompat.checkSelfPermission(context, permission)
						!= PackageManager.PERMISSION_GRANTED) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void activateSpeedLimitFragment() {
		startListeners();
	}

	private int meterPerSecToKmPerHour (double speed) {
		return (int) (speed * 3.6);
	}

	private int meterPerSecToMilesPerHour (double speed) {
		return (int) (speed * 2.23694);
	}

	public void startListeners() {
		PositioningManager.getInstance().addListener(new WeakReference<>(positionLister));
		MapDataPrefetcher.getInstance().addListener(prefetcherListener);
	}

	public void stopWatching() {
		PositioningManager.getInstance().removeListener(positionLister);
		MapDataPrefetcher.getInstance().removeListener(prefetcherListener);
	}

	MapDataPrefetcher.Adapter prefetcherListener = new MapDataPrefetcher.Adapter() {
		@Override
		public void onStatus(int requestId, PrefetchStatus status) {
			if(status != PrefetchStatus.PREFETCH_IN_PROGRESS) {
				fetchingDataInProgress = false;
			}
		}
	};

	PositioningManager.OnPositionChangedListener positionLister = new PositioningManager.OnPositionChangedListener() {
		@Override
		public void onPositionUpdated(PositioningManager.LocationMethod locationMethod,
									  GeoPosition geoPosition, boolean b) {

			if (PositioningManager.getInstance().getRoadElement() == null && !fetchingDataInProgress) {
				GeoBoundingBox areaAround = new GeoBoundingBox(geoPosition.getCoordinate(), 500, 500);
				MapDataPrefetcher.getInstance().fetchMapData(areaAround);
				fetchingDataInProgress = true;
			}

			if (geoPosition.isValid() && geoPosition instanceof MatchedGeoPosition) {

				MatchedGeoPosition mgp = (MatchedGeoPosition) geoPosition;

				currentSpeedLimitTransformed = 0;
				int currentSpeed = meterPerSecToKmPerHour(mgp.getSpeed());

				if (mgp.getRoadElement() != null) {
					double currentSpeedLimit = mgp.getRoadElement().getSpeedLimit();
					currentSpeedLimitTransformed  = meterPerSecToKmPerHour(currentSpeedLimit);
				}

			} else {
				//handle error
			}
		}

		@Override
		public void onPositionFixChanged(PositioningManager.LocationMethod locationMethod,
										 PositioningManager.LocationStatus locationStatus) {

		}
	};


}
