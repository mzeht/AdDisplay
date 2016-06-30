package com.wpmac.addisplay.avtivity;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.widget.VideoView;

import com.wpmac.addisplay.R;

import java.util.Timer;
import java.util.TimerTask;

public class LaunchActivity extends Activity {
    private VideoView videoView;
    private Timer tExit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);
        initView();
        initData();
    }

    private void initData() {
        String uri = "android.resource://" + getPackageName() + "/" + R.raw.launch;
        videoView.setVideoURI(Uri.parse(uri));
        videoView.start();
    }

    private void initView() {

        videoView= (VideoView) findViewById(R.id.video);
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                tExit = new Timer();
                tExit.schedule(new TimerTask() {
                    @Override
                    public void run() {

                        Intent intent = new Intent();
                        intent.setClass(LaunchActivity.this, LoginActivity.class);
                        startActivity(intent);
                        LaunchActivity.this.finish();
                    }
                }, 2000);
            }
        });
    }


}
