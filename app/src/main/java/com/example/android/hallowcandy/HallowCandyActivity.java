package com.example.android.hallowcandy;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;

import android.widget.GridView;
import android.widget.Toast;

import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationListener;


public class HallowCandyActivity extends Activity implements
		ConnectionCallbacks, OnConnectionFailedListener, LocationListener {

	protected static final String TAG = "HallowCandyActivity";


	/**
	 * The desired interval for location updates. Inexact. Updates may be more or less frequent.
	 */
	public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;

	/**
	 * The fastest rate for active location updates. Exact. Updates will never be more frequent
	 * than this value.
	 */
	public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
			UPDATE_INTERVAL_IN_MILLISECONDS / 2;

	// Keys for storing activity state in the Bundle.
	protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
	protected final static String LOCATION_KEY = "location-key";
	protected final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";

	/**
	 * Provides the entry point to Google Play services.
	 */
	protected GoogleApiClient mGoogleApiClient;

	/**
	 * Stores parameters for requests to the FusedLocationProviderApi.
	 */
	protected LocationRequest mLocationRequest;

	/**
	 * Represents a geographical location.
	 */
	protected Location mCurrentLocation;

	// UI Widgets.
//	protected Button mStartUpdatesButton;
//	protected Button mStopUpdatesButton;
//	protected TextView mLastUpdateTimeTextView;
//	protected TextView mLatitudeTextView;
//	protected TextView mLongitudeTextView;

	/**
	 * Tracks the status of the location updates request. Value changes when the user presses the
	 * Start Updates and Stop Updates buttons.
	 */
	protected Boolean mRequestingLocationUpdates;

	/**
	 * Time when the location was updated represented as a String.
	 */
	protected String mLastUpdateTime;

	protected JSONObject imageData;

	private static final int ACTION_TAKE_PHOTO_B = 1;

	//192.168.56.1 for genymotion

	public static final String ENDPOINT = "http://localhost:8080";
	private static final String BITMAP_STORAGE_KEY = "viewbitmap";
	private static final String IMAGEVIEW_VISIBILITY_STORAGE_KEY = "imageviewvisibility";
	private ImageView mImageView;
	private Bitmap mImageBitmap;

	private String mCurrentPhotoPath;

	private static final String JPEG_FILE_PREFIX = "IMG_";
	private static final String JPEG_FILE_SUFFIX = ".jpg";

	private AlbumStorageDirFactory mAlbumStorageDirFactory = null;


	/* Photo album for this application */
	private String getAlbumName() {
		return getString(R.string.album_name);
	}


	private File getAlbumDir() {
		File storageDir = null;

		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

			storageDir = mAlbumStorageDirFactory.getAlbumStorageDir(getAlbumName());

			if (storageDir != null) {
				if (! storageDir.mkdirs()) {
					if (! storageDir.exists()){
						Log.d("CameraSample", "failed to create directory");
						return null;
					}
				}
			}

		} else {
			Log.v(getString(R.string.app_name), "External storage is not mounted READ/WRITE.");
		}

		return storageDir;
	}

	private File createImageFile() throws IOException {
		// Create an image file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
		File albumF = getAlbumDir();
		File imageF = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, albumF);
		return imageF;
	}

	private File setUpPhotoFile() throws IOException {

		File f = createImageFile();
		mCurrentPhotoPath = f.getAbsolutePath();

		return f;
	}

