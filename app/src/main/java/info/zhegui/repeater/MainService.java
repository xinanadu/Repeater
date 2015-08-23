package info.zhegui.repeater;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2015/8/16.
 */
public class MainService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, AudioManager.OnAudioFocusChangeListener {
    @Nullable
    public MediaPlayer mMediaPlayer = null;
    public String path;
    protected SharedPreferences prefs;
    private final String LAST_POSITION = "last_position";
    private MediaPlayerState mMediaPlayerState = MediaPlayerState.END;
    private int lastPosition;
    /**
     * this flag is used for the service to determine whether to stop itself
     */
    public boolean isActivityVisible = false;
    public List<String> listMedia = new ArrayList<String>();

    public final int NOTIFICATION_ID = 201;
    private String ACTION_MEDIA_STATE_CHANGED = "info.zhegui.repeater.action_media_state_changed";

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        MainService getService() {
            // Return this instance of LocalService so clients can call public methods
            return MainService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        log("onCreate()");

        prefs = getSharedPreferences(MainService.class.getSimpleName(), MODE_PRIVATE);
        lastPosition = prefs.getInt(LAST_POSITION, 0);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log("onStartCommand(" + intent + ", " + flags + ", " + startId + ")");
        return START_NOT_STICKY;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {

        mMediaPlayerState = MediaPlayerState.PLAY_BACK_COMPLETED;
        if (listMedia.size() == 0) {
            if (!isActivityVisible) {
                stopSelf();
            }
        } else {
            listMedia.remove(0);
            clickOnBtnPlay(listMedia.get(0));
        }
    }


    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        log("error happened");
        mMediaPlayerState = MediaPlayerState.ERROR;
        toast("error happened(" + what + "," + extra + ")");
        mp.release();
        mMediaPlayer = null;
        mMediaPlayerState = MediaPlayerState.END;

        broadcastMediaStateChanged();

        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mMediaPlayerState = MediaPlayerState.PREPARED;
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);

        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            // could not get audio focus.
        } else {
            if (lastPosition > 0) {
                mp.seekTo(lastPosition);
                lastPosition = 0;
                prefs.edit().remove(LAST_POSITION).commit();
            }
            mMediaPlayer.start();
            mMediaPlayerState = MediaPlayerState.STARTED;
            broadcastMediaStateChanged();

            startThreadSavePosition();
        }
    }

    private void broadcastMediaStateChanged() {
        Intent intent = new Intent(ACTION_MEDIA_STATE_CHANGED);
        sendBroadcast(intent);
    }

    protected void clickOnBtnPlay(String newPath) {
        if (path == null) {
            path = newPath;
            playNew();
        } else {
            playOrPause();
        }

    }

    public boolean isMediaPlaying() {
        if (mMediaPlayer != null && (mMediaPlayerState == MediaPlayerState.STARTED)) {
            try {
                return mMediaPlayer.isPlaying();
            } catch (IllegalStateException e) {
                log(">>>" + e.getMessage());
                return false;
            }
        }

        return false;
    }

    public boolean isMediaPaused() {
        return mMediaPlayer != null && (mMediaPlayerState == MediaPlayerState.PAUSED);
    }

    public boolean canGetPosition() {
//        log("mMediaPlayerState:" + mMediaPlayerState);
        if (mMediaPlayer != null && (mMediaPlayerState == MediaPlayerState.PREPARED || mMediaPlayerState == MediaPlayerState.STARTED || mMediaPlayerState == MediaPlayerState.PAUSED || mMediaPlayerState == MediaPlayerState.STOPED || mMediaPlayerState == MediaPlayerState.PLAY_BACK_COMPLETED)) {
            try {
                mMediaPlayer.getCurrentPosition();
                return true;
            } catch (IllegalStateException e) {
                log(">>>" + e.getMessage());
                return false;
            }
        }
        ;

        return false;
    }

    protected void playOrPause() {
        if (mMediaPlayer != null) {
            if (mMediaPlayerState == MediaPlayerState.STARTED) {
                mMediaPlayer.pause();

                mMediaPlayerState = MediaPlayerState.PAUSED;
            } else if (mMediaPlayerState == MediaPlayerState.PREPARED || mMediaPlayerState == MediaPlayerState.PAUSED || mMediaPlayerState == MediaPlayerState.PLAY_BACK_COMPLETED) {
                mMediaPlayer.start();

                mMediaPlayerState = MediaPlayerState.STARTED;


                startThreadSavePosition();
            }

        }
    }

    private void startThreadSavePosition() {
        new Thread() {
            @Override
            public void run() {
                while (isMediaPlaying()) {
                    prefs.edit().putInt(LAST_POSITION, mMediaPlayer.getCurrentPosition()).commit();
                    log("save current position");
                    SystemClock.sleep(10000);
                }
            }
        }.start();
    }

    protected void playNew() {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
        } else {
            mMediaPlayer = new MediaPlayer();
        }
        mMediaPlayerState = MediaPlayerState.IDLE;
        try {
            mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnErrorListener(this);
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setDataSource(path);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.prepareAsync();

            mMediaPlayerState = MediaPlayerState.PREPARING;

        } catch (Exception e) {
            e.printStackTrace();
            log("===>>>>" + e.getMessage());
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (focusChange != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            if (mMediaPlayer != null && mMediaPlayerState == MediaPlayerState.STARTED) {
                mMediaPlayer.pause();

                mMediaPlayerState = MediaPlayerState.PAUSED;
            }
            broadcastMediaStateChanged();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        log("onDestroy()");
        mMediaPlayerState = MediaPlayerState.END;
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;

        }
    }

    private void log(String msg) {
        Log.e("MainService", msg);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }


}
