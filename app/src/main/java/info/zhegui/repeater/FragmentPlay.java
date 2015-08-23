package info.zhegui.repeater;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Created by Administrator on 2015/8/22.
 */
public class FragmentPlay extends Fragment {

    private SeekBar mSeekBar;
    private TextView tvCurrentTime, tvTotalTime, tvCurrentItem, tvPieceStart, tvPieceLength, tvPieceEnd;
    private Button btnPlay, btnSetStart, btnSetEnd;
    private final int WHAT_UPDATE_POSITION = 101;
    private MainActivity mActivity;
    private String path;
    private DBHelper mDBHelper;
    private long pieceStartTime;
    private int SCREEN_WIDTH;
    private RelativeLayout layoutPiece;
    private String ACTION_MEDIA_STATE_CHANGED = "info.zhegui.repeater.action_media_state_changed";

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
//            log("handlerMessage(" + msg + ")");
            switch (msg.what) {
                case WHAT_UPDATE_POSITION:
                    updateViewsFrequently();
                    break;
            }
        }
    };

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            log("onReceive(" + context + "," + intent + ")");
            if (TextUtils.equals(intent.getAction(), ACTION_MEDIA_STATE_CHANGED)) {
                updateViews();
            }
        }
    };

    public static FragmentPlay newInstance() {
        FragmentPlay fragment = new FragmentPlay();
        Bundle args = new Bundle();
        fragment.setArguments(args);


        return fragment;
    }

    public FragmentPlay() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = (MainActivity) getActivity();
//            mDBHelper=new DBHelper(mActivity);

    }

    @Override
    public void onStart() {
        super.onStart();
        mActivity.registerReceiver(receiver, new IntentFilter(ACTION_MEDIA_STATE_CHANGED));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_play, container, false);
        SCREEN_WIDTH = mActivity.getResources().getDisplayMetrics().widthPixels;
        mSeekBar = (SeekBar) rootView.findViewById(R.id.seekBar);
        tvCurrentTime = (TextView) rootView.findViewById(R.id.tv_current_time);
        tvTotalTime = (TextView) rootView.findViewById(R.id.tv_total_time);
        tvCurrentItem = (TextView) rootView.findViewById(R.id.tv_current_item);
        btnPlay = (Button) rootView.findViewById(R.id.btn_play);
        btnPlay.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                log("mActivity.mBound:" + mActivity.mBound);
                if (mActivity.mBound) {
                    if (mActivity.mService.isMediaPaused()) {
                        mActivity.mService.playOrPause();
                        btnPlay.setText(R.string.action_pause);
                    } else if (mActivity.mService.isMediaPlaying()) {
                        mActivity.mService.playOrPause();
                        btnPlay.setText(R.string.action_play);

                    } else {
                        play(path);
                        btnPlay.setText(R.string.action_pause);
                    }
                } else {
                    btnPlay.setText(R.string.action_play);
                }
            }

        });
        btnSetStart = (Button) rootView.findViewById(R.id.btn_set_as_start);
        btnSetStart.setOnClickListener(new View.OnClickListener() {

                                           @Override
                                           public void onClick(View v) {
                                               if (mActivity.mBound && mActivity.mService.isMediaPlaying()) {
                                                   pieceStartTime = mActivity.mService.mMediaPlayer.getCurrentPosition();
                                                   tvPieceStart.setText(new Utils().formatTime(pieceStartTime));
                                                   tvPieceLength.setWidth(0);
                                                   tvPieceEnd.setVisibility(View.GONE);

                                                   RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) layoutPiece.getLayoutParams();
                                                   params.leftMargin = (int) (pieceStartTime * 1.00 / mActivity.mService.mMediaPlayer.getDuration() * SCREEN_WIDTH);
                                                   layoutPiece.setLayoutParams(params);
                                                   layoutPiece.setVisibility(View.VISIBLE);

                                               } else {
                                                   mActivity.toast("clickable only when playing");
                                               }
                                           }
                                       }

        );
        btnSetEnd = (Button) rootView.findViewById(R.id.btn_set_as_end);
        btnSetEnd.setOnClickListener(new View.OnClickListener() {

                                         @Override
                                         public void onClick(View v) {
                                             if (mActivity.mBound && mActivity.mService.isMediaPlaying()) {
                                                 pieceStartTime = 0;
                                                 tvPieceEnd.setText(new Utils().formatTime(mActivity.mService.mMediaPlayer.getCurrentPosition()));
                                                 tvPieceEnd.setVisibility(View.VISIBLE);
                                             }
                                         }
                                     }

        );
        tvPieceStart = (TextView) rootView.findViewById(R.id.tv_start_time);
        tvPieceLength = (TextView) rootView.findViewById(R.id.tv_piece_time);
        tvPieceEnd = (TextView) rootView.findViewById(R.id.tv_end_time);
        layoutPiece = (RelativeLayout) rootView.findViewById(R.id.layout_set_new_piece);

        resetViews();

        return rootView;
    }

    private void updateViewsFrequently() {
        if (mSeekBar != null && mActivity.mBound && mActivity.mService.canGetPosition()) {
            mSeekBar.setProgress(mActivity.mService.mMediaPlayer.getCurrentPosition());
            tvCurrentTime.setText(new Utils().formatTime(mActivity.mService.mMediaPlayer.getCurrentPosition()));
            if (pieceStartTime > 0) {
                if (tvPieceLength != null) {
                    int width = (int) (SCREEN_WIDTH * ((mActivity.mService.mMediaPlayer.getCurrentPosition() - pieceStartTime) * 1.00 / mActivity.mService.mMediaPlayer.getDuration()));
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) tvPieceLength.getLayoutParams();
                    params.width = width;
                    tvPieceLength.setLayoutParams(params);
                }
            }
        }
    }

    /**
     * this is called after service bound
     */
    public void updateViews() {
        if (mActivity.mBound) {
            if (mActivity.mService.isMediaPlaying() || mActivity.mService.canGetPosition()) {
                path = mActivity.mService.path;
                btnPlay.setText(R.string.action_pause);
                mSeekBar.setMax(mActivity.mService.mMediaPlayer.getDuration());
                tvTotalTime.setText(new Utils().formatTime(mActivity.mService.mMediaPlayer.getDuration()));
                tvCurrentItem.setText(path);

                new Thread() {
                    @Override
                    public void run() {
                        while (mActivity.mBound && mActivity.mService.isMediaPlaying() || mActivity.mService.canGetPosition()) {
                            mHandler.sendEmptyMessage(WHAT_UPDATE_POSITION);
                            SystemClock.sleep(500L);
                        }
                    }
                }.start();
            }
        } else {
            btnPlay.setText(R.string.action_play);

        }
    }

    public void play(String path) {
        log("play(" + path + ")");
        layoutPiece.setVisibility(View.INVISIBLE);
        this.path = path;
        resetViews();
        tvCurrentItem.setText(path);

        mActivity.mService.clickOnBtnPlay(path);
        mActivity.mViewPager.setCurrentItem(1);
    }

    private void resetViews() {
        tvCurrentTime.setText(R.string.default_time);
        tvTotalTime.setText(R.string.default_time);
        btnPlay.setText(R.string.action_play);
    }

    @Override
    public void onStop() {
        super.onStop();
        mActivity.unregisterReceiver(receiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void log(String msg) {
        Log.e("FragmentPlay", msg);
    }


    public void save(long start, long end, String path, String alias) {
        if (mDBHelper != null) {

        }
    }
}