//	private void setPic() {
//
//		/* There isn't enough memory to open up more than a couple camera photos */
//		/* So pre-scale the target bitmap into which the file is decoded */
//
//		/* Get the size of the ImageView */
////		end
//
//		/* Get the size of the image */
//		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
//		bmOptions.inJustDecodeBounds = true;
//		BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
//		int photoW = bmOptions.outWidth;
//		int photoH = bmOptions.outHeight;
//
//		/* Figure out which way needs to be reduced less */
//		int scaleFactor = 1;
//		if ((targetW > 0) || (targetH > 0)) {
//			scaleFactor = Math.min(photoW/targetW, photoH/targetH);
//		}
//
//		/* Set bitmap options to scale the image decode target */
//		bmOptions.inJustDecodeBounds = false;
//		bmOptions.inSampleSize = scaleFactor;
//		bmOptions.inPurgeable = true;
//
//		/* Decode the JPEG file into a Bitmap */
//		Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
//
//		/* Associate the Bitmap to the ImageView */
//		mImageView.setImageBitmap(bitmap);
//		mImageView.setVisibility(View.VISIBLE);
//	}

	private void galleryAddPic() {
		    Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
			File f = new File(mCurrentPhotoPath);
		    Uri contentUri = Uri.fromFile(f);
		    mediaScanIntent.setData(contentUri);
		    this.sendBroadcast(mediaScanIntent);

	}

	private void dispatchTakePictureIntent(int actionCode) {

		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

		switch(actionCode) {
		case ACTION_TAKE_PHOTO_B:
			File f = null;

			try {
				f = setUpPhotoFile();
				mCurrentPhotoPath = f.getAbsolutePath();
				takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
			} catch (IOException e) {
				e.printStackTrace();
				f = null;
				mCurrentPhotoPath = null;
			}
			break;

		default:
			break;
		} // switch

		startActivityForResult(takePictureIntent, actionCode);
	}

	private void handleBigCameraPhoto() {

		if (mCurrentPhotoPath != null) {
			//setPic();
			updateUI();
			uploadPic();
			galleryAddPic();
			mCurrentPhotoPath = null;
			getPic();
		}

	}

	private void getPic(){
		Ion.with(HallowCandyActivity.this)
				.load(ENDPOINT + "/getImages")
				.setMultipartParameter("lat", String.valueOf(mCurrentLocation.getLatitude()))
				.setMultipartParameter("lon", String.valueOf(mCurrentLocation.getLongitude()))
				.asString()
				.withResponse()
				.setCallback(new FutureCallback<Response<String>>() {
					@Override
					public void onCompleted(Exception e, Response<String> result) {
						Log.i(TAG, result.toString());
						try {
							imageData = new JSONObject(result.getResult());

							//LOAD IMAGE LIST
							//null list of strings for text descriptions for now
							final ArrayList<JSONObject> listdata = new ArrayList<JSONObject>();
							JSONArray jArray = imageData.getJSONArray("data");
							if (jArray != null) {
								for (int i=0;i<7;i++){
									listdata.add(jArray.getJSONObject(i));
								}
							}

							ImageList adapter = new ImageList(HallowCandyActivity.this, listdata);
							GridView list=(GridView)findViewById(R.id.list);
							list.setAdapter(adapter);
							list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

								@Override
								public void onItemClick(AdapterView<?> parent, View view,
														int position, long id) {
									Toast.makeText(HallowCandyActivity.this, "You Clicked at " + position, Toast.LENGTH_SHORT).show();

									try {
										findCandy(Double.valueOf(listdata.get(position).getString("lat")), Double.valueOf(listdata.get(position).getString("lon")));
									} catch (Exception e) {
										Toast.makeText(getApplicationContext(), "Can't find candy D:", Toast.LENGTH_SHORT).show();
									}
								}
							});

						}catch(JSONException e1){
							Toast.makeText(getApplicationContext(), "ERROR: getting images", Toast.LENGTH_SHORT).show();
						}
					}
				});
	}

	private void uploadPic() {
		// get the image file we've saved
		File f = new File(mCurrentPhotoPath);

		Future uploading = Ion.with(HallowCandyActivity.this)
				.load(ENDPOINT + "/upload")
				.setMultipartFile("image", f)
				.setMultipartParameter("lat", String.valueOf(mCurrentLocation.getLatitude()))
				.setMultipartParameter("lon", String.valueOf(mCurrentLocation.getLongitude()))
				.asString()
				.withResponse()

				.setCallback(new FutureCallback<Response<String>>() {
					@Override
					public void onCompleted(Exception e, Response<String> result) {
						try {
							JSONObject jobj = new JSONObject(result.getResult());
							Toast.makeText(getApplicationContext(), jobj.getString("response"), Toast.LENGTH_SHORT).show();

						} catch (JSONException e1) {
							e1.printStackTrace();
						}

					}
				});
	}

	Button.OnClickListener mTakePicOnClickListener = 
		new Button.OnClickListener() {
		@Override
		public void onClick(View v) {
			dispatchTakePictureIntent(ACTION_TAKE_PHOTO_B);
		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		//location stuff
	//	mLatitudeTextView = (TextView) findViewById((R.id.textLatitude));
		//mLongitudeTextView = (TextView) findViewById((R.id.textLongitude));

		mRequestingLocationUpdates = false;
		mLastUpdateTime = "";

		// Update values using data stored in the Bundle.
		updateValuesFromBundle(savedInstanceState);

		// Kick off the process of building a GoogleApiClient and requesting the LocationServices
		// API.
		buildGoogleApiClient();

		//picture stuff
	//	mImageView = (ImageView) findViewById(R.id.imagePreview);
		Ion.getDefault(this).configure().setLogging("Ion", Log.DEBUG);
		mImageBitmap = null;

		Button picBtn = (Button) findViewById(R.id.btnPicture);
		setBtnListenerOrDisable(
				picBtn,
				mTakePicOnClickListener,
				MediaStore.ACTION_IMAGE_CAPTURE
		);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
			mAlbumStorageDirFactory = new FroyoAlbumDirFactory();
		} else {
			mAlbumStorageDirFactory = new BaseAlbumDirFactory();
		}

	}

	/**
	 * Updates fields based on data stored in the bundle.
	 *
	 * @param savedInstanceState The activity state saved in the Bundle.
	 */
	private void updateValuesFromBundle(Bundle savedInstanceState) {
		Log.i(TAG, "Updating values from bundle");
		if (savedInstanceState != null) {
			// Update the value of mRequestingLocationUpdates from the Bundle, and make sure that
			// the Start Updates and Stop Updates buttons are correctly enabled or disabled.
			if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
				mRequestingLocationUpdates = savedInstanceState.getBoolean(
						REQUESTING_LOCATION_UPDATES_KEY);
			}

			// Update the value of mCurrentLocation from the Bundle and update the UI to show the
			// correct latitude and longitude.
			if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
				// Since LOCATION_KEY was found in the Bundle, we can be sure that mCurrentLocation
				// is not null.
				mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
			}

			// Update the value of mLastUpdateTime from the Bundle and update the UI.
			if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
				mLastUpdateTime = savedInstanceState.getString(LAST_UPDATED_TIME_STRING_KEY);
			}
			updateUI();
		}
	}

	/**
	 * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
	 * LocationServices API.
	 */
	protected synchronized void buildGoogleApiClient() {
		Log.i(TAG, "Building GoogleApiClient");
		mGoogleApiClient = new GoogleApiClient.Builder(this)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(LocationServices.API)
				.build();
		createLocationRequest();
	}


	/**
	 * Sets up the location request. Android has two location request settings:
	 * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
	 * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
	 * the AndroidManifest.xml.
	 * <p/>
	 * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
	 * interval (5 seconds), the Fused Location Provider API returns location updates that are
	 * accurate to within a few feet.
	 * <p/>
	 * These settings are appropriate for mapping applications that show real-time location
	 * updates.
	 */
	protected void createLocationRequest() {
		mLocationRequest = new LocationRequest();

		// Sets the desired interval for active location updates. This interval is
		// inexact. You may not receive updates at all if no location sources are available, or
		// you may receive them slower than requested. You may also receive updates faster than
		// requested if other applications are requesting location at a faster interval.
		mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

		// Sets the fastest rate for active location updates. This interval is exact, and your
		// application will never receive updates faster than this value.
		mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
	}

