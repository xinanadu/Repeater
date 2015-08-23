package info.zhegui.repeater;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.Comparator;

/**
 * Created by Administrator on 2015/8/22.
 */
public class FragmentList extends Fragment {
    private final String LAST_PATH = "last_path", UPWARD = "..";
    private final int WHAT_LOADING = 101, WHAT_DONE = 102;
    private ArrayAdapter<String> adapter;
    private TextView tvDir, tvCount, tvLoading;
    private ListView listView;
    private boolean isLoading = true;
    private String newPath = File.separator;
    private Thread threadLoading;
    private MainActivity mActivity;

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
//                log("handlerMessage(" + msg + ")");
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

                    mActivity.prefs.edit().putString(LAST_PATH, newPath).commit();
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
//                        log(strLoading);
                    tvLoading.setText(strLoading);
                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = (MainActivity) getActivity();
    }

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
                String curPath = newPath;

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
                        if (new File(newPath + File.separator + str).isDirectory())
                            newPath += File.separator + str;

                    }
                }


                if (TextUtils.equals(newPath, curPath)) {
                    if (str.toLowerCase().endsWith("mp3")) {
                        if (mActivity.fragmentPlay != null)
                            mActivity.fragmentPlay.play(newPath + File.separator + str);
                    }
                } else {
                    updateDirList();
                }
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

        newPath = mActivity.prefs.getString(LAST_PATH, File.separator);
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
            if (threadLoading.getState() == Thread.State.NEW) {
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

    private void log(String msg) {
        Log.e("FragmentList", msg);
    }
}
