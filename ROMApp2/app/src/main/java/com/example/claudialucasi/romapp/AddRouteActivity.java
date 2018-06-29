package com.example.claudialucasi.romapp;


import android.*;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.algolia.search.saas.Client;
import com.algolia.search.saas.Index;
import com.example.claudialucasi.romapp.Adapters.PlaceAdapter;
import com.example.claudialucasi.romapp.models.PlaceInfo;
import com.example.claudialucasi.romapp.models.Route;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.RuntimeRemoteException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;

public class AddRouteActivity extends AppBaseActivity implements View.OnClickListener {

    private static final String TAG = " ADD ROUTE : ";
    private EditText _titleInput;
    private EditText _description;
    private LinearLayout parentLinearLayout;
    private int maxFields = 15;
    protected GeoDataClient mGeoDataClient;
    private static final LatLngBounds BOUNDS_GREATER_SYDNEY = new LatLngBounds(
            new LatLng(43.6667, 25.3667), new LatLng(48.2391, 26.7324));
    private Map<Integer,PlaceInfo> routeMap;
    private ArrayList<Uri> images;
    private CircleImageView image1;
    private CircleImageView image2;
    private CircleImageView image3;
    private Uri mainImageURI1;
    private Uri mainImageURI2;
    private Uri mainImageURI3;
    int image = 0;
    private String user_id;
    private String unique;

    private StorageReference storageReference;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;

    private Bitmap compressedImageFile1;
    private Bitmap compressedImageFile2;
    private Bitmap compressedImageFile3;

    private Route route;

    /*** prepare the  arraylist for uploaded picture ***/
    private ArrayList<String> uploads = new ArrayList<>();
    ArrayList<PlaceInfo> orderdedRoutePoints;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Client client = new Client("85CD6IVLRR", "1f11162f8ebcaa359aad2b9713e6fc77");
        final Index index = client.getIndex("routes");
        LayoutInflater inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        RelativeLayout content = (RelativeLayout) findViewById(R.id.activity_content);
        inflater.inflate(R.layout.activity_add_route, content);

        getSupportActionBar().setTitle("Add a new Route");


        mGeoDataClient = Places.getGeoDataClient(this, null);


        _titleInput = (EditText) findViewById(R.id.title_input);
        _description = (EditText) findViewById(R.id.description_input);

        parentLinearLayout = (LinearLayout) findViewById(R.id.parent_linear_layout);
        routeMap = new HashMap<Integer, PlaceInfo>();
        images = new ArrayList<Uri>();

        image1 = findViewById(R.id.firstPhoto);
        image2 = findViewById(R.id.secondPhoto);
        image3 = findViewById(R.id.thirdPhoto);

        image1.setOnClickListener(this);
        image2.setOnClickListener(this);
        image3.setOnClickListener(this);