//	/**
//	 * Handles the Start Updates button and requests start of location updates. Does nothing if
//	 * updates have already been requested.
//	 */
//	public void startUpdatesButtonHandler() {
//		if (!mRequestingLocationUpdates) {
//			mRequestingLocationUpdates = true;
//			startLocationUpdates();
//		}
//	}
//
//	/**
//	 * Handles the Stop Updates button, and requests removal of location updates. Does nothing if
//	 * updates were not previously requested.
//	 */
//	public void stopUpdatesButtonHandler() {
//		if (mRequestingLocationUpdates) {
//			mRequestingLocationUpdates = false;
//			stopLocationUpdates();
//		}
//	}

	/**
	 * Requests location updates from the FusedLocationApi.
	 */
	protected void startLocationUpdates() {
		// The final argument to {@code requestLocationUpdates()} is a LocationListener
		// (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
		LocationServices.FusedLocationApi.requestLocationUpdates(
				mGoogleApiClient, mLocationRequest, this);
	}


	/**
	 * Updates the latitude, the longitude, and the last location time in the UI.
	 * 42.360020, -71.094278 test mit data
	 */
	private void updateUI() {
		//navigation
		//Uri gmmIntentUri = Uri.parse("google.navigation:q=" + 42.360020 + "," + -71.094278 + "&mode=w");
		//findCandy(42.360020, -71.094278);
//		mLatitudeTextView.setText(String.format("%s: %f", "Latitude:",
//				mCurrentLocation.getLatitude()));
//		mLongitudeTextView.setText(String.format("%s: %f", "Longitude:",
//				mCurrentLocation.getLongitude()));
	}

	private void findCandy(double lat, double lon) {
		Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + lat + "," + lon + "(" + "Candy" + ")");
		Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
		mapIntent.setPackage("com.google.android.apps.maps");
		startActivity(mapIntent);
	}

	/**
	 * Removes location updates from the FusedLocationApi.
	 */
	protected void stopLocationUpdates() {
		// It is a good practice to remove location requests when the activity is in a paused or
		// stopped state. Doing so helps battery performance and is especially
		// recommended in applications that request frequent location updates.

		// The final argument to {@code requestLocationUpdates()} is a LocationListener
		// (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
		LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
	}


	@Override
	protected void onStart() {
		super.onStart();
		mGoogleApiClient.connect();
	}


	@Override
	public void onResume() {
		super.onResume();
		// Within {@code onPause()}, we pause location updates, but leave the
		// connection to GoogleApiClient intact.  Here, we resume receiving
		// location updates if the user has requested them.

		if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
			startLocationUpdates();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		// Stop location updates to save battery, but don't disconnect the GoogleApiClient object.
		if (mGoogleApiClient.isConnected()) {
			stopLocationUpdates();
		}
	}

	@Override
	protected void onStop() {
		// Stop location updates
		if (mGoogleApiClient.isConnected()) {
			stopLocationUpdates();
		}

		mGoogleApiClient.disconnect();

		super.onStop();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case ACTION_TAKE_PHOTO_B: {
			if (resultCode == RESULT_OK) {
				handleBigCameraPhoto();
			}
			break;
		} // ACTION_TAKE_PHOTO_B
		} // switch
	}

	// Some lifecycle callbacks so that the image can survive orientation change
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putParcelable(BITMAP_STORAGE_KEY, mImageBitmap);
		outState.putBoolean(IMAGEVIEW_VISIBILITY_STORAGE_KEY, (mImageBitmap != null));

		//save location details
		outState.putParcelable(LOCATION_KEY, mCurrentLocation);

		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mImageBitmap = savedInstanceState.getParcelable(BITMAP_STORAGE_KEY);
