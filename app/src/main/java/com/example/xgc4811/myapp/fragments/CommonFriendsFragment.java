package com.example.xgc4811.myapp.fragments;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.example.xgc4811.myapp.R;
import com.example.xgc4811.myapp.helper.AppHelper;
import com.example.xgc4811.myapp.services.BluetoothService;

import java.util.HashMap;
import java.util.Set;

public class CommonFriendsFragment extends ListFragment {

    ArrayAdapter<String> listAdapter;
    ListView mListView ;

    AppHelper mAppHelper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_common_friends, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getActivity().setTitle( "Common Friends List" );
        mListView = (ListView) view.findViewById( android.R.id.list );
        listAdapter = new ArrayAdapter<String>( getActivity(), android.R.layout.simple_list_item_1,0 );
        mListView.setAdapter(listAdapter);

        mAppHelper = new AppHelper(getContext());

        HashMap<String, Set<String>> commonfriends = mAppHelper.getCommonFriends();
        try{
            if ( commonfriends.size() > 0) {
                Set<String> c = commonfriends.get(AppHelper.COMMON_FRIENDS);

                for (String u : c) {
                    listAdapter.add(u);
                }

            }
        }catch (NullPointerException e){
            e.fillInStackTrace();
        }



    }

}
