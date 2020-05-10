package com.paul.song.frameanimation.gles30;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PixelFormat;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import com.paul.song.frameanimation.R;
import com.paul.song.frameanimation.listener.AnimationStateChangedListener;
import com.paul.song.frameanimation.utils.MatrixState;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Copyright (C)
 * <p>
 * FileName: FrameAnimationView  Opengl es 3.0 版本
 * <p>
 * Author: Paul Song
 * <p>
 * Date: 2020/5/5 22:56
 * <p>
 * Description: 帧动画自定义View
 */
public class FrameAnimationView extends GLSurfaceView {

    private SceneRenderer mRenderer;//场景渲染器

    private FrameForDraw mDraw;

    private static final ScaleType[] sScaleTypeArray = {
            ScaleType.MATRIX,
            ScaleType.FIT_XY,
            ScaleType.FIT_START,
            ScaleType.FIT_CENTER,
            ScaleType.FIT_END,
            ScaleType.CENTER,
            ScaleType.CENTER_CROP,
            ScaleType.CENTER_INSIDE
    };


    private boolean mOneShot = false;

    private int stepDuration;

    private String path;

    private ScaleType mScaleType;

    public FrameAnimationView(Context context) {
        this(context, null);
    }

    public FrameAnimationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        //初始化View
        init();

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FrameAnimationView);

        mOneShot = a.getBoolean(R.styleable.FrameAnimationView_oneshot, false);

        if (a.hasValue(R.styleable.FrameAnimationView_src)) {
            final String path = a.getString(R.styleable.FrameAnimationView_src);
            setImageSource(path);
        }

        if (a.hasValue(R.styleable.FrameAnimationView_stepDuration)) {
            final int stepDuration = a.getInt(R.styleable.FrameAnimationView_stepDuration, 50);
            setStepDuration(stepDuration);
        }

        final int index = a.getInt(R.styleable.FrameAnimationView_scaleType, -1);
        if (index >= 0) {
            setScaleType(sScaleTypeArray[index]);
        }

        a.recycle();

    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mDraw.destroy();
    }

    public void setStepDuration(int stepDuration) {
        mDraw.setTimeStep(stepDuration);
    }

    public void setImageSource(String path) {
        if (!path.equals(this.path)) {
            this.path = path;
            mDraw.setZipPath(path);
        }
    }

    /**
     * <pre>
     *  设置透明背景的方法，根据实际情况，可能setEGLConfigChooser中的alpha可能要设置成0
     *  再者就是这个方法需要在setRenderer之前调用才有效
     * </pre>
     */
    public void setTranslucent() {
        // 设置背景透明，否则一般加载时间长的话会先黑一下，但是也有问题，就是在它之上无法再有View了，因为它是top的，用的时候需要注意，必要的时候将其设置为false
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        setZOrderOnTop(true);
    }

    private void init() {
        this.setEGLContextClientVersion(3); //设置使用OPENGL ES3.0
        setTranslucent();                   //设置背景透明
        mRenderer = new SceneRenderer();    //创建场景渲染器
        setRenderer(mRenderer);             //设置渲染器
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);//设置渲染模式
        this.setKeepScreenOn(true);
        mScaleType = ScaleType.CENTER_CROP;
        //创建帧动画绘制类
        mDraw = new FrameForDraw(FrameAnimationView.this);
    }

    public void setAnimation(String path,int timeStep){
        mDraw.setAnimation(this, path, timeStep);
    }

    public void start(){
        mDraw.start();
    }

    public void stop(){
        mDraw.stop();
    }

    public boolean isPlay(){
        return mDraw.isPlay();
    }

    public void setAnimationStateChangedListener(AnimationStateChangedListener listener) {
        mDraw.setAnimationStateChangedListener(listener);
    }

    public ScaleType getScaleType() {
        return mScaleType;
    }

    public boolean isOneShot() {
        return mOneShot;
    }

    class SceneRenderer implements Renderer {

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            //设置屏幕背景色RGBA
            GLES30.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            //创建帧动画绘制类
//            mDraw = new FrameForDraw(FrameAnimationView.this);
            //GLSurfaceView创建完毕, 初始化绘制类
            mDraw.onCreated();
            //初始化变换矩阵
            MatrixState.setInitStack();
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            //设置视窗大小及位置
            GLES30.glViewport(0, 0, width, height);
            //计算GLSurfaceView的宽高比
            float ratio = (float) width / height;
            //调用此方法计算产生透视投影矩阵
//            MatrixState.setProjectOrtho(-ratio, ratio, -1, 1, 1, 10);
            MatrixState.setProjectOrtho(-1, 1, -1, 1, 1, 10);
            //调用此方法产生摄像机9参数位置矩阵
            MatrixState.setCamera(0,0,1,0f,0f,0f,0f,1.0f,0.0f);
            mDraw.onSizeChanged(width, height);
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            //清除深度缓冲与颜色缓冲
            GLES30.glClear( GLES30.GL_DEPTH_BUFFER_BIT | GLES30.GL_COLOR_BUFFER_BIT);

            //保护现场
            MatrixState.pushMatrix();
            //开启混合
            GLES30.glEnable(GLES30.GL_BLEND);
            //设置混合因子
            GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA,GLES30.GL_ONE_MINUS_SRC_ALPHA);
//            GLES30.glBlendFunc(GLES30.GL_SRC_COLOR,GLES30.GL_ONE_MINUS_SRC_COLOR);
            //绘制纹理
            mDraw.draw();
            //关闭混合
            GLES30.glDisable(GLES30.GL_BLEND);
            //恢复现场
            MatrixState.popMatrix();
        }
    }

    /**
     * Options for scaling the bounds of an image to the bounds of this view.
     */
    public enum ScaleType {

        MATRIX      (0),

        FIT_XY      (1),

        FIT_START   (2),

        FIT_CENTER  (3),

        FIT_END     (4),

        CENTER      (5),

        CENTER_CROP (6),

        CENTER_INSIDE (7);

        ScaleType(int ni) {
            nativeInt = ni;
        }
        final int nativeInt;
    }

    public void setScaleType(ScaleType scaleType) {
        if (scaleType == null) {
            throw new NullPointerException();
        }

        if (mScaleType != scaleType) {
            mScaleType = scaleType;

//            requestRender();
        }
    }
}
