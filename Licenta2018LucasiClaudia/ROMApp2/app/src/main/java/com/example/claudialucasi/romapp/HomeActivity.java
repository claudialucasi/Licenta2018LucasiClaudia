package com.example.claudialucasi.romapp;

/**
 * Created by Claudia Lucasi on 6/6/2018.
 */

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.algolia.search.saas.Client;
import com.algolia.search.saas.Index;
import com.bumptech.glide.Glide;
import com.example.claudialucasi.romapp.models.PlaceInfo;
import com.example.claudialucasi.romapp.models.Route;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.firebase.ui.firestore.SnapshotParser;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;


public class HomeActivity extends AppBaseActivity {
    private RecyclerView routes_list_view;

    private FirebaseFirestore db;
    private FirestoreRecyclerAdapter adapter;
    private LinearLayoutManager linearLayoutManager;

    private ArrayList<Map<String, Object>> DataList = new ArrayList<>();
    private ArrayList<PlaceInfo> places = new ArrayList<>();
    private Task task;
    private Intent i;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        RelativeLayout content = (RelativeLayout) findViewById(R.id.activity_content);
        inflater.inflate(R.layout.activity_home, content);

        Client client = new Client("85CD6IVLRR", "1f11162f8ebcaa359aad2b9713e6fc77");
        Index index = client.getIndex("your_index_name");

        routes_list_view = (RecyclerView) findViewById(R.id.routes_list_view);
        routes_list_view.setHasFixedSize(true);
        init();
        getRoutesList();

    }

    private void init(){
        linearLayoutManager = new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.VERTICAL, false);
        routes_list_view.setLayoutManager(linearLayoutManager);
        db = FirebaseFirestore.getInstance();
    }

    private void getRoutesList() {

        Query query = db.collection("Routes").orderBy("createdAt",Query.Direction.DESCENDING);
        //Log.d("homelist",db.collection("Routes").orderBy("createdAt",Query.Direction.DESCENDING).get().toString());

        FirestoreRecyclerOptions<Route> response = new FirestoreRecyclerOptions.Builder<Route>()
                .setQuery(query, new SnapshotParser<Route>() {
                    @Override
                    public Route parseSnapshot(DocumentSnapshot snapshot) {
                        Route route = snapshot.toObject(Route.class);
                        return route;
                    }
                })
                .setLifecycleOwner(this)
                .build();

        adapter = new FirestoreRecyclerAdapter<Route, RouteHolder>(response) {
            @NonNull
            @Override
            public RouteHolder onCreateViewHolder(@NonNull ViewGroup group, int viewType) {
                View view = LayoutInflater.from(group.getContext())
                        .inflate(R.layout.route_row, group, false);

                return new RouteHolder(view);
            }

            @Override
            public void onError(FirebaseFirestoreException e) {
                Log.e("error", e.getMessage());
            }

            @Override
            public void onBindViewHolder(RouteHolder holder, int position,  final Route model) {

                holder.post_title.setText(model.getTitle());
                holder.post_desc.setText(model.getDescription());

                ArrayList<String> images = model.getImages();
                if(!images.isEmpty()) {

                    Glide.with(getApplicationContext())
                            .load(images.get(0))
                            .into(holder.post_image);
                }
                else{
                    Glide.with(getApplicationContext())
                            .load(getResources().getIdentifier("standard","drawable",getPackageName()))
                            .into(holder.post_image);
                }

                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                            i = new Intent(HomeActivity.this, ViewRoutePostActivity.class);
                            i.putExtra("title", model.getTitle());
                            i.putExtra("description", model.getDescription());
                            i.putExtra("images", model.getImages());
                            i.putExtra("userID", model.getCreatedBy());
                            i.putExtra("createdAt", model.getCreatedAt());
                            queryPlaces(model.getCreatedAt());
                    }
                });
            }
        };
        adapter.notifyDataSetChanged();
        routes_list_view.setAdapter(adapter);
    }

    private void queryPlaces(String id) {
        task = db.collection("Routes").document(id).collection("Places").orderBy("createdAt", Query.Direction.ASCENDING).get().continueWith(new Continuation<QuerySnapshot, Void>() {
            @Override
            public Void then(@NonNull Task<QuerySnapshot> task) throws Exception {
                QuerySnapshot result = task.getResult();
                for(DocumentSnapshot doc : result){
                    Map<String, Object> data = doc.getData();
                    PlaceInfo place = new PlaceInfo();
                    place.setName(data.get("name").toString());
                    data.get("latlng").getClass();
                    String latlng = data.get("latlng").toString();
                    String[] string = latlng.toString().split(",");
                    String[] latitude = string[0].split("=");
                    String[] longitude = string[1].split("=");
                    longitude[1]=longitude[1].substring(0, longitude[1].length()-1);
                    place.setLatlng(new LatLng(Double.parseDouble(latitude[1]),Double.parseDouble(longitude[1])));
                    place.setAddress(data.get("address").toString());
                    places.add(place);
                }
                if(!places.isEmpty())
                {
                    i.putExtra("places", places);
                    //progressDialog.dismiss();
                    startActivity(i);
                    finish();
                }
                if(places.isEmpty())
                {
                    Toast.makeText(HomeActivity.this,"Connection error",Toast.LENGTH_LONG).show();
                }
                return null;
            }
        });

    }


    public class RouteHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.post_title)
        TextView post_title;
        @BindView(R.id.post_image)
        ImageView post_image;
        @BindView(R.id.post_desc)
        TextView post_desc;

        public RouteHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
    }
}