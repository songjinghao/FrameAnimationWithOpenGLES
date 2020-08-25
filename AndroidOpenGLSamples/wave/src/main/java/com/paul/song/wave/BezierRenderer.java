package com.paul.song.wave;

import android.content.res.Resources;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import androidx.annotation.ColorInt;

import com.paul.song.common.gl.MatrixState;
import com.paul.song.common.gl.ShaderUtil;
import com.paul.song.common.util.ColorUtil;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Create by Paul Song at 2020/8/16
 */
public class BezierRenderer implements GLSurfaceView.Renderer {

    public final GLSurfaceView mGlSurfaceView;
    public CubicBezier[] mBezierCurves;
    public int numberOfPoints = 256;

    private int mProgram; //自定义渲染管线着色器程序id
    public int muMVPMatrixHandle; //总变换矩阵引用id
    public int muColorHandle; // 曲线颜色引用id
    public int muAmpHandle; // 曲线振幅引用id
    public int muBzDataHandle; // 曲线起始点结束点位置引用id
    public int muBzDataCtrlHandle; // 曲线控制点位置引用id
    public int muBgColorHandle; // 视图窗口背景颜色引用id
    public int maTDataHandle; // 曲线时间参数t属性引用id

    private String mVertexShader; //顶点着色器代码脚本
    private String mFragmentShader; //片元着色器代码脚本


    private float[] mBgColor;


    public BezierRenderer(GLSurfaceView glSurfaceView, @ColorInt int backgroundColor) {
        mGlSurfaceView = glSurfaceView;
        mBgColor = ColorUtil.toOpenGlColor(backgroundColor);
    }

    private void initShader(Resources resources) {
        //加载顶点着色器的脚本内容
        mVertexShader = ShaderUtil.loadFromAssetsFile("pkm_mul.vert", /*view.getResources()*/resources);
        //加载片元着色器的脚本内容
        mFragmentShader = ShaderUtil.loadFromAssetsFile("pkm_mul.frag", /*view.getResources()*/resources);
        //基于顶点着色器与片元着色器创建程序
        mProgram = ShaderUtil.createProgram(mVertexShader, mFragmentShader);
        //获取程序中总变换矩阵引用id
        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        //获取程序中曲线颜色引用id
        muColorHandle = GLES20.glGetUniformLocation(mProgram, "u_Color");
        //获取程序中曲线振幅引用id
        muAmpHandle = GLES20.glGetUniformLocation(mProgram, "u_Amp");
        //获取程序中曲线起始点结束点位置引用id
        muBzDataHandle = GLES20.glGetUniformLocation(mProgram, "u_BzData");
        //获取程序中曲线控制点位置引用id
        muBzDataCtrlHandle = GLES20.glGetUniformLocation(mProgram, "u_BzDataCtrl");
        //获取程序中视图窗口背景颜色引用id
        muBgColorHandle = GLES20.glGetUniformLocation(mProgram, "u_BgColor");
        //获取程序中曲线时间参数t属性引用id
        maTDataHandle = GLES20.glGetAttribLocation(mProgram, "a_TData");
    }

    private void initVertexData() {

    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glDisable(GLES20.GL_CULL_FACE);

        //初始化顶点坐标与着色数据
        initVertexData();
        //初始化shader
        initShader(mGlSurfaceView.getResources());

        //初始化变换矩阵
        MatrixState.setInitStack();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        //设置视窗大小及位置
        GLES20.glViewport(0, 0, width, height);
        //计算GLSurfaceView的宽高比
        float ratio = (float) width / height;
        //调用此方法计算产生透视投影矩阵
        MatrixState.setProjectOrtho(-ratio, ratio, -1, 1, 1, 10);
        //调用此方法产生摄像机9参数位置矩阵
        MatrixState.setCamera(0,0,1,0f,0f,0f,0f,1.0f,0.0f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {

    }
}
