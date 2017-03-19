package com.tanmay.biisit;

import android.Manifest;
import android.app.SearchManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.ResultCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.tanmay.biisit.myMusic.MyMusicFragment;
import com.tanmay.biisit.soundCloud.SoundCloudFragment;

public class NavigationDrawerActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int RC_SIGN_IN = 111;
    private static final String LOG_TAG = NavigationDrawerActivity.class.getSimpleName();
    private static final String MY_MUSIC_STATE_KEY = "MY_MUSIC_STATE_KEY";
    private static final String SOUNDCLOUD_STATE_KEY = "SOUNDCLOUD_STATE_KEY";
    private static final String SOUNDCLOUD_FRAGMENT_TAG = "SoundCloud";
    private static final String MY_MUSIC_FRAGMENT_TAG = "MyMusic";
    private static final String TITLE_STATE_KEY = "TITLE_STATE_KEY";

    public static final String FRAGMENT_TO_LAUNCH = "FRAGMENT_TO_LAUNCH";
    public static final int MY_MUSIC_FRAGMENT = 1;
    public static final int SOUNDCLOUD_FRAGMENT = 2;

    NavigationView mNavigationView;

    MyMusicFragment mMyMusicFragment;
    private Fragment.SavedState mMyMusicFragmentState;

    SoundCloudFragment mSoundCloudFragment;
    private Fragment.SavedState mSoundCloudFragmentState;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermission();
        setContentView(R.layout.activity_navigation_drawer);

        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);

        if (savedInstanceState == null) {
//            Log.i(LOG_TAG, "onCreate: null savedState");
            mMyMusicFragment = new MyMusicFragment();
            mSoundCloudFragment = new SoundCloudFragment();
            onNavigationItemSelected(mNavigationView.getMenu().getItem(0));
        }
        else {
//            Log.i(LOG_TAG, "onCreate: Restoring from savedState");
            mMyMusicFragment = (MyMusicFragment) getSupportFragmentManager().findFragmentByTag(MY_MUSIC_FRAGMENT_TAG);
            mSoundCloudFragment = (SoundCloudFragment) getSupportFragmentManager().findFragmentByTag(SOUNDCLOUD_FRAGMENT_TAG);
//            mMyMusicFragment = (MyMusicFragment) getSupportFragmentManager().getFragment(savedInstanceState, MY_MUSIC_STATE_KEY);
            if (mMyMusicFragment == null) {
                mMyMusicFragment = new MyMusicFragment();
                mMyMusicFragmentState = savedInstanceState.getParcelable(MY_MUSIC_STATE_KEY);
                mMyMusicFragment.setInitialSavedState(mMyMusicFragmentState);
            }
            if (mSoundCloudFragment == null) {
                mSoundCloudFragment = new SoundCloudFragment();
                mSoundCloudFragmentState = savedInstanceState.getParcelable(SOUNDCLOUD_STATE_KEY);
                mSoundCloudFragment.setInitialSavedState(mSoundCloudFragmentState);
            }
            setTitle(savedInstanceState.getString(TITLE_STATE_KEY));
        }
        if (getIntent() != null)
            handleIntent(getIntent());
        updateUserDisplay();
    }

    private void checkPermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                showSnackbar(getString(R.string.permission_explanation_snackbar_message));
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        1);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
//            Log.i(LOG_TAG, "handleIntent: SEARCH intent got");
            String query = intent.getStringExtra(SearchManager.QUERY);
            if (mSoundCloudFragment != null){
                mSoundCloudFragment.handleSearch(query);
            }
        }
        else if (intent.hasExtra(FRAGMENT_TO_LAUNCH)){
            switch (intent.getIntExtra(FRAGMENT_TO_LAUNCH, -1)){
                case MY_MUSIC_FRAGMENT:{
                    onNavigationItemSelected(mNavigationView.getMenu().getItem(0));
                    break;
                }
                case SOUNDCLOUD_FRAGMENT:{
                    onNavigationItemSelected(mNavigationView.getMenu().getItem(1));
                    break;
                }
                default:
            }

        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(MY_MUSIC_STATE_KEY, mMyMusicFragmentState);
        outState.putParcelable(SOUNDCLOUD_STATE_KEY, mSoundCloudFragmentState);
        outState.putString(TITLE_STATE_KEY, getTitle().toString());
    }

    private void updateUserDisplay(){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Name, email address, and profile photo Url
            String name = user.getDisplayName();
            String email = user.getEmail();

            if (mNavigationView == null)
                return;
            View header = mNavigationView.getHeaderView(0);
            ((TextView)header.findViewById(R.id.user_email)).setText(email);
            ((TextView)header.findViewById(R.id.user_name)).setText(name);
            MenuItem signInOutMenuItem = mNavigationView.getMenu().findItem(R.id.sign_in_out);
            signInOutMenuItem.setTitle(R.string.sign_out_title);
        }
        else {
            if (mNavigationView == null)
                return;
            View header = mNavigationView.getHeaderView(0);
            ((TextView)header.findViewById(R.id.user_email)).setText("");
            ((TextView)header.findViewById(R.id.user_name)).setText(R.string.logged_out_user_display_name);
            MenuItem signInOutMenuItem = mNavigationView.getMenu().findItem(R.id.sign_in_out);
            signInOutMenuItem.setTitle(R.string.sign_in_title);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        boolean accountStuff = false;

        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.my_music) {
//            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                checkPermission();
                return false;
            }
            try {

                mSoundCloudFragmentState = getSupportFragmentManager().saveFragmentInstanceState(mSoundCloudFragment);
            }catch (IllegalStateException i){
                Log.w(LOG_TAG, "onNavigationItemSelected: fragment was not present with frag manager");
            }
            getSupportFragmentManager().beginTransaction().replace(R.id.content_navigation_drawer, mMyMusicFragment, MY_MUSIC_FRAGMENT_TAG).commit();
        }
        else if (id == R.id.soundcloud) {
//            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            try {
                mMyMusicFragmentState = getSupportFragmentManager().saveFragmentInstanceState(mMyMusicFragment);
            }
            catch (IllegalStateException i){
                Log.w(LOG_TAG, "onNavigationItemSelected: fragment was not present with frag manager");
            }
            getSupportFragmentManager().beginTransaction().replace(R.id.content_navigation_drawer, mSoundCloudFragment, SOUNDCLOUD_FRAGMENT_TAG).commit();
        }
        else if (id == R.id.sign_in_out) {
            accountStuff = true;
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                AuthUI.getInstance()
                        .signOut(this)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                MenuItem signInOutMenuItem = mNavigationView.getMenu().findItem(R.id.sign_in_out);
                                signInOutMenuItem.setTitle(getString(R.string.sign_in_title));
                                updateUserDisplay();
                            }
                        });
            }
            else {
                startActivityForResult(
                        AuthUI.getInstance().createSignInIntentBuilder()
                            .setTheme(R.style.FirebaseOrangeTheme)
//                                .setLogo(R.mipmap.ic_launcher)
//                            .setProviders(Arrays.asList(new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build()))
//                            .setTosUrl(getSelectedTosUrl())
                                .setIsSmartLockEnabled(true)
                                .setAllowNewEmailAccounts(true)
//                            .setAllowNewEmailAccounts(mAllowNewEmailAccounts.isChecked())
                                .build(),
                        RC_SIGN_IN);
            }
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        if (! accountStuff) {
            item.setChecked(true);
            setTitle(item.getTitle());
        }
        else {
            return false;
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            handleSignInResponse(resultCode, data);
            return;
        }

        showSnackbar(getString(R.string.sign_in_snackbar_unexpected_response));
    }

    @MainThread
    private void handleSignInResponse(int resultCode, Intent data) {
        IdpResponse response = IdpResponse.fromResultIntent(data);

        // Successfully signed in
        if (resultCode == ResultCodes.OK) {
            updateUserDisplay();
            return;
        } else {
            // Sign in failed
            if (response == null) {
                // User pressed back button
                showSnackbar(getString(R.string.sign_in_snackbar_cancelled));
                return;
            }

            if (response.getErrorCode() == ErrorCodes.NO_NETWORK) {
                showSnackbar(getString(R.string.sign_in_snackbar_network_error));
                return;
            }

            if (response.getErrorCode() == ErrorCodes.UNKNOWN_ERROR) {
                showSnackbar(getString(R.string.sign_in_snackbar_generic_error));
                return;
            }
        }

        showSnackbar(getString(R.string.sign_in_snackbar_unexpected_response));
    }

    @MainThread
    public void showSnackbar(String errorMessageRes) {
        Snackbar.make(findViewById(R.id.drawer_layout), errorMessageRes, Snackbar.LENGTH_LONG).show();
    }
}
