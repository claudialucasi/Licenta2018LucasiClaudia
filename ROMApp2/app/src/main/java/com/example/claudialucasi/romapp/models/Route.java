package com.example.claudialucasi.romapp.models;


import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

/**
 * Created by Claudia Lucasi on 6/22/2018.
 */

public class Route {


    private String title;
    private String description;
    private ArrayList<String> images;
    private String createdAt;
    private String createdBy;

    public Route(){}

    public Route(String title, String description,ArrayList<String> images,String createdAt,String createdBy){
        this.setTitle(title);
        this.setDescription(description);
        this.setImages(images);
        this.setCreatedAt(createdAt);
        this.setCreatedBy(createdBy);
    }
    public String getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public ArrayList<String> getImages() {
        return images;
    }

    public void setImages(ArrayList<String> images) {
        this.images = images;
    }
}
