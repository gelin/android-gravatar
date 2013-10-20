package com.example.android.gravatar;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import jgravatar.Gravatar;
import jgravatar.GravatarDefaultImage;
import jgravatar.GravatarRating;

public class GetGravatarFragment extends Fragment {

    EditText email;
    ProgressBar progress;
    ImageView image;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.get_gravatar_fragment, container, false);
        this.email = (EditText) view.findViewById(R.id.email);
        this.progress = (ProgressBar) view.findViewById(R.id.progress);
        this.image = (ImageView) view.findViewById(R.id.image);

        Button button = (Button) view.findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findGravatar();
            }
        });

        return view;
    }

    public void findGravatar() {
        String email = this.email.getText().toString();
        new DownloadGravatarTask().execute(email);
    }

    private void showProgress() {
        this.progress.setVisibility(View.VISIBLE);
    }

    private void showImage(Bitmap bitmap) {
        this.image.setImageBitmap(bitmap);
        //image.getDrawable().setFilterBitmap(false);
        this.progress.setVisibility(View.GONE);
        this.image.setVisibility(View.VISIBLE);
    }

    private void showError() {
        this.progress.setVisibility(View.GONE);
        this.image.setVisibility(View.GONE);
        Toast.makeText(getActivity(), R.string.download_failure, Toast.LENGTH_LONG).show();
    }

    class DownloadGravatarTask extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected void onPreExecute() {
            showProgress();
        }

        @Override
        protected Bitmap doInBackground(String... strings) {
            try {
                Gravatar gravatar = new Gravatar();
                gravatar.setSize(getResources().getDimensionPixelSize(R.dimen.gravatar_size));
                gravatar.setRating(GravatarRating.GENERAL_AUDIENCES);
                gravatar.setDefaultImage(GravatarDefaultImage.IDENTICON);
                byte[] jpg = gravatar.download(strings[0]);
                return BitmapFactory.decodeByteArray(jpg, 0, jpg.length);
            } catch (Exception e) {
                Log.w(Tag.TAG, "failed to load Gravatar", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap == null) {
                showError();
            } else {
                showImage(bitmap);
            }
        }

    }

}
