package info.zhegui.repeater;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements ActionBar.TabListener {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;
    protected SharedPreferences prefs;
    protected FragmentList fragmentList;
    protected FragmentPlay fragmentPlay;
    MainService mService;
    boolean mBound = false;


    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            log("onServiceConnected()");
            // We've bound to MainService, cast the IBinder and get LocalService instance
            MainService.LocalBinder binder = (MainService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;

            mService.isActivityVisible = true;

            mService.stopForeground(true);
            fragmentPlay.updateViews();
            if (mService.isMediaPlaying()) {
                mViewPager.setCurrentItem(1);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            log("onServiceDisconnected()");
            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log("onCreate()");
        setContentView(R.layout.activity_main);

        prefs = getPreferences(MODE_PRIVATE);

        // Set up the action bar.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        log("onStart()");

        new Thread() {
            @Override
            public void run() {

                startService(new Intent(MainActivity.this, MainService.class));
                SystemClock.sleep(1000);

                while (fragmentPlay == null || fragmentList == null) SystemClock.sleep(50);

                // Bind to MainService
                Intent intent = new Intent(MainActivity.this, MainService.class);
                MainActivity.this.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
                log("bindService()");
            }
        }.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        log("onStop()");

        // Unbind from the service
        if (mBound) {
            mService.isActivityVisible = false;
            if (!mService.isMediaPlaying()) {
                stopService(new Intent(this, MainService.class));
            } else {
                startForeground();
            }
            unbindService(mConnection);
            log("unbindService(" + mConnection + ")");
            mBound = false;
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        log("onDestroy()");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            switch (position) {
                case 0:
                    if (fragmentList == null) {
                        fragmentList = FragmentList.newInstance();
                    }
                    return fragmentList;
                case 1:
                    if (fragmentPlay == null) {
                        fragmentPlay = FragmentPlay.newInstance();
                    }
                    return fragmentPlay;
            }

            return null;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return "List";
                case 1:
                    return "play";
            }
            return null;
        }
    }

    public void startForeground() {
        if (mBound) {
            String songName = new File(mService.path).getName();
// assign the song name to songName
            PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                    new Intent(getApplicationContext(), MainActivity.class),
                    PendingIntent.FLAG_UPDATE_CURRENT);
            Notification notification = new Notification();
//            notification.tickerText = text;
            notification.icon = android.R.drawable.ic_media_play;
            notification.flags |= Notification.FLAG_ONGOING_EVENT;
            notification.setLatestEventInfo(getApplicationContext(), "MusicPlayerSample",
                    "Playing: " + songName, pi);
            mService.startForeground(mService.NOTIFICATION_ID, notification);
        }
    }

    @Override
    public void finish() {
        super.finish();
    }

    protected void toast(String msg) {
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
    }

    private void log(String msg) {
        Log.e("MainActivity", msg);
    }

}
