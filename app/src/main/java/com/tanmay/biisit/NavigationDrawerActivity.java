package com.tanmay.biisit;

import android.Manifest;
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
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.ResultCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.tanmay.biisit.myMusic.MyMusicFragment;

public class NavigationDrawerActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int RC_SIGN_IN = 111;
    private static final String LOG_TAG = NavigationDrawerActivity.class.getSimpleName();
    private static final String MY_MUSIC_STATE_KEY = "MY_MUSIC_STATE_KEY";
    private static final String SOUNDCLOUD_FRAGMENT_TAG = "SoundCloud";
    private static final String MY_MUSIC_FRAGMENT_TAG = "MyMusic";
    NavigationView mNavigationView;

    MyMusicFragment myMusicFragment;
    private Fragment.SavedState mMyMusicFragmentState;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation_drawer);

        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);

        if (savedInstanceState == null) {
            Log.i(LOG_TAG, "onCreate: null savedState");
            myMusicFragment = new MyMusicFragment();
            onNavigationItemSelected(mNavigationView.getMenu().getItem(0));
        }
        else {
            Log.i(LOG_TAG, "onCreate: Restoring from savedState");
            myMusicFragment = (MyMusicFragment) getSupportFragmentManager().findFragmentByTag(MY_MUSIC_STATE_KEY);
//            myMusicFragment = (MyMusicFragment) getSupportFragmentManager().getFragment(savedInstanceState, MY_MUSIC_STATE_KEY);
            if (myMusicFragment == null) {
                myMusicFragment = new MyMusicFragment();
                mMyMusicFragmentState = (Fragment.SavedState) savedInstanceState.getParcelable(MY_MUSIC_STATE_KEY);
                myMusicFragment.setInitialSavedState(mMyMusicFragmentState);
            }
        }
        updateUserDisplay();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(MY_MUSIC_STATE_KEY, mMyMusicFragmentState);
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
            signInOutMenuItem.setTitle("Sign Out");
        }
        else {
            if (mNavigationView == null)
                return;
            View header = mNavigationView.getHeaderView(0);
            ((TextView)header.findViewById(R.id.user_email)).setText("");
            ((TextView)header.findViewById(R.id.user_name)).setText("Biisit User");
            MenuItem signInOutMenuItem = mNavigationView.getMenu().findItem(R.id.sign_in_out);
            signInOutMenuItem.setTitle("Sign In");
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

        //remove this check?
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Toast.makeText(this, "Gimme permission", Toast.LENGTH_SHORT).show();
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

        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.my_music) {
//                MyMusicFragment fragment = new MyMusicFragment();
//                getSupportFragmentManager().beginTransaction().replace(R.id.content_navigation_drawer, new MyMusicFragment(), MY_MUSIC_STATE_KEY).commit();
//            }
//            else {
                getSupportFragmentManager().beginTransaction().replace(R.id.content_navigation_drawer, myMusicFragment, MY_MUSIC_FRAGMENT_TAG).commit();
//            }
        } else if (id == R.id.soundcloud) {
            mMyMusicFragmentState = getSupportFragmentManager().saveFragmentInstanceState(myMusicFragment);
            getSupportFragmentManager().beginTransaction().replace(R.id.content_navigation_drawer, new Fragment(), SOUNDCLOUD_FRAGMENT_TAG).commit();
        } else if (id == R.id.sign_in_out) {
            accountStuff = true;
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                AuthUI.getInstance()
                        .signOut(this)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                MenuItem signInOutMenuItem = mNavigationView.getMenu().findItem(R.id.sign_in_out);
                                signInOutMenuItem.setTitle("Sign In");
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

        showSnackbar("Unexpected response from Sign-in");
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
                showSnackbar("Sign in was cancelled");
                return;
            }

            if (response.getErrorCode() == ErrorCodes.NO_NETWORK) {
                showSnackbar("Unable to sign in. Please check internet connection.");
                return;
            }

            if (response.getErrorCode() == ErrorCodes.UNKNOWN_ERROR) {
                showSnackbar("Error while signing in");
                return;
            }
        }

        showSnackbar("Unexpected response from Sign-in");
    }

    @MainThread
    private void showSnackbar(String errorMessageRes) {
        Snackbar.make(findViewById(R.id.drawer_layout), errorMessageRes, Snackbar.LENGTH_LONG).show();
    }
}
