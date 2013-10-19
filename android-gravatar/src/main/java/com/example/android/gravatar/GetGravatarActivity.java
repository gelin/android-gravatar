package com.example.android.gravatar;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

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

    class DownloadGravatarTask extends AsyncTask<String, Void, byte[]> {

        @Override
        protected byte[] doInBackground(String... strings) {
            Gravatar gravatar = new Gravatar();
            gravatar.setSize(getResources().getDimensionPixelSize(R.dimen.gravatar_size));
            gravatar.setRating(GravatarRating.GENERAL_AUDIENCES);
            gravatar.setDefaultImage(GravatarDefaultImage.IDENTICON);
            return gravatar.download(strings[0]);
        }

    }

}
