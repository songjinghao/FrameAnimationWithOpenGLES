package com.paul.song.androidopenglsamples;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.paul.song.frameanimation.listener.AnimationStateChangedListener;
import com.paul.song.frameanimation.gles30.FrameAnimationView;

public class MainActivity extends AppCompatActivity {

    private FrameAnimationView mGLSurfaceView;
    private String animPath = "assets/Avatar_ETC2.zip";
    private String animPath2 = "assets/Test_ETC2.zip";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mGLSurfaceView = findViewById(R.id.av30);
        //初始化GLSurfaceView
//        mGLSurfaceView = new FrameAnimationView(this);
//        setContentView(mGLSurfaceView);
//        mGLSurfaceView.setScaleType(FrameAnimationView.ScaleType.CENTER_INSIDE);

        mGLSurfaceView.requestFocus();//获取焦点
        mGLSurfaceView.setFocusableInTouchMode(true);//设置为可触控
        mGLSurfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mGLSurfaceView.isPlay()) {
//                    mGLSurfaceView.setAnimation(animPath, 50);
//                    mGLSurfaceView.setImageSource("");
                    mGLSurfaceView.start();
                }
            }
        });
        mGLSurfaceView.setAnimationStateChangedListener(new AnimationStateChangedListener() {
            @Override
            public void onStateChanged(int lastState, int nowState) {
                Log.d("sjh", "lastState = " + lastState + " nowState = " + nowState);
            }
        });

        findViewById(R.id.stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGLSurfaceView.stop();
            }
        });

        findViewById(R.id.playAnother).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGLSurfaceView.setImageSource(animPath);
                mGLSurfaceView.start();
            }
        });

        findViewById(R.id.playAnother2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGLSurfaceView.setImageSource(animPath2);
                mGLSurfaceView.start();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGLSurfaceView.onPause();
    }
}
