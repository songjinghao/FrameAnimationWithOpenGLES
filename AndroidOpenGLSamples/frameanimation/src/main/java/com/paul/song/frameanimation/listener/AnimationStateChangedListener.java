package com.paul.song.frameanimation.listener;

/**
 * Copyright (C)
 * <p>
 * FileName: FrameAnimationStateChangedListener
 * <p>
 * Author: jhsong4
 * <p>
 * Date: 2020/5/7 16:56
 * <p>
 * Description:
 */
public interface AnimationStateChangedListener {

    int START=1;
    int STOP=2;
    int PLAYING=3;
    int INIT=4;
    int PAUSE=5;
    int RESUME=6;

    void onStateChanged(int lastState, int nowState);
}
