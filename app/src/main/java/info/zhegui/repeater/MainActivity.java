package info.zhegui.repeater;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.net.URI;
import java.util.Comparator;
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
    private static final int REQUEST_GET_CONTENT = 101;
    private static SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
    protected void onDestroy() {
        super.onDestroy();
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
                    return FragmentList.newInstance();
                case 1:
                    return FragmentPlay.newInstance();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_GET_CONTENT) {
            if (resultCode == RESULT_OK) {
                Uri uri = intent.getData();
                String type = intent.getType();
//                LogHelper.i(TAG,"Pick completed: "+ uri + " "+type);
                if (uri != null) {
                    String path = uri.toString();
                    if (path.toLowerCase().startsWith("file://")) {
                        // Selected file/directory path is below
                        path = (new File(URI.create(path))).getAbsolutePath();
                    }

                }
            }

        }
    }

    public static class FragmentList extends Fragment {
        private final String LAST_PATH = "last_path", UPWARD = ".";
        private final int WHAT_LOADING = 101, WHAT_DONE = 102;
        private ArrayAdapter<String> adapter;
        private TextView tvDir, tvCount, tvLoading;
        private ListView listView;
        private boolean isLoading = true;
        private String newPath = File.separator;
        private Thread threadLoading;

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static FragmentList newInstance() {
            FragmentList fragment = new FragmentList();
            Bundle args = new Bundle();
            fragment.setArguments(args);
            return fragment;
        }

        public FragmentList() {
        }

        private Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                log("handlerMessage(" + msg + ")");
                switch (msg.what) {
                    case WHAT_DONE:
                        String[] tempArr = (String[]) msg.obj;
                        adapter.clear();
                        //add the dot item to go to the upper folder
                        if (!TextUtils.equals(newPath, File.separator)) {
                            adapter.add(UPWARD);
                        }
                        for (int i = 0; tempArr != null && i < tempArr.length; i++) {
                            String str = tempArr[i];
                            adapter.add(str);
                        }
                        adapter.sort(new Comparator<String>() {
                            @Override
                            public int compare(String lhs, String rhs) {
                                return lhs.compareTo(rhs);
                            }
                        });
                        adapter.notifyDataSetChanged();
                        tvLoading.setVisibility(View.GONE);

                        prefs.edit().putString(LAST_PATH, newPath).commit();
                        tvDir.setText(newPath);

                        String strCount = adapter.getCount() + "";
                        //exclude the dot item at position 0
                        if (!TextUtils.equals(newPath, File.separator)) {
                            strCount = (adapter.getCount() - 1) + "";
                        }
                        SpannableString spanString = new SpannableString(strCount);
                        ForegroundColorSpan span = new ForegroundColorSpan(Color.BLACK);
                        spanString.setSpan(span, 0, strCount.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        tvCount.setText(spanString);
                        tvCount.append("  items found");
                        break;
                    case WHAT_LOADING:
                        String strLoading = "loading";
                        for (int i = 0; i < msg.arg1; i++) {
                            strLoading += ".";
                        }
                        log(strLoading);
                        tvLoading.setText(strLoading);
                        break;
                }
            }
        };

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_list, container, false);
            tvDir = (TextView) rootView.findViewById(R.id.tv_dir);
            tvCount = (TextView) rootView.findViewById(R.id.tv_count);
            tvLoading = (TextView) rootView.findViewById(R.id.tv_loading);
            listView = (ListView) rootView.findViewById(R.id.listview);
            tvDir.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //write your own tiny file explorer
                }
            });


            adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1);
            listView.setAdapter(adapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String str = adapter.getItem(position);
                    String newDir = tvDir.getText().toString();
                    if (TextUtils.equals(str, UPWARD)) {
                        int lastSlash = newDir.lastIndexOf(File.separator);
                        if (lastSlash > 0)
                            newPath = newDir.substring(0, lastSlash);
                        else
                            newPath = File.separator; //root directory
                    } else {
                        if (TextUtils.equals(newDir, File.separator)) {
                            newPath = File.separator + str;
                        } else {
                            newPath += File.separator + str;
                        }
                    }
                    updateDirList();

                }
            });


            threadLoading = new Thread() {
                @Override
                public void run() {
                    int count = 0;
                    while (isLoading) {
                        Message msg = mHandler.obtainMessage(WHAT_LOADING, count++ % 6, 0);
                        msg.sendToTarget();
                        SystemClock.sleep(100);
                    }
                }
            };

            newPath = prefs.getString(LAST_PATH, File.separator);
            updateDirList();

            return rootView;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            isLoading = false;
        }

        private boolean updateDirList() {
            log("updateDirList(" + newPath + ")");
            final File currentDir = new File(newPath);
            if (currentDir.isDirectory()) {
                tvLoading.setVisibility(View.VISIBLE);
                if (!threadLoading.isAlive()) {
                    threadLoading.start();
                }
                new Thread() {
                    @Override
                    public void run() {

                        final String[] tempArr = currentDir.list();
                        Message msg = mHandler.obtainMessage(WHAT_DONE, tempArr);
                        msg.sendToTarget();

                    }
                }.start();


                return true;
            }
            return false;
        }
    }


    public static class FragmentPlay extends Fragment {

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static FragmentPlay newInstance() {
            FragmentPlay fragment = new FragmentPlay();
            Bundle args = new Bundle();
            fragment.setArguments(args);
            return fragment;
        }

        public FragmentPlay() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_play, container, false);
            return rootView;
        }
    }

    private static void log(String msg) {
        Log.e("MainActivity", msg);
    }
}
