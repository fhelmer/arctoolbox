package se.helmer.arctoolbox;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String PREF_NAME = "mypref";
    private static final String KEY_MMSI = "mmsi";
    private static final String KEY_EMAIL = "email";
    private static final long LOCATION_REFRESH_TIME = 5000;
    private static final long LOCATION_REFRESH_DISTANCE = 10;

    private SharedPreferences sharedPreferences;
    private LocationManager mLocationManager;
    private boolean isListeningForLocation = false;

    private EditText editTextMMSI;
    private EditText editTextEmail;
    private TextView textViewLatitude;
    private TextView textViewLongitude;
    private TextView textViewSpeed;
    private TextView textViewBearing;
    private TextView textStatus;
    private TextView textUtcTime;

    private ActivityResultLauncher<String[]> locationPermissionRequest;

    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull final Location location) {
            Log.d(TAG, "Location received: " + location);
            saveSharedPreferences();

            String isoNow = PositionReportHelper.formatIso8601(new Date());
            textStatus.setText(getString(R.string.status_format, getString(R.string.status_updated_gps, isoNow)));

            textViewLatitude.setText(PositionReportHelper.formatCoordinate(location.getLatitude()));
            textViewLongitude.setText(PositionReportHelper.formatCoordinate(location.getLongitude()));

            String speed = location.hasSpeed()
                    ? PositionReportHelper.formatSpeedKnots(PositionReportHelper.toKnots(location.getSpeed()))
                    : "0.0";
            String bearing = location.hasBearing()
                    ? PositionReportHelper.formatCourse(location.getBearing())
                    : "0";

            textViewSpeed.setText(speed);
            textViewBearing.setText(bearing);
            textUtcTime.setText(PositionReportHelper.formatUtcTime(new Date()));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootScrollView), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });

        textStatus = findViewById(R.id.tvStatus);
        textStatus.setText(getString(R.string.status_format, getString(R.string.status_na)));

        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        editTextMMSI = findViewById(R.id.editMmsi);
        editTextEmail = findViewById(R.id.editEmail);

        if (sharedPreferences.contains(KEY_MMSI)) {
            editTextMMSI.setText(sharedPreferences.getString(KEY_MMSI, ""));
        }

        if (sharedPreferences.contains(KEY_EMAIL)) {
            editTextEmail.setText(sharedPreferences.getString(KEY_EMAIL, getString(R.string.default_email)));
        }

        textViewLatitude = findViewById(R.id.tvLatitude);
        textViewLongitude = findViewById(R.id.tvLongitude);
        textViewSpeed = findViewById(R.id.tvSpeed);
        textViewBearing = findViewById(R.id.tvBearing);
        textUtcTime = findViewById(R.id.tvUtctime);

        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        locationPermissionRequest = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean fineGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                    if (fineGranted != null && fineGranted) {
                        startLocationUpdates();
                    } else if (coarseGranted != null && coarseGranted) {
                        Toast.makeText(getApplicationContext(), R.string.toast_approximate_only, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.toast_no_permission, Toast.LENGTH_SHORT).show();
                    }
                }
        );

        requestLocation();

        final Button emailButton = findViewById(R.id.btnEmail);
        emailButton.setOnClickListener(v -> onSendEmailClicked());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
    }

    private void requestLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            locationPermissionRequest.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void startLocationUpdates() {
        if (mLocationManager == null) {
            return;
        }

        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            textStatus.setText(getString(R.string.status_format, getString(R.string.status_gps_disabled)));
            Toast.makeText(this, R.string.status_gps_disabled, Toast.LENGTH_LONG).show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, R.string.toast_no_permission, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isListeningForLocation) {
            try {
                mLocationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        LOCATION_REFRESH_TIME,
                        LOCATION_REFRESH_DISTANCE,
                        mLocationListener
                );
                isListeningForLocation = true;
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException requesting location updates", e);
                Toast.makeText(this, R.string.toast_no_permission, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        textStatus.setText(getString(R.string.status_format, getString(R.string.status_querying_gps)));
    }

    private void stopLocationUpdates() {
        if (mLocationManager != null && isListeningForLocation) {
            mLocationManager.removeUpdates(mLocationListener);
            isListeningForLocation = false;
        }
    }

    private void onSendEmailClicked() {
        String mmsi = editTextMMSI.getText().toString().trim();
        if (mmsi.isEmpty()) {
            Toast.makeText(this, R.string.toast_mmsi_required, Toast.LENGTH_SHORT).show();
            editTextMMSI.requestFocus();
            return;
        }

        if (!PositionReportHelper.isValidMmsi(mmsi)) {
            Toast.makeText(this, R.string.toast_mmsi_invalid, Toast.LENGTH_SHORT).show();
            editTextMMSI.requestFocus();
            return;
        }

        String lat = textViewLatitude.getText().toString();
        String lon = textViewLongitude.getText().toString();
        if (!PositionReportHelper.hasValidFix(lat, lon)) {
            Toast.makeText(this, R.string.toast_no_fix_yet, Toast.LENGTH_LONG).show();
            return;
        }

        saveSharedPreferences();

        String toAddress = editTextEmail.getText().toString().trim();
        if (toAddress.isEmpty()) {
            toAddress = getString(R.string.default_email);
        }

        String emailBody = PositionReportHelper.buildReport(
                mmsi,
                lat,
                lon,
                textViewSpeed.getText().toString(),
                textViewBearing.getText().toString(),
                textUtcTime.getText().toString()
        );

        composeEmail(toAddress, getString(R.string.email_subject), emailBody);
    }

    private void composeEmail(String address, String subject, String body) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + Uri.encode(address)));
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body);

        try {
            textStatus.setText(getString(R.string.status_format, getString(R.string.status_sending_email)));
            startActivity(Intent.createChooser(intent, getString(R.string.email_chooser_title)));
            textStatus.setText(getString(R.string.status_format, getString(R.string.status_done)));
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(MainActivity.this, R.string.toast_no_email_client, Toast.LENGTH_SHORT).show();
        }
    }

    private void saveSharedPreferences() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_MMSI, editTextMMSI.getText().toString().trim());
        editor.putString(KEY_EMAIL, editTextEmail.getText().toString().trim());
        editor.apply();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }
}
