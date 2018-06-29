package com.example.claudialucasi.romapp;


import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.directions.route.AbstractRouting;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.example.claudialucasi.romapp.Adapters.ViewPagerAdapter;
import com.example.claudialucasi.romapp.models.PlaceInfo;
import com.example.claudialucasi.romapp.models.Route;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DirectionsStep;
import com.google.maps.model.EncodedPolyline;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class ViewRoutePostActivity extends AppBaseActivity implements OnMapReadyCallback,RoutingListener {

    private Route route;
    private TextView title,description;
    private ViewPager viewPager;
    private LinearLayout sliderDotspanel;
    private int dotscount;
    private ImageView[] dots;
    private LinearLayout parentLinearLayout;
    private FirebaseFirestore db;
    private GoogleMap map;

    private ArrayList<PlaceInfo> places;
    private ArrayList<LatLng> waypoints;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        RelativeLayout content = (RelativeLayout) findViewById(R.id.activity_content);
        inflater.inflate(R.layout.activity_view_route_post, content);
        getRoute();
        init();

    }

    private void getRoute(){
        route = new Route();
        Intent i = getIntent();
        route.setTitle(getIntent().getStringExtra("title"));
        route.setDescription(getIntent().getStringExtra("description"));
        route.setImages(getIntent().getStringArrayListExtra("images"));
        route.setCreatedBy(getIntent().getStringExtra("userID"));
        route.setCreatedAt(getIntent().getStringExtra("createdAt"));
        places = i.getParcelableArrayListExtra("places");

    }

    private void init(){

        title = findViewById(R.id.title_view);
        title.setText(route.getTitle());
        description = findViewById(R.id.description_view);
        description.setText(route.getDescription());

        /*** dealing with the images ***/
        int noOfImages;
        if(!route.getImages().isEmpty()){
            noOfImages = route.getImages().size();
            parentLinearLayout = (LinearLayout) findViewById(R.id.parent);

            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View imagesView = inflater.inflate(R.layout.slider, null);
            parentLinearLayout.addView(imagesView, parentLinearLayout.getChildCount() - 3);

            viewPager = imagesView.findViewById(R.id.viewPager);

            sliderDotspanel = imagesView.findViewById(R.id.SliderDots);

            ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(ViewRoutePostActivity.this);
            viewPagerAdapter.setImages(route.getImages());

            viewPager.setAdapter(viewPagerAdapter);

            dotscount = noOfImages;
            dots = new ImageView[dotscount];

            for(int i = 0; i < dotscount; i++){

                dots[i] = new ImageView(ViewRoutePostActivity.this);
                dots[i].setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.non_active_dot));

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

                params.setMargins(8, 0, 8, 0);

                sliderDotspanel.addView(dots[i], params);

            }
            dots[0].setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.active_dot));

            viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                }

                @Override
                public void onPageSelected(int position) {

                    for(int i = 0; i< dotscount; i++){
                        dots[i].setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.non_active_dot));
                    }

                    dots[position].setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.active_dot));

                }

                @Override
                public void onPageScrollStateChanged(int state) {

                }
            });
        }
        /*** finished dealing with the images ***/


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.route_map);
        mapFragment.getMapAsync(ViewRoutePostActivity.this);
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {

        map = googleMap;
        map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        initMap();
    }

    private void initMap() {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        LatLng latLng;
        waypoints = new ArrayList<>();

        /** marker for start point **/
        LatLng start = places.get(0).getLatlng();

        map.addMarker(new MarkerOptions()
                .position(start)
                .title(places.get(0).getName())
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        builder.include(start);
        waypoints.add(start);

        /** marker for end point **/

        LatLng end = places.get(places.size()-1).getLatlng();

        map.addMarker(new MarkerOptions()
                .position(end)
                .title(places.get(places.size()-1).getName())
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)));
        builder.include(end);


        /** markers for intermediate points **/
        for(int i=1; i < places.size()-1 ;i++)
        {
            String title;
            latLng = places.get(i).getLatlng();
            waypoints.add(latLng);
            title =places.get(i).getName();
            builder.include(latLng);
            map.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(title)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)));

            builder.include(latLng);
        }
        waypoints.add(end);

        LatLngBounds bounds = builder.build();
        final CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 200);

        map.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                map.animateCamera(cu);
                drawRoute();
            }
        });
    }

    private void drawRoute() {
        Routing routing = new Routing.Builder()
                .travelMode(Routing.TravelMode.WALKING)
                .withListener(this)
                .alternativeRoutes(false)
                .waypoints(waypoints)
                .build();
        routing.execute();


    }


    @Override
    public void onBackPressed() {
        map.clear();
        Intent i = new Intent(ViewRoutePostActivity.this, HomeActivity.class);
        startActivity(i);
        finish();
        super.onBackPressed();
    }

    @Override
    protected void onStop() {
        finish();
        super.onStop();
    }

    @Override
    public void onRoutingFailure(RouteException e) {

    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    public void onRoutingSuccess(ArrayList<com.directions.route.Route> arrayList, int i) {
                //ArrayList<Polyline> polylines = new ArrayList<>();

                PolylineOptions polylineOptions = new PolylineOptions();
                polylineOptions.color(Color.CYAN);
                polylineOptions.width(13);
                polylineOptions.addAll(arrayList.get(0).getPoints());
                map.addPolyline(polylineOptions);

    }

    @Override
    public void onRoutingCancelled() {

    }
}


