package com.example.android.gravatar;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.CursorAdapter;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;

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
                        ContactsContract.Contacts.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Email.ADDRESS },
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
                ContactsContract.Data.CONTENT_URI,
                new String[] {  ContactsContract.Data._ID,
                        ContactsContract.Contacts.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Email.ADDRESS },
                ContactsContract.CommonDataKinds.Email.ADDRESS + " is not null" +
                        " AND " + ContactsContract.Data.MIMETYPE + " = '" +
                        ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE + "'",
                null,
                ContactsContract.Contacts.DISPLAY_NAME);
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
