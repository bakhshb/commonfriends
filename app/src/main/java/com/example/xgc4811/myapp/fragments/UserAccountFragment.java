package com.example.xgc4811.myapp.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.xgc4811.myapp.R;
import com.example.xgc4811.myapp.helper.AppHelper;
import com.facebook.Profile;
import com.facebook.login.widget.ProfilePictureView;

import java.util.HashMap;


public class UserAccountFragment extends Fragment {

    private TextView mUserName;
    private TextView mUserToken;
    private ProfilePictureView profilePictureView;
    private AppHelper mAppHelper;
    public UserAccountFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setTitle( "User Account Detail" );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate( R.layout.fragment_useraccount, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init( view );
    }

    public void init(View view){
        mUserName = (TextView) view.findViewById(R.id.userName );
        mUserToken = (TextView) view.findViewById( R.id.userToken );
        profilePictureView = (ProfilePictureView) view.findViewById(R.id.profile_picture_view);
        profilePictureView.setProfileId(getArguments().getString("image"));
        mAppHelper = new AppHelper( getContext() );

        mUserName.setText(getArguments().getString("name"));
        // get user data from session
        HashMap<String, String> user = mAppHelper.getUserDetails();
        mUserToken.setText( user.get( AppHelper.USER_TOKEN ) );
    }

    public static UserAccountFragment newInstance (Profile profile){
        UserAccountFragment user_account_frag = new UserAccountFragment();
        Bundle args = new Bundle();
        args.putString("name", profile.getName());
        args.putString("image", profile.getId());
        user_account_frag.setArguments(args);
        return  user_account_frag;
    }
}
