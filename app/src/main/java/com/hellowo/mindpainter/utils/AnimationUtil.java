package com.hellowo.mindpainter.utils;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.view.View;

/**
 * Created by Day2Life Android Dev on 2016-09-05.
 */
public class AnimationUtil {

    private static final long ANIMATION_DURATION = 250;

    /**
     * 하이라이트 애니메이션 재생
     * @param view 타겟뷰
     */
    public static void startScaleShowAnimation(View view){
        final AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(
                ObjectAnimator.ofFloat(view, "scaleX",
                        0f, 1f).setDuration(ANIMATION_DURATION),
                ObjectAnimator.ofFloat(view, "scaleY",
                        0f, 1f).setDuration(ANIMATION_DURATION)
        );
        animSet.setInterpolator(new FastOutSlowInInterpolator());
        animSet.start();
    }

    /**
     * 하이라이트 애니메이션 재생
     * @param view 타겟뷰
     */
    public static void startScaleHideAnimation(View view){
        final AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(
                ObjectAnimator.ofFloat(view, "scaleX",
                        1f, 0f).setDuration(ANIMATION_DURATION),
                ObjectAnimator.ofFloat(view, "scaleY",
                        1f, 0f).setDuration(ANIMATION_DURATION)
        );
        animSet.setInterpolator(new FastOutSlowInInterpolator());
        animSet.start();
    }

    /**
     * 아래에서 등장하는 애니메이션
     * @param view 애니메이션 뷰
     */
    public static void startFromBottomSlideAppearAnimation(View view, float offset){
        final AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(
                ObjectAnimator.ofFloat(view, "translationY",
                        offset, 0).setDuration(ANIMATION_DURATION),
                ObjectAnimator.ofFloat(view, "alpha",
                        0f, 1f).setDuration(ANIMATION_DURATION)
        );
        animSet.setInterpolator(new FastOutSlowInInterpolator());
        animSet.start();
    }

    /**
     * 왼쪽에서 등장하는 애니메이션
     * @param view 애니메이션 뷰
     */
    public static void startFromLeftSlideAppearAnimation(View view, float offset){
        final AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(
                ObjectAnimator.ofFloat(view, "translationX",
                        -offset, 0).setDuration(ANIMATION_DURATION),
                ObjectAnimator.ofFloat(view, "alpha",
                        0f, 1f).setDuration(ANIMATION_DURATION)
        );
        animSet.setInterpolator(new FastOutSlowInInterpolator());
        animSet.start();
    }

    /**
     * 위로 사라지는 애니메이션
     * @param view 애니메이션 뷰
     */
    public static void startToTopDisappearAnimation(View view, float offset){
        final AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(
                ObjectAnimator.ofFloat(view, "translationY",
                        0, -offset).setDuration(ANIMATION_DURATION),
                ObjectAnimator.ofFloat(view, "alpha",
                        1f, 0f).setDuration(ANIMATION_DURATION)
        );
        animSet.setInterpolator(new FastOutSlowInInterpolator());
        animSet.start();
    }

}
