package com.example.claudialucasi.romapp.models;

/**
 * Created by Claudia Lucasi on 6/20/2018.
 */

public class User{
    private String displayname;
    private String Email;
    private long createdAt;

    public User (){};
    public User(String displayname,String email,long createdAt){
        this.setDisplayname(displayname);
        this.setEmail(email);
        this.setCreatedAt(createdAt);
    }

    public String getDisplayname() {
        return displayname;
    }

    public void setDisplayname(String displayname) {
        this.displayname = displayname;
    }

    public String getEmail() {
        return Email;
    }

    public void setEmail(String email) {
        Email = email;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
