package com.example.iot;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.TextView;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements LocationListener {

    TextView locationView, accelerationView, rotationView;

    protected LocationManager locationManager;
    SensorManager sm = null;

    List list;

    long locationLastUpdate, locationNowUpdate;

    long accnLastUpdate, accnNowUpdate;

    long gyroLastUpdate, gyroNowUpdate;

    float oldAccVal = 0.0f;
    float[] gravity = new float[3];
    float[] linear_acceleration = new float[3];

    float[] oldOrientations = new float[3];

    double latitude, longitude;

    String num = "7999850446";
    String msg = "Help me I have been in a car accident!! My location : http://maps.google.com/?q=";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        askPermissionsAndLocation();

        setUpAccelerometer();

        setUpGyroscope();
    }

    public void setUpAccelerometer(){
        list = sm.getSensorList(Sensor.TYPE_ACCELEROMETER);
        if(list.size()>0){
            sm.registerListener(accSensor, (Sensor) list.get(0), SensorManager.SENSOR_DELAY_NORMAL);
        }else{
            accelerationView = findViewById(R.id.acceleration);
            accelerationView.setText("No acceleration sensor");
        }
    }

    public void setUpGyroscope(){
        list = sm.getSensorList(Sensor.TYPE_ROTATION_VECTOR );
        if(list.size()>0){
            sm.registerListener(rotSensor, (Sensor) list.get(0), SensorManager.SENSOR_DELAY_NORMAL);
        }else{
            accelerationView = findViewById(R.id.rotation);
            accelerationView.setText("No rotation sensor");
        }
    }

    public void init(){
        accnLastUpdate = Calendar.getInstance().getTimeInMillis();
        gyroLastUpdate = Calendar.getInstance().getTimeInMillis();
        locationLastUpdate = Calendar.getInstance().getTimeInMillis();
        sm = (SensorManager)getSystemService(SENSOR_SERVICE);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    public void askPermissionsAndLocation(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, 101 );
        }

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[] { Manifest.permission.ACCESS_COARSE_LOCATION }, 100 );
        }

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[] { Manifest.permission.ACCESS_BACKGROUND_LOCATION }, 102 );
        }

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[] { Manifest.permission.SEND_SMS }, 103 );
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        locationNowUpdate = Calendar.getInstance().getTimeInMillis();

        if(locationNowUpdate - locationLastUpdate > 1000) {
            locationView = findViewById(R.id.location);
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            String msg = "Latitude:" + latitude + ", Longitude:" + longitude + ", speed: " + location.getSpeed();
            locationView.setText(msg);

            locationLastUpdate = locationNowUpdate;
        }
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        Log.d("Latitude", "enable");
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        Log.d("Latitude", "disable");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("Latitude", "status");
    }

    @Override
    protected void onStop() {
        if(list.size()>0){
            sm.unregisterListener(accSensor);
        }
        super.onStop();
    }

    @Override
    protected void onPostResume() {
        sm.registerListener(accSensor, (Sensor) list.get(0), SensorManager.SENSOR_DELAY_NORMAL);
        super.onPostResume();
    }

    // acceleration event listener

    SensorEventListener accSensor = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}

        public void onSensorChanged(SensorEvent event) {

            accnLastUpdate = Calendar.getInstance().getTimeInMillis();

            if(accnLastUpdate - accnNowUpdate > 20){

                final float alpha = 0.8f;

                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

                linear_acceleration[0] = event.values[0] - gravity[0];
                linear_acceleration[1] = event.values[1] - gravity[1];
                linear_acceleration[2] = event.values[2] - gravity[2];

                float val = 0;

                for(int i = 0 ; i < 3 ; i++){
                    val += linear_acceleration[i]*linear_acceleration[i];
                }

                val = (float) Math.sqrt(val);

                if(val - oldAccVal > 9.81){
                    //sendMessage("Collision");
                    //Toast.makeText(getApplicationContext(), "Accident Happened ! force is " + ((val - oldAccVal)/9.81) + " g", Toast.LENGTH_SHORT).show();
                    getPrediction(linear_acceleration[0], linear_acceleration[1], linear_acceleration[2]);
                }


                accelerationView = findViewById(R.id.acceleration);

                String msg = "x: "+linear_acceleration[0]+"\ny: "+linear_acceleration[1]+"\nz: "+linear_acceleration[2] + "\n";

                msg += val + "";

                accelerationView.setText(msg);

                oldAccVal = val;

                accnLastUpdate = accnNowUpdate;
            }
        }
    };

    // rotation event listener

    SensorEventListener rotSensor = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}

        public void onSensorChanged(SensorEvent event) {

            gyroNowUpdate = Calendar.getInstance().getTimeInMillis();

            if(gyroNowUpdate - gyroLastUpdate > 10){

                float[] rotationMatrix = new float[16];
                SensorManager.getRotationMatrixFromVector(
                        rotationMatrix, event.values);

                // Convert to orientations
                float[] orientations = new float[3];
                SensorManager.getOrientation(rotationMatrix, orientations);

                for(int i = 0; i < 3; i++) {
                    orientations[i] = (float)(Math.toDegrees(orientations[i]));
                }

                rotationView = findViewById(R.id.rotation);

                String msg = "x : " + orientations[0] + "\ny : " + orientations[1] + "\nz : " + orientations[2] + "\n";

                rotationView.setText(msg);

                gyroLastUpdate = gyroNowUpdate;
            }
        }
    };

    public void sendMessage(String accidentType){
        Intent intent=new Intent(getApplicationContext(),MainActivity.class);

        //PendingIntent pi=PendingIntent.getActivity(getApplicationContext(),0,intent,0);

        SmsManager sms= SmsManager.getDefault();

        ActivityCompat.requestPermissions(MainActivity.this, new String[] { Manifest.permission.SEND_SMS}, 1);

        String completeMsg = msg + latitude + "," + longitude + " accident type : " + accidentType;

        sms.sendTextMessage(num, null, completeMsg, null, null);
        //sms.sendTextMessage(num,null, completeMsg ,pi,null);

//        Toast.makeText(getApplicationContext(),"Message Sent successfully!", Toast.LENGTH_LONG).show();
    }

    public void getPrediction(Float x, Float y, Float z)
    {
        String value = x + "," + y + "," + z;

        Toast.makeText(getApplicationContext(), value, Toast.LENGTH_LONG).show();
        Call<Results> call = RetrofitClient.getInstance().getMyApi().getPredictionByValues(value);
        call.enqueue(new Callback<Results>() {
            @Override
            public void onResponse(Call<Results> call, Response<Results> response) {
                Toast.makeText(getApplicationContext(), "Response", Toast.LENGTH_LONG).show();
                Results prediction = response.body();
                Toast.makeText(getApplicationContext(), prediction.getResult(), Toast.LENGTH_LONG).show();

                if(prediction.getResult() == "Accident !")
                    sendMessage("Collision");
            }

            @Override
            public void onFailure(Call<Results> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "An error has occured", Toast.LENGTH_LONG).show();
            }
        });

    }
}