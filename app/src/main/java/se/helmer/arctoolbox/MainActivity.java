package se.helmer.arctoolbox;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    public static String TAG = "arctoolbox";
    private static final long LOCATION_REFRESH_TIME = 5000;
    private static final long LOCATION_REFRESH_DISTANCE = 10;
    SharedPreferences sharedpreferences;

    private LocationManager mLocationManager;

    EditText editTextMMSI;
    EditText editTextEmail;
    TextView textViewLatitude;
    TextView textViewLongitude;
    TextView textViewSpeed;
    TextView textViewBearing;
    TextView textStatus;
    TextView textUtcTime;

    private String now() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm"); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        String nowAsISO = df.format(new Date());
        return nowAsISO;
    }

    private String nowISO8601() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        String nowAsISO = df.format(new Date());
        return nowAsISO;
    }

    private String getEmailBody() {
        final String result = "MMSI=" + editTextMMSI.getText() + "\n" +
                "LAT="+textViewLatitude.getText() +"\n" +
                "LONG="+textViewLongitude.getText() + "\n" +
                "SPEED="+textViewSpeed.getText() + "\n" +
                "COURSE="+textViewBearing.getText() + "\n" +
                "TIMESTAMP="+textUtcTime.getText() + "\n";
        Log.w(TAG,result);
        return result;
    }

    private void composeEmail(String address, String subject) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        String[] to = new String[] { address };
        intent.putExtra(Intent.EXTRA_EMAIL, to);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, getEmailBody());
        startActivity(Intent.createChooser(intent, "Select a Email client"));
    }


    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            Log.w(TAG, location.toString());
            saveSharedPreferences();
            textStatus.setText("Status: Updated GPS location at " + nowISO8601());

            DecimalFormat longlatFormat = new DecimalFormat("#.#####",
                    DecimalFormatSymbols.getInstance(Locale.US));
            DecimalFormat speedFormat = new DecimalFormat("#.#",
                    DecimalFormatSymbols.getInstance(Locale.US));
            DecimalFormat courseFormat = new DecimalFormat("#",
                    DecimalFormatSymbols.getInstance(Locale.US));
            textViewLatitude.setText(longlatFormat.format(location.getLatitude()));
            textViewLongitude.setText(longlatFormat.format(location.getLongitude()));

            String speed = location.hasSpeed() ? speedFormat.format(location.getSpeed()) : "0.0";
            String bearing = location.hasBearing() ? courseFormat.format(location.getBearing()) : "0";
            textViewSpeed.setText(speed);
            textViewBearing.setText(bearing);
            textUtcTime.setText(now());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textStatus = (TextView) findViewById(R.id.tvStatus);
        textStatus.setText("Status: N/A");

        sharedpreferences = getSharedPreferences("mypref",
                Context.MODE_PRIVATE);

        editTextMMSI = findViewById(R.id.editMssi);
        editTextEmail = findViewById(R.id.editEmail);

        if (sharedpreferences.contains("mmsi")) {
            editTextMMSI.setText(sharedpreferences.getString("mmsi", ""));
        }

        if (sharedpreferences.contains("email")) {
            editTextEmail.setText(sharedpreferences.getString("email", "report@marinetraffic.com"));
        }

        textViewLatitude = (TextView) findViewById(R.id.tvLatitude);
        textViewLongitude = (TextView) findViewById(R.id.tvLongitude);
        textViewSpeed = (TextView) findViewById(R.id.tvSpeed);
        textViewBearing = (TextView) findViewById(R.id.tvBearing);
        textUtcTime =  (TextView) findViewById(R.id.tvUtctime);

        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        ActivityResultLauncher<String[]> locationPermissionRequest =
                registerForActivityResult(new ActivityResultContracts
                                .RequestMultiplePermissions(), result -> {
                            Boolean fineLocationGranted = result.getOrDefault(
                                    Manifest.permission.ACCESS_FINE_LOCATION, false);
                            Boolean coarseLocationGranted = result.getOrDefault(
                                    Manifest.permission.ACCESS_COARSE_LOCATION,false);
                            if (fineLocationGranted != null && fineLocationGranted) {
                                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                                        ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                    Toast toast = Toast.makeText(getApplicationContext(),
                                            "No permission to access GPS location (check settings) ",
                                            Toast.LENGTH_SHORT);
                                    toast.show();
                                } else {
                                    textStatus.setText("Status: Querying GPS location...");
                                    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_TIME,
                                            LOCATION_REFRESH_DISTANCE, mLocationListener);
                                }
                            } else if (coarseLocationGranted != null && coarseLocationGranted) {
                                Toast toast = Toast.makeText(getApplicationContext(),
                                        "Only approximate location access granted  (check settings) ",
                                        Toast.LENGTH_SHORT);
                                toast.show();
                                // Only approximate location access granted.
                            } else {
                                Toast toast = Toast.makeText(getApplicationContext(),
                                        "No location access granted  (check settings) ",
                                        Toast.LENGTH_SHORT);
                                toast.show();
                            }
                        }
                );

        final Button button = findViewById(R.id.btnGetLocation);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                locationPermissionRequest.launch(new String[] {
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                });
            }
        });

        final Button emailButton = findViewById(R.id.btnEmail);
        emailButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    textStatus.setText("Status: Trying to send e-mail ...");
                    saveSharedPreferences();
                    composeEmail(editTextEmail.getText().toString(), "Position report");
                    textStatus.setText("Status: Done");
                } catch (ActivityNotFoundException ex) {
                    Toast.makeText(MainActivity.this, "There are no email clients installed.",Toast.LENGTH_SHORT).show();
                }
            }
        });




    }

    private void saveSharedPreferences() {
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString("mmsi", editTextMMSI.getText().toString() );
        editor.putString("email", editTextEmail.getText().toString() );
        editor.commit();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