        firebaseAuth = FirebaseAuth.getInstance();
        user_id = firebaseAuth.getCurrentUser().getUid();
        storageReference = FirebaseStorage.getInstance().getReference();
        firebaseFirestore = FirebaseFirestore.getInstance();
        route = new Route();
        unique = String.valueOf(new Date().getTime());

    }

    public void onAddField(View v) {
        if(maxFields == 0)
        {
            Toast.makeText(AddRouteActivity.this, "You reached your limit of route points", Toast.LENGTH_LONG).show();
            return;
        }
        else
        {
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View rowView = inflater.inflate(R.layout.field, null);
            final int id = rowView.generateViewId();
            rowView.setId(id);
            // Add the new row before the add field button.
            parentLinearLayout.addView(rowView, parentLinearLayout.getChildCount() - 4);
            maxFields = maxFields - 1;

            Log.d(TAG,"ID: " + rowView.getId());
            routeMap.put(id,null);
            final PlaceAdapter adapter = new PlaceAdapter(this, mGeoDataClient, BOUNDS_GREATER_SYDNEY, null);

            final AutoCompleteTextView autocompleteTextView = rowView.findViewById(R.id.number_edit_text);

            final OnCompleteListener<PlaceBufferResponse> mUpdatePlaceDetailsCallback
                    = new OnCompleteListener<PlaceBufferResponse>() {
                @Override
                public void onComplete(Task<PlaceBufferResponse> task) {
                    try {
                        PlaceBufferResponse places = task.getResult();

                        // Get the Place object from the buffer.
                        final Place place = places.get(0);
                        try{
                            PlaceInfo mPlace;
                            mPlace = new PlaceInfo();
                            mPlace.setName(place.getName().toString());
                            mPlace.setAddress(place.getAddress().toString());
                            mPlace.setId(place.getId());
                            mPlace.setLatlng(place.getLatLng());
                            routeMap.put(id,mPlace);
                        }catch (NullPointerException e) {
                            Log.e(TAG, "onResult: NullPointerException: " + e.getMessage());
                        }


                        Log.i(TAG, "Place details received: " + place.getName());

                        places.release();
                    } catch (RuntimeRemoteException e) {
                        // Request did not complete successfully
                        Log.e(TAG, "Place query did not complete.", e);
                        return;
                    }
                }
            };

            AdapterView.OnItemClickListener mAutocompleteClickListener
                    = new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                /*
                 Retrieve the place ID of the selected item from the Adapter.
                 The adapter stores each Place suggestion in a AutocompletePrediction from which we
                 read the place ID and title.
                  */
                    final AutocompletePrediction item = adapter.getItem(position);
                    final String placeId = item.getPlaceId();
                    final CharSequence primaryText = item.getPrimaryText(null);

                    Task<PlaceBufferResponse> placeResult = mGeoDataClient.getPlaceById(placeId);
                    placeResult.addOnCompleteListener(mUpdatePlaceDetailsCallback);

                    Log.i(TAG, "Autocomplete item selected: " + primaryText);
                    rowView.invalidate();
                }
            };

            autocompleteTextView.setOnItemClickListener(mAutocompleteClickListener);
            autocompleteTextView.setAdapter(adapter);


        }

    }

    public void onDelete(View v) {
        int id = ((View) v.getParent()).getId();
        Log.d(TAG, "ID :" + id);
        routeMap.remove(id);
        parentLinearLayout.removeView((View) v.getParent());
        maxFields = maxFields + 1;
        Log.d(TAG, String.valueOf(routeMap));
    }


    private void BringImagePicker() {

        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(1, 1)
                .start(AddRouteActivity.this);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {

                switch (image){
                    case 1:
                        mainImageURI1 = result.getUri();
                        image1.setImageURI(mainImageURI1);
                        break;
                    case 2:
                        mainImageURI2 = result.getUri();
                        image2.setImageURI(mainImageURI2);
                        break;
                    case 3:
                        mainImageURI3 = result.getUri();
                        image3.setImageURI(mainImageURI3);
                }

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {

                Exception error = result.getError();

            }
        }
    }

    public boolean validateRouteFields(){
        for( int i = 4; i< 18 - maxFields; i++)
        {
            View v = parentLinearLayout.getChildAt(i);
            AutoCompleteTextView a = v.findViewById(R.id.number_edit_text);
            if(a.getText().toString().isEmpty())
            {
                Toast.makeText(this, "You have at least one empty route point field", Toast.LENGTH_LONG).show();
                return false;
            }
        }
        return true;
    }

    public boolean validate(){
        if(_titleInput.getText().toString().isEmpty())
        {
            _titleInput.setError("Set a title");
            return false;
        }
        if(_description.getText().toString().isEmpty()) {
            _description.setError("Write a description");
            return false;
        }
        if(routeMap.size() < 2)
        {
            Toast.makeText(this, "You should have at least 2 route points", Toast.LENGTH_LONG).show();
            return false;
        }
        if(validateRouteFields() == false)
        {
            return false;
        }
        return true;
    }

    public void onAddRoute(View v) throws InterruptedException {
        if (validate() == false) {
            return;
        }

        List sortedKeys = new ArrayList(routeMap.keySet());
        Collections.sort(sortedKeys);
        Log.d(TAG, sortedKeys.toString());
        orderdedRoutePoints = new ArrayList<PlaceInfo>(sortedKeys.size());
        int k = 0;
        for (int i = 0; i < sortedKeys.size(); i++) {
            orderdedRoutePoints.add(i, routeMap.get(sortedKeys.get(i)));
            orderdedRoutePoints.get(i).setCreatedAt(String.valueOf(k));
            k = k +1;
        }
        Log.d(TAG, orderdedRoutePoints.toString());

        //construct the route object
        route.setTitle(_titleInput.getText().toString());
        route.setDescription(_description.getText().toString());
        route.setCreatedAt(unique);
        route.setCreatedBy(user_id);
        /***** object constructed ******/
        progressDialog = new ProgressDialog(AddRouteActivity.this,
                R.style.AppTheme_Dark_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Proccesing your data ...");
        progressDialog.show();
                if (mainImageURI1 != null) {
                    File newImageFile = new File(mainImageURI1.getPath());
                    try {

                        compressedImageFile1 = new Compressor(AddRouteActivity.this)
                                .setMaxHeight(640)
                                .setMaxWidth(480)
                                .setQuality(100)
                                .compressToBitmap(newImageFile);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    compressedImageFile1.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    byte[] thumbData = baos.toByteArray();

                    UploadTask image_path = storageReference.child("route_images").child(unique).child("1" + ".jpg").putBytes(thumbData);

                    image_path.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                            if (task.isSuccessful()) {
                                //put the task in an array list
                                Log.d(TAG,"first image url : " + task.getResult().getDownloadUrl());
                                uploads.add(task.getResult().getDownloadUrl().toString());
                                Log.d(TAG,"first image url : " + uploads);
                                //storeFirestore(task, route);

                            } else {

                                String error = task.getException().getMessage();
                                Toast.makeText(AddRouteActivity.this, "(IMAGE Error) : " + error, Toast.LENGTH_LONG).show();

                            }
                        }
                    });
                }
                if (mainImageURI2 != null) {
                    File newImageFile = new File(mainImageURI2.getPath());
                    try {

                        compressedImageFile2 = new Compressor(AddRouteActivity.this)
                                .setMaxHeight(640)
                                .setMaxWidth(480)
                                .setQuality(100)
                                .compressToBitmap(newImageFile);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    compressedImageFile2.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    byte[] thumbData = baos.toByteArray();

                    UploadTask image_path = storageReference.child("route_images").child(unique).child("2" + ".jpg").putBytes(thumbData);

                    image_path.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                            if (task.isSuccessful()) {
                                //put the task in an array list
                                Log.d(TAG,"second image url : " + task.getResult().getDownloadUrl());
                                uploads.add(task.getResult().getDownloadUrl().toString());
                                Log.d(TAG,"second image url : " + uploads);

                            } else {

                                String error = task.getException().getMessage();
                                Toast.makeText(AddRouteActivity.this, "(IMAGE Error) : " + error, Toast.LENGTH_LONG).show();

                            }
                        }
                    });
                }
                if (mainImageURI3 != null) {
                    File newImageFile = new File(mainImageURI3.getPath());
                    try {

                        compressedImageFile3 = new Compressor(AddRouteActivity.this)
                                .setMaxHeight(640)
                                .setMaxWidth(480)
                                .setQuality(100)
                                .compressToBitmap(newImageFile);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    compressedImageFile3.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    byte[] thumbData = baos.toByteArray();

                    UploadTask image_path = storageReference.child("route_images").child(unique).child("3" + ".jpg").putBytes(thumbData);

                    image_path.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                            if (task.isSuccessful()) {
                                //put the task in an array list
                                Log.d(TAG,"third image url : " + task.getResult().getDownloadUrl());
                                uploads.add(task.getResult().getDownloadUrl().toString());
                                Log.d(TAG,"third image url : " + uploads);

                            } else {

                                String error = task.getException().getMessage();
                                Toast.makeText(AddRouteActivity.this, "(IMAGE Error) : " + error, Toast.LENGTH_LONG).show();

                            }
                        }
                    });
                }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    Thread.sleep(30000);
                    if(uploads != null)
                    {
                        storeFirestore(uploads, route);
                    }
                    storeFirestore(null, route);
                }catch(InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }


    private void storeFirestore(@NonNull ArrayList<String> uploads, final Route route) {

        if (uploads != null) {
            route.setImages(uploads);
        }
            firebaseFirestore.collection("Routes").document(unique).set(route).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {

                    if (task.isSuccessful()) {
                        /**** now add the PlaceInfos subcolectiona ****/
                        for( int i = 0; i < orderdedRoutePoints.size(); i++)
                        {
                            firebaseFirestore.collection("Routes")
                                             .document(unique)
                                             .collection("Places")
                                             .document(orderdedRoutePoints.get(i).getId())
                                             .set(orderdedRoutePoints.get(i));
                        }
                        /** add to Algolia **/





                        progressDialog.dismiss();
                        Toast.makeText(AddRouteActivity.this, "The route was succesfully added.", Toast.LENGTH_LONG).show();
                        Intent mainIntent = new Intent(AddRouteActivity.this, HomeActivity.class);
                        startActivity(mainIntent);
                        finish();
                        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);

                    } else {
                        progressDialog.dismiss();
                        String error = task.getException().getMessage();
                        Toast.makeText(AddRouteActivity.this, "(FIRESTORE Error) : " + error, Toast.LENGTH_LONG).show();

                    }

                }
            });
    }


    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.firstPhoto:
                image = 1;
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){

                    if(ContextCompat.checkSelfPermission(AddRouteActivity.this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){

                        Toast.makeText(AddRouteActivity.this, "Permission Denied", Toast.LENGTH_LONG).show();
                        ActivityCompat.requestPermissions(AddRouteActivity.this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

                    } else {
                        BringImagePicker();
                    }
                } else {
                    BringImagePicker();
                }
                break;
            case R.id.secondPhoto:
                image = 2;
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){

                    if(ContextCompat.checkSelfPermission(AddRouteActivity.this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){

                        Toast.makeText(AddRouteActivity.this, "Permission Denied", Toast.LENGTH_LONG).show();
                        ActivityCompat.requestPermissions(AddRouteActivity.this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

                    } else {
                        BringImagePicker();
                    }
                } else {
                    BringImagePicker();
                }
                break;
            case R.id.thirdPhoto:
                image = 3;
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){

                    if(ContextCompat.checkSelfPermission(AddRouteActivity.this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){

                        Toast.makeText(AddRouteActivity.this, "Permission Denied", Toast.LENGTH_LONG).show();
                        ActivityCompat.requestPermissions(AddRouteActivity.this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

                    } else {
                        BringImagePicker();
                    }
                } else {
                    BringImagePicker();
                }
                break;
        }
    }

}