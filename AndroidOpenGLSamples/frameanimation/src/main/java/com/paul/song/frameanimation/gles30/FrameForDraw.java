package com.paul.song.frameanimation.gles30;

import android.content.res.Resources;
import android.opengl.GLES30;
import android.util.Log;
import android.util.SparseArray;

import com.paul.song.common.gl.MatrixState;
import com.paul.song.common.gl.ShaderUtil;
import com.paul.song.frameanimation.listener.AnimationStateChangedListener;
import com.paul.song.frameanimation.utils.ZipPkmReader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Copyright (C)
 * <p>
 * FileName: FrameForDraw  Opengl es 3.0 版本
 * <p>
 * Author: Paul Song
 * <p>
 * Date: 2020/5/5 23:46
 * <p>
 * Description:
 */
public class FrameForDraw {
    private static final String TAG = FrameForDraw.class.getSimpleName();

    private int mProgram; //自定义渲染管线着色器程序id
    private int muMVPMatrixHandle; //总变换矩阵引用id
    private int maPositionHandle; //顶点位置属性引用id
    private int maTexCoorHandle; //顶点纹理坐标属性引用id
    private int msTextureHandle; //纹理属性引用id
    private int msTextureAlphaHandle; //Alpha纹理属性引用id

    private String mVertexShader; //顶点着色器代码脚本
    private String mFragmentShader; //片元着色器代码脚本

    private FloatBuffer mVertexBuffer; //顶点坐标数据缓冲
    private FloatBuffer mTexCoorBuffer; //顶点纹理坐标数据缓冲

    private int texture; //纹理id
    private ZipPkmReader mPkmReader;
    private String lastPath;
    private boolean animChanged = false;
    private FrameAnimationView mView;
    private final int START = 0;
    private final int STOP = 1;
    private int state = STOP; //是否开启动画播放
    private Semaphore semaphore = new Semaphore(0);
    private boolean isPlay = false; //动画是否正在播放
    private long time = 0;
    private int timeStep = 50;
//    private ByteBuffer emptyBuffer;
    private int viewWidth, viewHeight;
    private int texWidth, texHeight;

    private SparseArray<ZipPkmReader.ETC2Texture> mTextureCache = new SparseArray<>(); // TODO: 替换成线程安全map
    private int pushIndex = 0;
    private int pullIndex = 0;
    private int texCacheLength = Integer.MAX_VALUE;

    //顶点坐标
    private float[] vertices = {
            -1.0f,  1.0f,
            -1.0f, -1.0f,
             1.0f,  1.0f,
             1.0f, -1.0f,
    };

    //纹理坐标
    private float[] texCoor = {
            0.0f,  0.0f,
            0.0f,  1.0f,
            1.0f,  0.0f,
            1.0f,  1.0f,
    };
    private AnimationStateChangedListener mStateChangeListener;

    public FrameForDraw(FrameAnimationView view) {
        this.mView = view;
        mPkmReader = new ZipPkmReader(view.getResources().getAssets());
    }

    public void onCreated() {
        //初始化顶点坐标与着色数据
        initVertexData();
        //初始化shader
        initShader(mView.getResources());
        //初始化纹理
        initTexture();
    }

    private void initVertexData() {
        // 创建顶点坐标数据缓冲
        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder()); //设置字节顺序
        mVertexBuffer = vbb.asFloatBuffer(); //转换为Float型缓冲
        mVertexBuffer.put(vertices); //向缓冲区中放入顶点坐标数据
        mVertexBuffer.position(0); // 设置缓冲区起始位置

        //创建顶点纹理坐标数据缓冲
        ByteBuffer cbb = ByteBuffer.allocateDirect(texCoor.length * 4);
        cbb.order(ByteOrder.nativeOrder()); //设置字节顺序
        mTexCoorBuffer = cbb.asFloatBuffer(); //转换为Float型缓冲
        mTexCoorBuffer.put(texCoor); //向缓冲区中放入顶点着色数据
        mTexCoorBuffer.position(0); // 设置缓冲区起始位置

