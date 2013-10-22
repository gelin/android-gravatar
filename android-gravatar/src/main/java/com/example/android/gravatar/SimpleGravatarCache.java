package com.example.android.gravatar;

import android.graphics.Bitmap;

import java.util.WeakHashMap;

public class SimpleGravatarCache {

    private WeakHashMap<String, Bitmap> cache = new WeakHashMap<String, Bitmap>();

    public Bitmap getGravatar(String email) {
        return this.cache.get(email);
    }

    public void putGravatar(String email, Bitmap gravatar) {
        this.cache.put(email, gravatar);
    }

}
