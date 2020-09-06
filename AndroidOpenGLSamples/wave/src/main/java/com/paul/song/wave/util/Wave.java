package com.paul.song.wave.util;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;

import androidx.annotation.ColorInt;

import com.paul.song.wave.BezierRenderer;

/**
 * Create by jhsong4 at 2020/8/30
 */
public class Wave {

    private BezierRenderer mRenderer;

    public Wave(GLSurfaceView glSurfaceView, @ColorInt int backgroundColor) {
        initView(glSurfaceView, backgroundColor);
    }

    private void initView(GLSurfaceView glSurfaceView, @ColorInt int backgroundColor) {
    // check if the system supports opengl es 2.0.
        Context context = glSurfaceView.getContext();
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;

        if (supportsEs2) {
            // Request an OpenGL ES 2.0 compatible context.
            glSurfaceView.setEGLContextClientVersion(2);

            // Set the renderer to our demo renderer, defined below.
            mRenderer = new BezierRenderer(glSurfaceView, backgroundColor);
            glSurfaceView.setRenderer(mRenderer);
            glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public void updateView(float[] amps) {
        mRenderer.setAmplitudes(amps);
    }
}
