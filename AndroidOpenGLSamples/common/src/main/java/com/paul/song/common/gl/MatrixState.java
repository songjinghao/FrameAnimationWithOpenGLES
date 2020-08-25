package com.paul.song.common.gl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.opengl.Matrix;

/**
 * Copyright (C)
 * <p>
 * FileName: MatrixState
 * <p>
 * Author: Paul Song
 * <p>
 * Date: 2020/5/5 21:41
 * <p>
 * Description: 存储系统矩阵状态的类
 */
public class MatrixState {
    private static float[] mProjMatrix = new float[16];//4x4矩阵 投影用
    private static float[] mVMatrix = new float[16];//摄像机位置朝向9参数矩阵
    private static float[] mMVPMatrix = new float[16];//最后起作用的总变换矩阵
    private static float[] currMatrix;//当前变换矩阵
    public static float[] lightLocation = new float[]{0, 0, 0};//定位光光源位置
    public static FloatBuffer cameraFB;
    public static FloatBuffer lightPositionFB;

    //保护变换矩阵的栈
    static float[][] mStack = new float[10][16];
    static int stackTop = -1;

    //获取不变换初始矩阵
    public static void setInitStack() {
        currMatrix = new float[16];
        Matrix.setRotateM(currMatrix, 0, 0, 1, 0, 0);
    }

    //保护变换矩阵
    public static void pushMatrix() {
        stackTop++;
        for (int i = 0; i < 16; i++) {
            mStack[stackTop][i] = currMatrix[i];
        }
    }

    //恢复变换矩阵
    public static void popMatrix() {
        for (int i = 0; i < 16; i++) {
            currMatrix[i] = mStack[stackTop][i];
        }
        stackTop--;
    }

    //设置沿xyz轴移动
    public static void translate(float x, float y, float z) {
        Matrix.translateM(currMatrix, 0, x, y, z);
    }

    //设置绕xyz轴移动
    public static void rotate(float angle, float x, float y, float z) {
        Matrix.rotateM(currMatrix, 0, angle, x, y, z);
    }

    public static void scale(float x, float y, float z) {
        Matrix.scaleM(currMatrix, 0, x, y, z);
    }

    //插入自带矩阵
    public static void matrix(float[] self) {
        float[] result = new float[16];
        Matrix.multiplyMM(result, 0, currMatrix, 0, self, 0);
        currMatrix = result;
    }


    static ByteBuffer llbb = ByteBuffer.allocateDirect(3 * 4);
    static float[] cameraLocation = new float[3];//摄像机位置

    /**
     * 设置摄像机
     * @param cx  摄像机位置x
     * @param cy  摄像机位置y
     * @param cz  摄像机位置z
     * @param tx  摄像机目标点x
     * @param ty  摄像机目标点y
     * @param tz  摄像机目标点z
     * @param upx 摄像机UP向量X分量
     * @param upy 摄像机UP向量Y分量
     * @param upz 摄像机UP向量Z分量
     */
    public static void setCamera(float cx, float cy, float cz, float tx, float ty, float tz, float upx, float upy, float upz) {
        Matrix.setLookAtM(mVMatrix, 0, cx, cy, cz, tx, ty, tz, upx, upy, upz);

        cameraLocation[0] = cx;
        cameraLocation[1] = cy;
        cameraLocation[2] = cz;

        llbb.clear();
        llbb.order(ByteOrder.nativeOrder());//设置字节顺序
        cameraFB = llbb.asFloatBuffer();
        cameraFB.put(cameraLocation);
        cameraFB.position(0);
    }

    /**
     * 设置透视投影参数
     * @param left near面的left
     * @param right near面的right
     * @param bottom near面的bottom
     * @param top near面的top
     * @param near near面距离
     * @param far far面距离
     */
    public static void setProjectFrustum(float left, float right, float bottom, float top, float near, float far) {
        Matrix.frustumM(mProjMatrix, 0, left, right, bottom, top, near, far);
    }

    /**
     * 设置正交投影参数
     * @param left near面的left
     * @param right near面的right
     * @param bottom near面的bottom
     * @param top near面的top
     * @param near near面距离
     * @param far far面距离
     */
    public static void setProjectOrtho(float left, float right, float bottom, float top, float near, float far) {
        Matrix.orthoM(mProjMatrix, 0, left, right, bottom, top, near, far);
    }

    //获取具体物体的总变换矩阵
    public static float[] getFinalMatrix() {
        Matrix.multiplyMM(mMVPMatrix, 0, mVMatrix, 0, currMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVPMatrix, 0);
        return mMVPMatrix;
    }

    //获取具体物体的变换矩阵
    public static float[] getMMatrix() {
        return currMatrix;
    }

    //获取投影矩阵
    public static float[] getProjMatrix() {
        return mProjMatrix;
    }

    //获取摄像机朝向的矩阵
    public static float[] getCaMatrix() {
        return mVMatrix;
    }


    //设置灯光位置的方法
    static ByteBuffer llbbL = ByteBuffer.allocateDirect(3 * 4);

    public static void setLightLocation(float x, float y, float z) {
        llbbL.clear();

        lightLocation[0] = x;
        lightLocation[1] = y;
        lightLocation[2] = z;

        llbbL.order(ByteOrder.nativeOrder());//设置字节顺序
        lightPositionFB = llbbL.asFloatBuffer();
        lightPositionFB.put(lightLocation);
        lightPositionFB.position(0);
    }
}

