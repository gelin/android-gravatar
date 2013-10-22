package com.example.android.gravatar;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;

import jgravatar.Gravatar;
import jgravatar.GravatarDefaultImage;
import jgravatar.GravatarRating;

import static android.provider.ContactsContract.Data;
import static android.provider.ContactsContract.Contacts;
import static android.provider.ContactsContract.CommonDataKinds.Email;

public class ViewContactsFragment extends ListFragment {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                getActivity(),
                R.layout.person_list_item,
                null,
                new String[] {
                        Contacts.DISPLAY_NAME,
                        Email.ADDRESS,
                        Data.CONTACT_ID,
                },
                new int[] {
                        R.id.name,
                        R.id.email,
                        R.id.photo,
                },
                0);
        adapter.setViewBinder(new ContactViewBinder());
        setListAdapter(adapter);

        LoaderManager manager = getLoaderManager();
        manager.initLoader(ContactsLoaderCallbacks.ID, null, new ContactsLoaderCallbacks());
    }

    class ContactsLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {

        public static final int ID = 0;

        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
            return new CursorLoader(
                    getActivity(),
                    Data.CONTENT_URI,
                    new String[] {
                            Data._ID,
                            Data.CONTACT_ID,
                            Contacts.DISPLAY_NAME,
                            Email.ADDRESS,
                    },
                    Email.ADDRESS + " is not null" +
                            " AND " + Data.MIMETYPE + " = '" + Email.CONTENT_ITEM_TYPE + "'",
                    null,
                    Contacts.DISPLAY_NAME);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> objectLoader, Cursor cursor) {
            CursorAdapter adapter = (CursorAdapter) getListAdapter();
            adapter.swapCursor(cursor);
        }


        @Override
        public void onLoaderReset(Loader<Cursor> objectLoader) {
            CursorAdapter adapter = (CursorAdapter) getListAdapter();
            adapter.swapCursor(null);
        }

    }

    class ContactViewBinder implements SimpleCursorAdapter.ViewBinder {

        @Override
        public boolean setViewValue(View view, Cursor cursor, int column) {
            int contactId;
            switch (column) {
                case 1:
                    contactId = cursor.getInt(column);
                    new PhotoLoadTask((ImageView) view).execute(contactId);
                    return true;
            }
            return false;
        }

    }

    class PhotoLoadTask extends AsyncTask<Integer, Void, Bitmap> {

        ImageView view;

        PhotoLoadTask(ImageView view) {
            this.view = view;
        }

        @Override
        protected Bitmap doInBackground(Integer... params) {
            try {
                int contactId = params[0];
                Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
                Uri photoUri = Uri.withAppendedPath(contactUri, Contacts.Photo.CONTENT_DIRECTORY);
                Cursor cursor = getActivity().getContentResolver().query(
                        photoUri,
                        new String[]{Contacts.Photo.PHOTO},
                        null, null, null);
                if (cursor == null) {
                    return null;
                }
                try {
                    if (cursor.moveToFirst()) {
                        byte[] data = cursor.getBlob(0);
                        return BitmapFactory.decodeByteArray(data, 0, data.length);
                    }
                } finally {
                    cursor.close();
                }
                return null;
            } catch (Exception e) {
                Log.w(Tag.TAG, "failed to load photo", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            this.view.setImageBitmap(bitmap);
        }

    }

    class GravatarLoadTask extends AsyncTask<String, Void, Bitmap> {

        ImageView view;

        GravatarLoadTask(ImageView view) {
            this.view = view;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            try {
                Gravatar gravatar = new Gravatar();
                gravatar.setSize(getResources().getDimensionPixelSize(R.dimen.gravatar_size));
                gravatar.setRating(GravatarRating.GENERAL_AUDIENCES);
                gravatar.setDefaultImage(GravatarDefaultImage.IDENTICON);
                byte[] jpg = gravatar.download(params[0]);
                return BitmapFactory.decodeByteArray(jpg, 0, jpg.length);
            } catch (Exception e) {
                Log.w(Tag.TAG, "failed to load Gravatar", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            this.view.setImageBitmap(bitmap);
        }

    }

}
