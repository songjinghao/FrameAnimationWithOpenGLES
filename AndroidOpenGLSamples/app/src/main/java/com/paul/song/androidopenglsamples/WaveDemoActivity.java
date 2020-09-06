package com.paul.song.androidopenglsamples;

import androidx.appcompat.app.AppCompatActivity;

import android.opengl.GLSurfaceView;
import android.os.Bundle;

import com.paul.song.wave.util.Wave;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class WaveDemoActivity extends AppCompatActivity {

    private Wave mWave;
    private GLSurfaceView glSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wave_demo);

        glSurfaceView = (GLSurfaceView) findViewById(R.id.gl_wave);
        mWave = new Wave(glSurfaceView, getResources().getColor(R.color.background));
    }

    @Override
    protected void onResume() {
        super.onResume();
        glSurfaceView.onResume();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Random random = new Random();
//                float amp1 = 1.0f, amp2 = 0.5f, amp3 = 0.8f, amp4 = 0.3f, amp5 = 0f;
                float amp1 = random.nextFloat(), amp2 = random.nextFloat(), amp3 = random.nextFloat(), amp4 = random.nextFloat(), amp5 = random.nextFloat();
                float pStep = 0.02f;
                float nStep = -pStep;
                float step1 = pStep, step2 = pStep, step3 = pStep, step4 = pStep, step5 = pStep;
                while (true) {
                    amp1 += step1;
                    if (amp1 > 1.0f) {
                        step1 = nStep;
                        amp1 = 1.0f;
                    } else if (amp1 < 0f) {
                        step1 = pStep;
                        amp1 = 0f;
                    }

                    amp2 += step2;
                    if (amp2 > 1.0f) {
                        step2 = nStep;
                        amp2 = 1.0f;
                    } else if (amp2 < 0f) {
                        step2 = pStep;
                        amp2 = 0f;
                    }

                    amp3 += step3;
                    if (amp3 > 1.0f) {
                        step3 = nStep;
                        amp3 = 1.0f;
                    } else if (amp3 < 0f) {
                        step3 = pStep;
                        amp3 = 0f;
                    }

                    amp4 += step4;
                    if (amp4 > 1.0f) {
                        step4 = nStep;
                        amp4 = 1.0f;
                    } else if (amp4 < 0f) {
                        step4 = pStep;
                        amp4 = 0f;
                    }

                    amp5 += step5;
                    if (amp5 > 1.0f) {
                        step5 = nStep;
                        amp5 = 1.0f;
                    } else if (amp5 < 0f) {
                        step5 = pStep;
                        amp5 = 0f;
                    }

                    try {
                        TimeUnit.MILLISECONDS.sleep(16);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mWave.updateView(new float[] {amp1, amp2, amp3, amp4, amp5});
                }
            }
        }).start();

    }

    @Override
    protected void onPause() {
        super.onPause();
        glSurfaceView.onPause();
    }
}