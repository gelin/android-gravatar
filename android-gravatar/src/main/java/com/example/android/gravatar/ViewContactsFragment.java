package com.example.android.gravatar;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.CursorAdapter;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;

import static android.provider.ContactsContract.Data;
import static android.provider.ContactsContract.Contacts;
import static android.provider.ContactsContract.CommonDataKinds.Email;
import static android.provider.ContactsContract.CommonDataKinds.Photo;

public class ViewContactsFragment extends ListFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ListAdapter adapter = new SimpleCursorAdapter(
                getActivity(),
                R.layout.person_list_item,
                null,
                new String[] {
                        Contacts.DISPLAY_NAME,
                        Email.ADDRESS },
                new int[] {
                        R.id.name,
                        R.id.email },
                0);
        setListAdapter(adapter);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(
                getActivity(),
                Data.CONTENT_URI,
                new String[] {
                        Data._ID,
                        Data.CONTACT_ID,
                        Contacts.DISPLAY_NAME,
                        Email.ADDRESS },
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
