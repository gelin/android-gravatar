package com.example.android.gravatar;

import android.app.Activity;
import android.app.ListFragment;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;

public class ViewContactsFragment extends ListFragment {

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        setListAdapter(createAdapter());
    }

    ListAdapter createAdapter() {
        Cursor cursor = getActivity().getContentResolver().query(
                ContactsContract.Data.CONTENT_URI,
                new String[] {  ContactsContract.Data._ID,
                                ContactsContract.Contacts.DISPLAY_NAME,
                                ContactsContract.CommonDataKinds.Email.ADDRESS },
                ContactsContract.CommonDataKinds.Email.ADDRESS + " is not null",
                null,
                ContactsContract.Contacts.DISPLAY_NAME);

        ListAdapter adapter = new SimpleCursorAdapter(
                getActivity(),
                android.R.layout.simple_list_item_1,
                cursor,
                new String[] { ContactsContract.Contacts.DISPLAY_NAME },
                new int[] { android.R.id.text1 },
                0);

        return adapter;
    }

}
