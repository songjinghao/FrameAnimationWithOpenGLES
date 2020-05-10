/*
 *
 * ZipPkmReader.java
 * 
 * Created by Wuwang on 2016/12/8
 * Copyright © 2016年 深圳哎吖科技. All rights reserved.
 */
package com.paul.song.frameanimation.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.opengl.ETC1;
import android.opengl.ETC1Util;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Description: 压缩纹理读取类
 */
public class ZipPkmReader {
    private static final String TAG = ZipPkmReader.class.getSimpleName();
    private String path;
    private ZipInputStream mZipStream;
    private AssetManager mManager;
    private ZipEntry mZipEntry;
    private ByteBuffer headerBuffer;

    public ZipPkmReader(Context context){
        this(context.getAssets());
    }

    public ZipPkmReader(AssetManager manager){
        this.mManager=manager;
    }

    public void setZipPath(String path){
        Log.e(TAG,path+" set");
        this.path=path;
    }

    public boolean open(){
        Log.e(TAG,path+" open");
        if(path==null)return false;
        try {
            if(path.startsWith("assets/")){
                InputStream s=mManager.open(path.substring(7));
                mZipStream=new ZipInputStream(s);
            }else{
                File f=new File(path);
                Log.e(TAG,path+" is File exists->"+f.exists());
                mZipStream=new ZipInputStream(new FileInputStream(path));
            }
            return true;
        } catch (IOException e) {
            Log.e(TAG,"eee-->"+e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void close(){
        if(mZipStream!=null){
            try {
                mZipStream.closeEntry();
                mZipStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if(headerBuffer!=null){
                headerBuffer.clear();
                headerBuffer=null;
            }
        }
    }

    private boolean hasElements(){
        try {
            if(mZipStream!=null){
                mZipEntry=mZipStream.getNextEntry();
                if(mZipEntry!=null){
                    return true;
                }
                Log.e(TAG,"mZip entry null");
            }
        } catch (IOException e) {
            Log.e(TAG,"err  dd->"+e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public InputStream getNextStream(){
        if(hasElements()){
            return mZipStream;
        }
        return null;
    }

    public ETC1Util.ETC1Texture getNextTexture(){
        if(hasElements()){
            try {
                ETC1Util.ETC1Texture e= createTexture(mZipStream);
                return e;
            } catch (IOException e1) {
                Log.e(TAG,"err->"+e1.getMessage());
                e1.printStackTrace();
            }
        }
        return null;
    }

    public ETC2Texture getNextETC2Texture(){
        if(hasElements()){
            try {
                ETC2Texture e= createETC2Texture(mZipStream);
                return e;
            } catch (IOException e1) {
                Log.e(TAG,"err->"+e1.getMessage());
                e1.printStackTrace();
            }
        }
        return null;
    }

    private ETC1Util.ETC1Texture createTexture(InputStream input) throws IOException {
        int width = 0;
        int height = 0;
        byte[] ioBuffer = new byte[4096];
        {
            if (input.read(ioBuffer, 0, ETC1.ETC_PKM_HEADER_SIZE) != ETC1.ETC_PKM_HEADER_SIZE) {
                throw new IOException("Unable to read PKM file header.");
            }
            if(headerBuffer==null){
                headerBuffer = ByteBuffer.allocateDirect(ETC1.ETC_PKM_HEADER_SIZE)
                        .order(ByteOrder.BIG_ENDIAN);
            }
            headerBuffer.put(ioBuffer, 0, ETC1.ETC_PKM_HEADER_SIZE).position(0);
            if (!ETC1.isValid(headerBuffer)) {
                throw new IOException("Not a PKM file.");
            }
            width = ETC1.getWidth(headerBuffer);
            height = ETC1.getHeight(headerBuffer);
        }
        int encodedSize = ETC1.getEncodedDataSize(width, height);
        ByteBuffer dataBuffer = ByteBuffer.allocateDirect(encodedSize).order(ByteOrder.nativeOrder());
        int len;
        while ((len =input.read(ioBuffer))!=-1){
            dataBuffer.put(ioBuffer,0,len);
        }
        dataBuffer.position(0);
        return new ETC1Util.ETC1Texture(width, height, dataBuffer);
    }

    /**
     * A utility class encapsulating a compressed ETC2 texture.
     */
    public static class ETC2Texture {
        public ETC2Texture(int width, int height, ByteBuffer data) {
            mWidth = width;
            mHeight = height;
            mData = data;
        }

        /**
         * Get the width of the texture in pixels.
         * @return the width of the texture in pixels.
         */
        public int getWidth() { return mWidth; }

        /**
         * Get the height of the texture in pixels.
         * @return the width of the texture in pixels.
         */
        public int getHeight() { return mHeight; }

        /**
         * Get the compressed data of the texture.
         * @return the texture data.
         */
        public ByteBuffer getData() { return mData; }

        private int mWidth;
        private int mHeight;
        private ByteBuffer mData;
    }

    private ETC2Texture createETC2Texture(InputStream input) throws IOException {
        int width = 0;
        int height = 0;
        byte[] ioBuffer = new byte[4096];
        {
            if (input.read(ioBuffer, 0, ETC1.ETC_PKM_HEADER_SIZE) != ETC1.ETC_PKM_HEADER_SIZE) {
                throw new IOException("Unable to read PKM file header.");
            }
            if(headerBuffer==null){
                headerBuffer = ByteBuffer.allocateDirect(ETC1.ETC_PKM_HEADER_SIZE)
                        .order(ByteOrder.BIG_ENDIAN);
            }
            headerBuffer.put(ioBuffer, 0, ETC1.ETC_PKM_HEADER_SIZE).position(0);

            /*byte[] data=loadDataFromAssets(input);
            ByteBuffer buffer = ByteBuffer.allocateDirect(data.length).order(ByteOrder.LITTLE_ENDIAN);
            buffer.put(data).position(16);

            ByteBuffer header = ByteBuffer.allocateDirect(ETC1.ETC_PKM_HEADER_SIZE).order(ByteOrder.BIG_ENDIAN);
            header.put(data, 0, 16).position(0);*/

            /*if (!ETC1.isValid(headerBuffer)) {
                throw new IOException("Not a PKM file.");
            }
            width = ETC1.getWidth(headerBuffer);
            height = ETC1.getHeight(headerBuffer);*/
            width = headerBuffer.getShort(12);
            height = headerBuffer.getShort(14);
        }

        int len, encodedSize = 0;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while ((len =input.read(ioBuffer))!=-1){
//            dataBuffer.put(ioBuffer,0,len);
            baos.write(ioBuffer, 0, len);
            encodedSize += len;
        }
//        int encodedSize = baos.size();
        Log.d(TAG, "size:" + baos.size() + ", byte size:" + baos.toByteArray().length);
        ByteBuffer dataBuffer = ByteBuffer.allocateDirect(encodedSize).order(ByteOrder.nativeOrder());
        dataBuffer.put(baos.toByteArray(), 0, encodedSize);
        dataBuffer.position(0);
        return new ETC2Texture(width, height, dataBuffer);
    }

    public static byte[] loadDataFromAssets(InputStream in){
        byte[] data=null;
//        InputStream in=null;
        try
        {
//            in = r.getAssets().open(fname);
            int ch=0;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while((ch=in.read())!=-1)
            {
                baos.write(ch);
            }
            data=baos.toByteArray();
            baos.close();
            in.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return data;
    }

}
