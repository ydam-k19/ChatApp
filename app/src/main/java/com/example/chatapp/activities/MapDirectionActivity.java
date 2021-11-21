package com.example.chatapp.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Shader;
import android.location.Location;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.chatapp.R;
import com.example.chatapp.databinding.ActivityMapDirectionBinding;
import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.PreferenceManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

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
    Bitmap receiverProfileImage;
    PreferenceManager preferenceManager;

    private final String API_URL_TEMP = "https://api.mapbox.com/directions/v5/mapbox/driving/%s,%s;%s,%s?" +
            "annotations=maxspeed&overview=full&geometries=geojson&access_token=%s";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferenceManager = new PreferenceManager(getApplicationContext());


        binding = ActivityMapDirectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        Intent intent = getIntent();
        senderId = intent.getStringExtra(Constants.KEY_SENDER_ID);
        String receiveId = intent.getStringExtra(Constants.KEY_RECEIVER_ID);
        String lat = intent.getStringExtra(Constants.KEY_SENDER_LATITUDE);
        String lng = intent.getStringExtra(Constants.KEY_SENDER_LONGITUDE);
        String receiverProfileString = intent.getStringExtra(Constants.KEY_RECEIVER_IMAGE);

        receiverProfileImage = getUserImage(receiverProfileString);


        senderLocation = new Location("");
        senderLocation.setLatitude(Double.parseDouble(lat));
        senderLocation.setLongitude(Double.parseDouble(lng));
        receiverLocation = new Location("");


        binding.fabEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentChat = new Intent(MapDirectionActivity.this, ChatActivity.class);
                setResult(RESULT_OK, intentChat);
                finish();

            }
        });


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


    private void drawRoute(ArrayList<LatLng> points) {
        if (points.size() < 1) return;

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
                receiverLocation = location;
                preferenceManager.getString(Constants.KEY_IMAGE);
                mkStart = addMarkerOnMap(senderLocation, getUserImage(preferenceManager.getString(Constants.KEY_IMAGE)));
                mkDest = addMarkerOnMap(receiverLocation, receiverProfileImage);
                requestDirection(new LatLng(receiverLocation.getLatitude(), receiverLocation.getLongitude()), new LatLng(senderLocation.getLatitude(), senderLocation.getLongitude()));
                moveCamera();
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

    private Marker addMarkerOnMap(Location location, Bitmap userImage) {
        LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions()
                .position(position)
                .icon(BitmapDescriptorFactory.fromBitmap(userImage));
        Marker marker = mMap.addMarker(markerOptions);
        return marker;
    }

//
//    private void setCamera() {
//        CameraPosition point = new CameraPosition.Builder()
//                .target(new LatLng(senderLocation.getLatitude(), senderLocation.getLongitude()))
//                .zoom(16)
//                .tilt(30)
//                .build();
//        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(point));
//    }

    private void moveCamera() {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        builder.include(mkStart.getPosition());
        builder.include(mkDest.getPosition());

        LatLngBounds bounds = builder.build();

        int padding = 0; // offset from edges of the map in pixels
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds,150);


        mMap.animateCamera(cu);

    }


    private Bitmap getUserImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        Bitmap imageView = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        return getCircularBitmapWithWhiteBorder(imageView, 10);
    }


    @SuppressLint("ResourceAsColor")
    public static Bitmap getCircularBitmapWithWhiteBorder(Bitmap bitmap,
                                                          int borderWidth) {
        if (bitmap == null || bitmap.isRecycled()) {
            return null;
        }

        final int width = bitmap.getWidth() + borderWidth;
        final int height = bitmap.getHeight() + borderWidth;

        Bitmap canvasBitmap = Bitmap.createBitmap(width, height + borderWidth * 3, Bitmap.Config.ARGB_8888);
        BitmapShader shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setShader(shader);

        Canvas canvas = new Canvas(canvasBitmap);
        float radius = width > height ? ((float) height) / 2f : ((float) width) / 2f;
        canvas.drawCircle(width / 2, height / 2, radius, paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(R.color.primary);
        paint.setStrokeWidth(borderWidth);
        canvas.drawCircle(width / 2, height / 2, radius - borderWidth / 2, paint);
        return canvasBitmap;
    }

}