        //特别提示：由于不同平台字节顺序不同数据单元不是字节的一定要经过ByteBuffer
        //转换，关键是要通过ByteOrder设置nativeOrder()，否则有可能会出问题
    }

    private void initShader(Resources resources) {
        //加载顶点着色器的脚本内容
        mVertexShader = ShaderUtil.loadFromAssetsFile("pkm_mul.vert", /*view.getResources()*/resources);
        //加载片元着色器的脚本内容
        mFragmentShader = ShaderUtil.loadFromAssetsFile("pkm.frag", /*view.getResources()*/resources);
        //基于顶点着色器与片元着色器创建程序
        mProgram = ShaderUtil.createProgram(mVertexShader, mFragmentShader);
        //获取程序中顶点位置属性引用id
        maPositionHandle = GLES30.glGetAttribLocation(mProgram, "aPosition");
        //获取程序中顶点纹理属性引用id
        maTexCoorHandle = GLES30.glGetAttribLocation(mProgram, "aTexCoord");
        //获取程序中总变换矩阵引用id
        muMVPMatrixHandle = GLES30.glGetUniformLocation(mProgram, "uMVPMatrix");
        //获取两个纹理引用
        msTextureHandle = GLES30.glGetUniformLocation(mProgram, "sTexture");
        msTextureAlphaHandle = GLES30.glGetUniformLocation(mProgram, "sTextureAlpha");
    }

    private void initTexture() {
        // 生成纹理ID
        int[] textures = new int[1];
        GLES30.glGenTextures(1, textures, 0);
        //生成纹理
        texture = textures[0];
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[0]);
        //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        //根据以上指定的参数，生成一个2D纹理
    }

    public void onSizeChanged(int width, int height) {
//        emptyBuffer = ByteBuffer.allocateDirect(ETC1.getEncodedDataSize(width, height));
        this.viewWidth = width;
        this.viewHeight = height;
    }

    public void draw() {
        if (state == STOP && !semaphore.tryAcquire()) {
//            Log.d(TAG, "draw return");
            return;
        }
//        Log.d(TAG, "semaphore.tryAcquire() | " + semaphore.availablePermits());
        /*if (time != 0) {
            Log.e(TAG, "time-->" + (System.currentTimeMillis() - time));
        }*/
        time = System.currentTimeMillis();
        long startTime = System.currentTimeMillis();
//        configProjectOrtho();
        drawSelf();
        long s = System.currentTimeMillis() - startTime;
        if (isPlay) {
            if (s < timeStep) {
                try {
                    TimeUnit.MILLISECONDS.sleep(timeStep - s);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            mView.requestRender();
        } else {
            //TODO
        }
    }

    private void configProjectOrtho() {
        if (texWidth > 0 && texHeight > 0 && viewWidth > 0 && viewHeight > 0) {
            if (FrameAnimationView.ScaleType.FIT_XY == mView.getScaleType()) {
                MatrixState.setProjectOrtho(-1, 1, -1, 1, 1, 10);
            } else {
                final int dwidth = this.texWidth;
                final int dheight = this.texHeight;

                final int vwidth = this.viewWidth;
                final int vheight = this.viewHeight;

                float scaleView = (float) vwidth / vheight;
                float scaleTex = (float) dwidth / dheight;
                if (scaleTex > scaleView) {
                    switch (mView.getScaleType()) {
                        case CENTER_CROP:
                            //控件中心和原始图片中心重叠
                            //等比例缩放
                            //原图比例和控件比例一致，则填满控件
                            //如果原图比例大于控件比例，则按照控件高/图片高进行等比例缩放，这样就能保证图片宽度在进行同等比例缩放的时候，图片宽度大于或等于控件的宽度
                            //如果原图比例小于控件比例，则按照控件宽/图片宽进行等比例缩放，这样就能保证图片高度在进行同等比例缩放的时候，图片高度大于或等于控件的高度
                            MatrixState.setProjectOrtho(-scaleView/scaleTex, scaleView/scaleTex, -1, 1, 1, 10);
                            break;
                        case CENTER_INSIDE:
                            //如果图片宽（或高）大于控件宽（或）则等比例缩小，显示效果和FitCenter一样。
                            //如果图片宽高都小于控件宽高则直接居中显示
                            MatrixState.setProjectOrtho(-1, 1, -scaleTex/scaleView, scaleTex/scaleView, 1, 10);
                            break;
                        case FIT_START:
                            //等比例缩放
                            //图片宽高比和控件宽高比一致，则填满控件显示
                            //图片宽高比和控件宽高比不一致，则等比缩放图片最长边，直到和控件宽或高任意一边重叠。这种情况会出现右边或者下边空白
                            //如果原图高大于宽，则左对齐显示
                            //如果原图宽大于高，则顶部对齐显示
                            MatrixState.setProjectOrtho(-1, 1, 1 - 2*scaleTex/scaleView, 1, 1, 10);
                            break;
                        case FIT_END:
                            //等比例缩放
                            //图片宽高比和控件宽高比一致，则填满控件显示
                            //图片宽高比和控件宽高比不一致，则等比缩放图片最长边，直到和控件宽或高任意一边重叠。这种情况会出现左边或者上边空白
                            //如果原图高大于宽，则右对齐显示
                            //如果原图宽大于高，则底部对齐显示
                            MatrixState.setProjectOrtho(-1, 1, -1, 2*scaleTex/scaleView - 1, 1, 10);
                            break;
                    }
                } else {
                    switch (mView.getScaleType()) {
                        case CENTER_CROP:
                            //控件中心和原始图片中心重叠
                            //等比例缩放
                            //原图比例和控件比例一致，则填满控件
                            //如果原图比例大于控件比例，则按照控件高/图片高进行等比例缩放，这样就能保证图片宽度在进行同等比例缩放的时候，图片宽度大于或等于控件的宽度
                            //如果原图比例小于控件比例，则按照控件宽/图片宽进行等比例缩放，这样就能保证图片高度在进行同等比例缩放的时候，图片高度大于或等于控件的高度
                            MatrixState.setProjectOrtho(-1, 1, -scaleTex/scaleView, scaleTex/scaleView, 1, 10);
                            break;
                        case CENTER_INSIDE:
                            //如果图片宽（或高）大于控件宽（或）则等比例缩小，显示效果和FitCenter一样。
                            //如果图片宽高都小于控件宽高则直接居中显示
                            MatrixState.setProjectOrtho(-scaleView/scaleTex, scaleView/scaleTex, -1, 1, 1, 10);
                            break;
                        case FIT_START:
                            //等比例缩放;
                            //图片宽高比和控件宽高比一致，则填满控件显示;
                            //图片宽高比和控件宽高比不一致，则等比缩放图片最长边，直到和控件宽或高任意一边重叠。这种情况会出现右边或者下边空白;
                            //如果原图高大于宽，则左对齐显示;
                            //如果原图宽大于高，则顶部对齐显示;
                            MatrixState.setProjectOrtho(-1, 2*scaleView/scaleTex - 1, -1,1, 1, 10);
                            break;
                        case FIT_END:
                            //等比例缩放;
                            //图片宽高比和控件宽高比一致，则填满控件显示;
                            //图片宽高比和控件宽高比不一致，则等比缩放图片最长边，直到和控件宽或高任意一边重叠。这种情况会出现左边或者上边空白;
                            //如果原图高大于宽，则右对齐显示;
                            //如果原图宽大于高，则底部对齐显示;
                            MatrixState.setProjectOrtho(1 - 2*scaleView/scaleTex, 1, -1, 1, 1, 10);
                            break;
                    }
                }
            }
        }
    }

    private void drawSelf() {
        //指定使用某套着色器程序
        GLES30.glUseProgram(mProgram);
        //将最终变换矩阵传入着色器程序
//        GLES30.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, MatrixState.getFinalMatrix(), 0);
        //启用顶点位置数据
        GLES30.glEnableVertexAttribArray(maPositionHandle);
        //将顶点位置数据传入渲染管线
        GLES30.glVertexAttribPointer(maPositionHandle, 2, GLES30.GL_FLOAT, false, 0, mVertexBuffer);
        //启用纹理坐标数据
        GLES30.glEnableVertexAttribArray(maTexCoorHandle);
        //将顶点纹理坐标数据传入渲染管线
        GLES30.glVertexAttribPointer(maTexCoorHandle, 2, GLES30.GL_FLOAT, false, 0, mTexCoorBuffer);

        ZipPkmReader.ETC2Texture t/*, tAlpha*/;
        if (!mView.isOneShot()) {// 循环播放 先从内存取纹理
            if (pullIndex >= texCacheLength) {
                pullIndex = 0;
            }
            t = mTextureCache.get(pullIndex++);
//            tAlpha = mTextureCache.get(pullIndex++);
            if (t == null/* && tAlpha == null*/) {
                t = mPkmReader.getNextETC2Texture();
//                tAlpha = mPkmReader.getNextTexture();
                Log.d("sjh1",
                        "t=" + (t != null ? t + "[" + t.getWidth() + " " + t.getHeight() + "], " : t + ", ") /*+
                        "tAlpha=" + (tAlpha != null ? tAlpha + "[" + tAlpha.getWidth() + " " + tAlpha.getHeight() + "]" : tAlpha)*/);

                if (t != null/* && tAlpha != null*/) {
                    /*if (t.getWidth() != tAlpha.getWidth()) {
                        t = tAlpha;
                        tAlpha = mPkmReader.getNextTexture();
                        Log.d("sjh1-fixed",
                                "t=" + (t != null ? t + "[" + t.getWidth() + " " + t.getHeight() + "], " : t + ", ") +
                                        "tAlpha=" + (tAlpha != null ? tAlpha + "[" + tAlpha.getWidth() + " " + tAlpha.getHeight() + "]" : tAlpha));
                    }*/

                    mTextureCache.append(pushIndex++, t);
//                    mTextureCache.append(pushIndex++, tAlpha);
                }
            }
        } else {// oneshot 直接IO读取
            t = mPkmReader.getNextETC2Texture();
//            tAlpha = mPkmReader.getNextTexture();
            Log.d("sjh2", t + ", "/* + tAlpha*/);
        }

        //绑定纹理
//        ETC1Util.ETC1Texture t = mPkmReader.getNextTexture();
//        ETC1Util.ETC1Texture tAlpha = mPkmReader.getNextTexture();
        if (t != null/* && tAlpha != null*/) {
//            Log.d(TAG, "drawSelf");
            this.texWidth = t.getWidth();
            this.texHeight = t.getHeight();
            configProjectOrtho();
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture);
//            ETC1Util.loadTexture(GLES30.GL_TEXTURE_2D, 0, 0, GLES30.GL_COMPRESSED_RGBA8_ETC2_EAC, GLES30.GL_UNSIGNED_SHORT_5_6_5, t);
            GLES30.glCompressedTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_COMPRESSED_RGBA8_ETC2_EAC, texWidth, texHeight, 0, t.getData().capacity(), t.getData());
            GLES30.glUniform1i(msTextureHandle, 0);

            /*GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[1]);
            ETC1Util.loadTexture(GLES30.GL_TEXTURE_2D, 0, 0, GLES30.GL_RGB, GLES30.GL_UNSIGNED_SHORT_5_6_5, tAlpha);
            GLES30.glUniform1i(msTextureAlphaHandle, 1);*/
        } else {
//            isPlay = false;
            texCacheLength = mTextureCache.size();
            if (!mView.isOneShot()) {//循环播放
                /*
                //绑定空纹理
                GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[0]);
                ETC1Util.loadTexture(GLES30.GL_TEXTURE_2D, 0, 0, GLES30.GL_RGB, GLES30.GL_UNSIGNED_SHORT_5_6_5, new ETC1Util.ETC1Texture(viewWidth, viewHeight, emptyBuffer));
                GLES30.glUniform1i(msTextureHandle, 0);

                GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[1]);
                ETC1Util.loadTexture(GLES30.GL_TEXTURE_2D, 0, 0, GLES30.GL_RGB, GLES30.GL_UNSIGNED_SHORT_5_6_5, new ETC1Util.ETC1Texture(viewWidth, viewHeight, emptyBuffer));
                GLES30.glUniform1i(msTextureAlphaHandle, 1);
                */

//                start();
//                mPkmReader.close(); // 关闭会导致IO读取出现异常
//                mPkmReader.open();
//                mView.requestRender();
            } else {
                stop();
                changeState(AnimationStateChangedListener.PLAYING, AnimationStateChangedListener.STOP);
            }
        }

        //将最终变换矩阵传入着色器程序
        GLES30.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, MatrixState.getFinalMatrix(), 0);
        //绘制三角形
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP,0,4);
        GLES30.glDisableVertexAttribArray(maPositionHandle);
        GLES30.glDisableVertexAttribArray(maTexCoorHandle);
    }

    public void setAnimation(FrameAnimationView view, String path, int timeStep) {
        this.mView=view;
        this.timeStep=timeStep;
        mPkmReader.setZipPath(path);
    }

    public void setZipPath(String path) {
        //        mPkmReader.close();
//        mPkmReader.setZipPath(path);
        if (!path.equals(this.lastPath)) {
            this.lastPath = path;
            this.animChanged = true;
            mPkmReader.setZipPath(path);
        }
        // 装载下一组动画, 清除缓存复位
        mTextureCache.clear();
        pushIndex = 0;
        pullIndex = 0;
        texCacheLength = Integer.MAX_VALUE;

        /*mView.postDelayed(new Runnable() {
            @Override
            public void run() {
                // 装载下一组动画, 清除缓存
                mTextureCache.clear();
                pushIndex = 0;
                pullIndex = 0;
                texCacheLength = Integer.MAX_VALUE;
            }
        }, timeStep);*/
    }

    public void setTimeStep(int stepDuration) {
        this.timeStep = stepDuration;
    }

    public void start() {
        Log.d(TAG, "start");
        if(!isPlay) {
//            stop();
            state = START;
            isPlay = true;
            changeState(AnimationStateChangedListener.STOP, AnimationStateChangedListener.START);
            if (animChanged && mPkmReader.open()) {
                animChanged = false;
            }
            mView.requestRender();
        } else if (!mView.isOneShot()) {
//            stop();
//            mPkmReader.close();
            if (animChanged && mPkmReader.open()) {
                animChanged = false;
            }
//            mPkmReader.open();
            mView.requestRender(); // TODO
        }
    }

    public void stop() {
        Log.d(TAG, "stop");
        if (isPlay) {
            state = STOP;
            isPlay=false;

            int availablePermits = semaphore.availablePermits();
            if (availablePermits > 1) {
                semaphore.tryAcquire(availablePermits - 1);
            } else if (availablePermits < 1) {
                semaphore.release(1 - availablePermits);
            }
            Log.d(TAG, "stop: semaphore.release() | " + semaphore.availablePermits());
        }
        /*try {
            TimeUnit.MILLISECONDS.sleep(timeStep);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
    }

    void destroy() {
        stop();
        if(mPkmReader!=null){
            mPkmReader.close();
        }
        mTextureCache.clear();
        mTextureCache = null;

        mVertexBuffer.clear();
        mVertexBuffer = null;
        mTexCoorBuffer.clear();
        mTexCoorBuffer = null;
    }

    public void setAnimationStateChangedListener(AnimationStateChangedListener listener) {
        this.mStateChangeListener = listener;
    }

    private void changeState(int lastState,int nowState) {
        if (this.mStateChangeListener != null) {
            this.mStateChangeListener.onStateChanged(lastState, nowState);
        }
    }

    public boolean isPlay(){
        return isPlay;
    }

}