//		mImageView.setImageBitmap(mImageBitmap);
//		mImageView.setVisibility(
//				savedInstanceState.getBoolean(IMAGEVIEW_VISIBILITY_STORAGE_KEY) ?
//						ImageView.VISIBLE : ImageView.INVISIBLE
//		);

		mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
	}

	/**
	 * Indicates whether the specified action can be used as an intent. This
	 * method queries the package manager for installed packages that can
	 * respond to an intent with the specified action. If no suitable package is
	 * found, this method returns false.
	 * http://android-developers.blogspot.com/2009/01/can-i-use-this-intent.html
	 *
	 * @param context The application's environment.
	 * @param action The Intent action to check for availability.
	 *
	 * @return True if an Intent with the specified action can be sent and
	 *         responded to, false otherwise.
	 */
	public static boolean isIntentAvailable(Context context, String action) {
		final PackageManager packageManager = context.getPackageManager();
		final Intent intent = new Intent(action);
		List<ResolveInfo> list =
			packageManager.queryIntentActivities(intent,
					PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}

	private void setBtnListenerOrDisable( 
			Button btn, 
			Button.OnClickListener onClickListener,
			String intentName
	) {
		if (isIntentAvailable(this, intentName)) {
			btn.setOnClickListener(onClickListener);        	
		} else {
			btn.setText( 
				getText(R.string.cannot).toString() + " " + btn.getText());
			btn.setClickable(false);
		}
	}

	/**
	 * Runs when a GoogleApiClient object successfully connects.
	 */
	@Override
	public void onConnected(Bundle connectionHint) {
		Log.i(TAG, "Connected to GoogleApiClient");

		// If the initial location was never previously requested, we use
		// FusedLocationApi.getLastLocation() to get it. If it was previously requested, we store
		// its value in the Bundle and check for it in onCreate(). We
		// do not request it again unless the user specifically requests location updates by pressing
		// the Start Updates button.
		//
		// Because we cache the value of the initial location in the Bundle, it means that if the
		// user launches the activity,
		// moves to a new location, and then changes the device orientation, the original location
		// is displayed as the activity is re-created.
		if (mCurrentLocation == null) {
			mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
			mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
			updateUI();
		}

		// If the user presses the Start Updates button before GoogleApiClient connects, we set
		// mRequestingLocationUpdates to true (see startUpdatesButtonHandler()). Here, we check
		// the value of mRequestingLocationUpdates and if it is true, we start location updates.
		mRequestingLocationUpdates = true;
		if (mRequestingLocationUpdates) {
			startLocationUpdates();
		}
	}

	/**
	 * Callback that fires when the location changes.
	 */
	@Override
	public void onLocationChanged(Location location) {
		mCurrentLocation = location;
		mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
		updateUI();
		Toast.makeText(this, "location updated!! :D",
				Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onConnectionSuspended(int cause) {
		// The connection to Google Play services was lost for some reason. We call connect() to
		// attempt to re-establish the connection.
		Log.i(TAG, "Connection suspended");
		mGoogleApiClient.connect();
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		// Refer to the javadoc for ConnectionResult to see what error codes might be returned in
		// onConnectionFailed.
		Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
	}

}