package com.example.android.gravatar;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.Intent;
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
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import java.util.WeakHashMap;

import jgravatar.Gravatar;
import jgravatar.GravatarDefaultImage;
import jgravatar.GravatarRating;

import static android.provider.ContactsContract.CommonDataKinds.Email;
import static android.provider.ContactsContract.Contacts;
import static android.provider.ContactsContract.Data;

public class ViewContactsFragment extends ListFragment {

    private static final String[] PROJECTION = new String[] {
            Data._ID,
            Data.CONTACT_ID,
            Contacts.DISPLAY_NAME,
            Email.ADDRESS,
    };
    private static final int CONTACT_ID_COL = 1;

    SimpleGravatarCache cache;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        this.cache = new SimpleGravatarCache();

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                getActivity(),
                R.layout.person_list_item,
                null,
                new String[] {
                        Contacts.DISPLAY_NAME,
                        Email.ADDRESS,
                        Data.CONTACT_ID,
                        Email.ADDRESS,
                },
                new int[] {
                        R.id.name,
                        R.id.email,
                        R.id.photo,
                        R.id.gravatar,
                },
                0);
        adapter.setViewBinder(new ContactViewBinder());
        setListAdapter(adapter);

        LoaderManager manager = getLoaderManager();
        manager.initLoader(ContactsLoaderCallbacks.ID, null, new ContactsLoaderCallbacks());
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Cursor cursor = (Cursor) getListAdapter().getItem(position);
        long contactId = cursor.getLong(CONTACT_ID_COL);
        Uri uri = Uri.withAppendedPath(Contacts.CONTENT_URI, String.valueOf(contactId));
        Log.d(Tag.TAG, "opening: " + uri);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    class ContactsLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {

        public static final int ID = 0;

        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
            return new CursorLoader(
                    getActivity(),
                    Data.CONTENT_URI,
                    PROJECTION,
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

        WeakHashMap<View, AsyncTask> imageLoadTasks = new WeakHashMap<View, AsyncTask>();

        @Override
        public boolean setViewValue(View view, Cursor cursor, int column) {
            switch (view.getId()) {
                case R.id.photo:
                    int contactId = cursor.getInt(column);
                    startPhotoTask(view, contactId);
                    return true;
                case R.id.gravatar:
                    String email = cursor.getString(column);
                    startGravatarTask(view, email);
                    return true;
            }
            return false;
        }

        void startPhotoTask(View view, int contactId) {
            cancelTask(view);
            PhotoLoadTask task = new PhotoLoadTask((ImageView) view);
            this.imageLoadTasks.put(view, task);
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, contactId);
        }

        void startGravatarTask(View view, String email) {
            cancelTask(view);
            GravatarLoadTask task = new GravatarLoadTask((ImageView) view);
            this.imageLoadTasks.put(view, task);
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, email);
        }

        void cancelTask(View view) {
            AsyncTask task = this.imageLoadTasks.get(view);
            if (task == null) {
                return;
            }
            task.cancel(true);
        }

    }

    abstract class ImageLoadTask<Params> extends AsyncTask<Params, Void, Bitmap> {

        ImageView view;

        protected ImageLoadTask(ImageView view) {
            this.view = view;
        }

        @Override
        protected void onPreExecute() {
            this.view.setVisibility(View.INVISIBLE);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            this.view.setImageBitmap(bitmap);
            this.view.setVisibility(View.VISIBLE);
        }

    }

    class PhotoLoadTask extends ImageLoadTask<Integer> {

        PhotoLoadTask(ImageView view) {
            super(view);
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

    }

    class GravatarLoadTask extends ImageLoadTask<String> {

        GravatarLoadTask(ImageView view) {
            super(view);
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            try {
                String email = params[0];
                Bitmap bitmap = ViewContactsFragment.this.cache.getGravatar(email);
                if (bitmap != null) {
                    return bitmap;
                }
                Gravatar gravatar = new Gravatar();
                gravatar.setSize(getResources().getDimensionPixelSize(R.dimen.gravatar_thumbnail_size));
                gravatar.setRating(GravatarRating.GENERAL_AUDIENCES);
                gravatar.setDefaultImage(GravatarDefaultImage.IDENTICON);
                byte[] jpg = gravatar.download(email);
                bitmap = BitmapFactory.decodeByteArray(jpg, 0, jpg.length);
                ViewContactsFragment.this.cache.putGravatar(email, bitmap);
                return bitmap;
            } catch (Exception e) {
                Log.w(Tag.TAG, "failed to load Gravatar", e);
                return null;
            }
        }

    }

}
