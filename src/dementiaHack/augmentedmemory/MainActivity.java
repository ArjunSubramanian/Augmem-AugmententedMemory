package dementiaHack.augmentedmemory;


import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.w3c.dom.Text;

import com.qualcomm.snapdragon.sdk.face.FacialProcessing;
import com.qualcomm.snapdragon.sdk.face.FacialProcessing.FEATURE_LIST;
import com.qualcomm.snapdragon.sdk.face.FacialProcessing.FP_MODES;



import android.support.v7.app.ActionBarActivity;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;



public class MainActivity extends ActionBarActivity implements Camera.PreviewCallback,LocationListener {
	
	private GridView gridView;
	private Camera cameraObj; // Accessing the Android native Camera.
	private FrameLayout preview; // Layout on which camera surface is displayed
	private CameraSurfacePreview mPreview;
	public static FacialProcessing faceObj;
	public final String TAG = "FacialRecognitionActivity";
	private ImageView cameraButton;
	private int FRONT_CAMERA_INDEX = 1;
	private int BACK_CAMERA_INDEX = 0;
	private int lastAngle = 0;
	private int rotationAngle = 0;
	public final int confidence_value = 58;
	public static boolean activityStartedOnce = false;
	public static final String ALBUM_NAME = "serialize_deserialize";
	public static final String HASH_NAME = "HashMap";
	private OrientationEventListener orientationListener; 
	 LocationManager locationManager;
	HashMap<String, String> hash;
	String provider;
	private static boolean cameraFacingFront = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();
        SimpleDateFormat s = new SimpleDateFormat("dd");
        String format = s.format(new Date());
        Calendar cal=Calendar.getInstance();
        SimpleDateFormat day = new SimpleDateFormat("dd");
        SimpleDateFormat month_date = new SimpleDateFormat("MMMM");
        SimpleDateFormat Year = new SimpleDateFormat("yyyy");
        String month_name = month_date.format(cal.getTime());
        String Day = day.format(cal.getTime());
        String year = Year.format(cal.getTime());
        Button time = (Button) findViewById(R.id.Time);
        time.setText(Day+" "+month_name.toLowerCase().substring(0, 3) +", "+year);
        hash = retrieveHash(getApplicationContext()); // Retrieve the previously
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		// saved Hash Map.
        Criteria criteria = new Criteria();


        //provider = locationManager.getBestProvider(criteria, false);
        provider = locationManager.NETWORK_PROVIDER;

        Location location = locationManager.getLastKnownLocation(provider);

        if (location != null) {
            System.out.println("Provider " + provider + " has been selected");
            onLocationChanged(location);
        } else {
            Log.d("MainActivity", " Location not available");
            Toast.makeText(this, "Location not available",
                    Toast.LENGTH_SHORT).show();
        }

