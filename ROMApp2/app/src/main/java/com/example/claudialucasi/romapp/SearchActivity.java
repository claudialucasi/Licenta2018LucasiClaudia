package com.example.claudialucasi.romapp;

import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.algolia.search.saas.Client;
import com.algolia.search.saas.Index;
import com.example.claudialucasi.romapp.models.PlaceInfo;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SearchActivity extends AppCompatActivity {


    private List<JSONObject> array;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        Client client = new Client("85CD6IVLRR", "1f11162f8ebcaa359aad2b9713e6fc77");
        final Index index = client.getIndex("routes");

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference routes = db.collection("Routes");

        array = new ArrayList<>();
        Task task = routes.get().continueWith(new Continuation<QuerySnapshot, Void>() {
            @Override
            public Void then(@NonNull Task<QuerySnapshot> task) throws Exception {
                QuerySnapshot result = task.getResult();
                for (DocumentSnapshot doc : result) {
                    Map<String, Object> data = doc.getData();

                    array.add(new JSONObject(data));
                }
                if (array.isEmpty()) {
                    Log.d("Algolia", String.valueOf(array.size()));
                } else {
                    Log.d("Algolia", String.valueOf(array.size()));
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                                Log.d("Thread", String.valueOf(array.size()));
                                JSONArray result = new JSONArray(array);
                                index.addObjectsAsync(result, null);

                        }
                    }).start();
                }
                return null;
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
    }
}
