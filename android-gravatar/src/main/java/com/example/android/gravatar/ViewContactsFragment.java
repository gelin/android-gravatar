package com.example.android.gravatar;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
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
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.WeakHashMap;

import jgravatar.Gravatar;
import jgravatar.GravatarDefaultImage;
import jgravatar.GravatarRating;

import static android.provider.ContactsContract.CommonDataKinds.Email;
import static android.provider.ContactsContract.CommonDataKinds.Photo;
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

        ListView list = getListView();
        list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        list.setMultiChoiceModeListener(new ChoiceListener());

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

    private class ContactsLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {

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

    private class ContactViewBinder implements SimpleCursorAdapter.ViewBinder {

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

    private abstract class ImageLoadTask<Params> extends AsyncTask<Params, Void, Bitmap> {

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

    private class PhotoLoadTask extends ImageLoadTask<Integer> {

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

    private class GravatarLoadTask extends ImageLoadTask<String> {

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

    private class ChoiceListener implements AbsListView.MultiChoiceModeListener {

        @Override
        public void onItemCheckedStateChanged(ActionMode actionMode, int position, long id, boolean checked) {
            actionMode.setTitle(String.valueOf(getListView().getCheckedItemCount()));
        }

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            MenuInflater inflater = actionMode.getMenuInflater();
            inflater.inflate(R.menu.context, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;   //nothing to do
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.action_save:
                    saveSelectedItems();
                    actionMode.finish();
                    return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            //nothing to do
        }
    }

    private void saveSelectedItems() {
        SparseBooleanArray pos = getListView().getCheckedItemPositions().clone();
        new SaveGravatarTask().execute(pos);
    }

    private class SaveGravatarTask extends AsyncTask<SparseBooleanArray, Void, Integer> {

        @Override
        protected Integer doInBackground(SparseBooleanArray... params) {
            try {
                SparseBooleanArray pos = params[0];

                ListAdapter adapter = getListAdapter();

                Gravatar gravatar = new Gravatar();
                gravatar.setSize(getResources().getDimensionPixelSize(R.dimen.gravatar_save_size));
                gravatar.setRating(GravatarRating.GENERAL_AUDIENCES);
                gravatar.setDefaultImage(GravatarDefaultImage.IDENTICON);

                ContentResolver resolver = getActivity().getContentResolver();

                ArrayList<ContentValues> values = new ArrayList<ContentValues>();

                for (int i = 0; i < adapter.getCount(); i++) {
                    //Log.d(Tag.TAG, i + " " + pos.get(i));
                    if (!pos.get(i)) {
                        continue;
                    }
                    Cursor cursor = (Cursor)adapter.getItem(i);
                    long id = cursor.getLong(0);
                    String email = cursor.getString(3);
                    Log.d(Tag.TAG, "saving gravatar to " + id + " " + email);
                    byte[] jpg = gravatar.download(email);
                    ContentValues row = new ContentValues();
                    row.put(Data.RAW_CONTACT_ID, id);
                    row.put(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
                    row.put(Photo.PHOTO, jpg);
                    values.add(row);
                }
                return resolver.bulkInsert(
                        Data.CONTENT_URI,
                        values.toArray(new ContentValues[] {}));
            } catch (Exception e) {
                Log.w(Tag.TAG, "failed to insert", e);
                return 0;
            }
        }

        @Override
        protected void onPostExecute(Integer inserted) {
            Toast.makeText(getActivity(), getString(R.string.saved, inserted),
                    Toast.LENGTH_LONG).show();
        }

    }

}