        orientationListener = new OrientationEventListener(this) {
			@Override
			public void onOrientationChanged(int orientation) {
				
			}
		};
if (!activityStartedOnce) // Check to make sure FacialProcessing object
// is not created multiple times.
{
activityStartedOnce = true;
// Check if Facial Recognition feature is supported in the device
boolean isSupported = FacialProcessing
.isFeatureSupported(FEATURE_LIST.FEATURE_FACIAL_RECOGNITION);
if (isSupported) {
Log.d(TAG, "Feature Facial Recognition is supported");
faceObj = (FacialProcessing) FacialProcessing.getInstance();
loadAlbum(); // De-serialize a previously stored album.
if (faceObj != null) {
faceObj.setRecognitionConfidence(confidence_value);
faceObj.setProcessingMode(FP_MODES.FP_MODE_STILL);
}
} else // If Facial recognition feature is not supported then
// display an alert box.
{
Log.e(TAG, "Feature Facial Recognition is NOT supported");
new AlertDialog.Builder(this)
.setMessage(
"Your device does NOT support Qualcomm's Facial Recognition feature. ")
.setCancelable(false)
.setNegativeButton("OK",
new DialogInterface.OnClickListener() {
public void onClick(DialogInterface dialog,
int id) {
MainActivity.this.finish();
}
}).show();
}
}
// Vibrator for button press
final Vibrator vibrate = (Vibrator) MainActivity.this
.getSystemService(Context.VIBRATOR_SERVICE);

        
        Button addFriend = (Button)findViewById(R.id.button1);
        addFriend.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				stopCamera();
				addNewPerson();
			}
		});
        
        Button LiveRecogintion = (Button) findViewById(R.id.button2) ;
        LiveRecogintion.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				stopCamera();
				liveRecognition();
			}
		});
        startCamera();
    }
    
    @Override
	protected void onStop() {
    	stopCamera();
		super.onStop();
	}
    
	protected void onResume() {
		super.onResume();
		if (cameraObj != null) {
			stopCamera();
		}
		startCamera();
		  locationManager.requestLocationUpdates(provider, 400, 1, this);
	}
	 @Override
	    protected void onPause() {
	        super.onPause();
	        stopCamera();
	        locationManager.removeUpdates(this);
	    }
	private void identifyPerson() {
		Intent intent = new Intent(this, AddPhoto.class);
		intent.putExtra("Username", "Not Identified");
		intent.putExtra("PersonId", -1);
		intent.putExtra("UpdatePerson", false);
		intent.putExtra("IdentifyPerson", true);
		startActivity(intent);
	}
	private void liveRecognition() {
		Intent intent = new Intent(this, LiveRecognition.class);
		startActivity(intent);
	}
	@Override
	public void onBackPressed() { // Destroy the activity to avoid stacking of
									// android activities
		super.onBackPressed();
		//FacialRecognitionActivity.this.finishAffinity();
		activityStartedOnce = false;
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
    
    private void addNewPerson() {
		Intent intent = new Intent(this, AddPhoto.class);
		intent.putExtra("Username", "null");
		intent.putExtra("PersonId", -1);
		intent.putExtra("UpdatePerson", false);
		intent.putExtra("IdentifyPerson", false);
		startActivity(intent);
	}
    
    protected HashMap<String, String> retrieveHash(Context context) {
		SharedPreferences settings = context.getSharedPreferences(HASH_NAME, 0);
		HashMap<String, String> hash = new HashMap<String, String>();
		hash.putAll((Map<? extends String, ? extends String>) settings.getAll());
		return hash;
	}
    
	protected void saveHash(HashMap<String, String> hashMap, Context context) {
		SharedPreferences settings = context.getSharedPreferences(HASH_NAME, 0);
		
		SharedPreferences.Editor editor = settings.edit();
		editor.clear();
		Log.e(TAG, "Hash Save Size = " + hashMap.size());
		for (String s : hashMap.keySet()) {
			editor.putString(s, hashMap.get(s));
		}
		editor.commit();
	}
	
	/*
	 * Function to retrieve the byte array from the Shared Preferences.
	 */
	public void loadAlbum() {
		SharedPreferences settings = getSharedPreferences(ALBUM_NAME, 0);
		String arrayOfString = settings.getString("albumArray", null);
		
		byte[] albumArray = null;
		if (arrayOfString != null) {
			String[] splitStringArray = arrayOfString.substring(1,
					arrayOfString.length() - 1).split(", ");
			
			albumArray = new byte[splitStringArray.length];
			for (int i = 0; i < splitStringArray.length; i++) {
				albumArray[i] = Byte.parseByte(splitStringArray[i]);
			}
			faceObj.deserializeRecognitionAlbum(albumArray);
			Log.e("TAG", "De-Serialized my album");
		}
	}
	
	/*
	 * Method to save the recognition album to a permanent device memory
	 */
	public void saveAlbum() {
		byte[] albumBuffer = faceObj.serializeRecogntionAlbum();
		SharedPreferences settings = getSharedPreferences(ALBUM_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("albumArray", Arrays.toString(albumBuffer));
		editor.commit();
	}
private void stopCamera() {
		
		if (cameraObj != null) {
			cameraObj.stopPreview();
			cameraObj.setPreviewCallback(null);
			preview.removeView(mPreview);
			cameraObj.release();
		}
		cameraObj = null;
	}
	
	/*
	 * Method that handles initialization and starting of camera.
	 */
	@SuppressLint("NewApi")
	private void startCamera() {
		if (cameraFacingFront) {
			cameraObj = Camera.open(FRONT_CAMERA_INDEX); // Open the Front
															// camera
		} else {
			cameraObj = Camera.open(BACK_CAMERA_INDEX); // Open the back camera
		}
		mPreview = new CameraSurfacePreview(MainActivity.this, cameraObj,
				orientationListener); // Create a new surface on which Camera
										// will be displayed.
		preview = (FrameLayout) findViewById(R.id.cameraview);
		preview.addView(mPreview);
		cameraObj.setPreviewCallback(MainActivity.this);
	}
	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
		 double  lat= location.getLatitude();
	        double lng= location.getLongitude();

	        Geocoder geoCoder= new Geocoder(this, Locale.getDefault());
	        StringBuilder builder= new StringBuilder();
	        try{
	            List<Address> address=geoCoder.getFromLocation(lat,lng,1);
	            int maxLines=address.get(0).getMaxAddressLineIndex();
	            for(int i=0;i<maxLines;i++){
	                String addressString=address.get(0).getAddressLine(i);
	                builder.append(addressString);
	                builder.append(" ");
	            }
	            String finalAddress= builder.toString();
	            TextView txt = (TextView)findViewById(R.id.Location);
	            txt.setText(finalAddress);
	          //  Toast.makeText(this, finalAddress,Toast.LENGTH_LONG).show();
	        }
	        catch (IOException e){}
	        catch (NullPointerException e){}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}
}
