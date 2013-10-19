package com.example.android.gravatar;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import jgravatar.Gravatar;
import jgravatar.GravatarDefaultImage;
import jgravatar.GravatarRating;

public class GetGravatarActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.get_gravatar_activity);
    }

    public void findGravatar(View view) {
        EditText edit = (EditText) findViewById(R.id.email);
        String email = edit.getText().toString();
        new DownloadGravatarTask().execute(email);
    }

    private void showProgress() {
        View progress = findViewById(R.id.progress);
        progress.setVisibility(View.VISIBLE);
    }

    private void showImage(Bitmap bitmap) {
        View progress = findViewById(R.id.progress);
        ImageView image = (ImageView) findViewById(R.id.image);
        image.setImageBitmap(bitmap);
        //image.getDrawable().setFilterBitmap(false);
        progress.setVisibility(View.GONE);
        image.setVisibility(View.VISIBLE);
    }

    class DownloadGravatarTask extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected void onPreExecute() {
            showProgress();
        }

        @Override
        protected Bitmap doInBackground(String... strings) {
            Gravatar gravatar = new Gravatar();
            gravatar.setSize(getResources().getDimensionPixelSize(R.dimen.gravatar_size));
            gravatar.setRating(GravatarRating.GENERAL_AUDIENCES);
            gravatar.setDefaultImage(GravatarDefaultImage.IDENTICON);
            byte[] jpg = gravatar.download(strings[0]);
            return BitmapFactory.decodeByteArray(jpg, 0, jpg.length);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            showImage(bitmap);
        }

    }

}
