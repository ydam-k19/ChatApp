package com.example.chatapp.activities;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.chatapp.R;
import com.example.chatapp.databinding.ActivityMapDirectionBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Objects;

public class MapDirectionActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int REQUEST_PERMISSION_CODE = 123;
    private GoogleMap mMap;
    private ActivityMapDirectionBinding binding;
    private FusedLocationProviderClient fusedLocationClient;
    private Location receiverLocation;
    private Location senderLocation;
    private Marker mkStart, mkDest;
    private Polyline route;
    private String senderId;



    private final String API_URL_TEMP = "https://api.mapbox.com/directions/v5/mapbox/driving/%s,%s;%s,%s?" +
            "annotations=maxspeed&overview=full&geometries=geojson&access_token=%s";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        binding = ActivityMapDirectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        Intent intent=getIntent();
        senderId=intent.getStringExtra("senderId");
        String receiveId=intent.getStringExtra("receiverId");
        String lat=intent.getStringExtra("senderLatitude");
        String lng=intent.getStringExtra("senderLongitude");

        Log.d("lattude mapDirection get",lat);
        Log.d("longittued map Direction get",lng);

        senderLocation=new Location("");
        senderLocation.setLatitude(Double.parseDouble(lat));
        senderLocation.setLongitude(Double.parseDouble(lng));

        receiverLocation=new Location("");


        binding.fabEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentChat=new Intent(MapDirectionActivity.this,ChatActivity.class);
                setResult(RESULT_OK,intentChat);
                finish();

            }
        });


    }

    private Bitmap getBitmapFromEncodedString(String encodedImage) {
        if(!encodedImage.equals("empty Image")){
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }
        return null;

    }
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        updateLastLocation();
       // requestDirection(new LatLng(10.762912, 106.682172), new LatLng(10.764958808724096, 106.67821224330648));

        binding.fabEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }



    private static BitmapDescriptor scaleIcon(Bitmap unscaledBitmap, Double scale) {
        int width = (int) (unscaledBitmap.getWidth() * scale);
        int height = (int) (unscaledBitmap.getHeight() * scale);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(unscaledBitmap, width, height, false);
        return BitmapDescriptorFactory.fromBitmap(scaledBitmap);
    }
    private void drawRoute(ArrayList<LatLng> points) {
        if (points.size() < 1) return;

        mkDest = mMap.addMarker(new MarkerOptions()
                .position(points.get(points.size() - 1))


        );

        route = mMap.addPolyline(new PolylineOptions()
                .addAll(points)
                .color(Color.RED)
        );
    }

    @SuppressLint("DefaultLocale")
    private void requestDirection(LatLng start, LatLng dest) {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = String.format(API_URL_TEMP,
                start.longitude, start.latitude,
                dest.longitude, dest.latitude,
                this.getString(R.string.mapbox_access_token));
        Log.d("@@@ url", url);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("@@@ response", response);
                        ArrayList<LatLng> points = parseJsonResult(start, dest, response);
                        Log.d("startinggggggggg",start.toString());
                        Log.d("destingggggggggggg",dest.toString());
                        drawRoute(points);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("@@@ error", error.toString());
            }
        });
        queue.add(stringRequest);
    }

    private ArrayList<LatLng> parseJsonResult(LatLng start, LatLng dest, String response) {
        ArrayList<LatLng> points = new ArrayList<>();

        try {

            JSONObject jsonResult = new JSONObject(response);
            JSONArray jsonPoints = jsonResult.getJSONArray("routes")
                    .getJSONObject(0)
                    .getJSONObject("geometry")
                    .getJSONArray("coordinates");

            for (int i = 0; i < jsonPoints.length(); i++) {
                JSONArray tmp = jsonPoints.getJSONArray(i);
                double lat = tmp.getDouble(1);
                double lng = tmp.getDouble(0);
                points.add(new LatLng(lat, lng));
            }

            points.add(0, start);
            points.add(dest);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return points;
    }


    private void updateLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};
            requestPermissions(permissions, REQUEST_PERMISSION_CODE);
            return;
        }
        mMap.setMyLocationEnabled(true);
        fusedLocationClient.getLastLocation().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Location location = task.getResult();
                receiverLocation=location;
                addMarkerOnMap(senderLocation);
                addMarkerOnMap(receiverLocation);
                requestDirection(new LatLng(receiverLocation.getLatitude(),receiverLocation.getLongitude()), new LatLng(senderLocation.getLatitude(),senderLocation.getLongitude()));

            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateLastLocation();
            } else {
                finish();
            }
        }
    }

    private Marker addMarkerOnMap(Location location) {
        LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions()
                .position(position);
        Marker marker = mMap.addMarker(markerOptions);
        CameraPosition point = new CameraPosition.Builder()
                .target(new LatLng(location.getLatitude(), location.getLongitude()))
                .zoom(16)
                .tilt(30)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(point));
        return marker;
    }

    private BitmapDescriptor BitmapFromVector(Context context, int vectorResId) {
        // below line is use to generate a drawable.
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);

        // below line is use to set bounds to our vector drawable.
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());

        // below line is use to create a bitmap for our
        // drawable which we have added.
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);

        // below line is use to add bitmap in our canvas.
        Canvas canvas = new Canvas(bitmap);

        // below line is use to draw our
        // vector drawable in canvas.
        vectorDrawable.draw(canvas);

        // after generating our bitmap we are returning our bitmap.
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
}