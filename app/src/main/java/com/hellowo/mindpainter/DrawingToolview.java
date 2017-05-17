package com.hellowo.mindpainter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.hellowo.mindpainter.utils.ViewUtil;

public class DrawingToolView extends FrameLayout {
    public DrawingToolView(Context context) {
        super(context);
        setLayout();
    }

    public DrawingToolView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayout();
    }

    public DrawingToolView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayout();
    }

    String[] colors = {
            "#000000", "#607D8B", "#9E9E9E", "#795548", "#FF5722",
            "#FF9800", "#FFC107", "#FFEB3B", "#CDDC39", "#8BC34A",
            "#4CAF50", "#009688", "#00BCD4", "#03A9F4", "#2196F3",
            "#3F51B5", "#673AB7", "#9C27B0", "#E91E63", "#F44336"
    };

    ImageButton toolBtn;
    ImageButton colorBtn;
    ImageButton lineBtn;
    ImageButton eraserBtn;

    HorizontalScrollView colorLy;
    View openedView;

    private void setLayout() {
        String infService = Context.LAYOUT_INFLATER_SERVICE;
        LayoutInflater li = (LayoutInflater) getContext().getSystemService(infService);
        View v = li.inflate(R.layout.drawing_tool_view, this, false);
        addView(v);

        colorBtn = (ImageButton)v.findViewById(R.id.colorBtn);
        colorLy = (HorizontalScrollView)v.findViewById(R.id.colorLy);

        LinearLayout colotRootView = (LinearLayout) colorLy.getChildAt(0);

        for (int i = 0; i < colors.length; i++) {
            final int color = Color.parseColor(colors[i]);
            ((ImageView)colotRootView.getChildAt(i)).setColorFilter(color);
            ((ImageView)colotRootView.getChildAt(i)).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    inkView.setColorWithListener(color);
                    close();
                }
            });
        }

        colorBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(openedView == colorLy) {
                    close();
                }else{
                    open(colorLy);
                }
            }
        });
    }

    private void open(View view) {
        openedView = view;
        final AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(
                ObjectAnimator.ofFloat(this, "translationY",
                        getTranslationY(), getTranslationY() - colorLy.getHeight())
                        .setDuration(250)
        );
        animSet.setInterpolator(new FastOutSlowInInterpolator());
        animSet.start();
    }

    private void close() {
        final AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(
                ObjectAnimator.ofFloat(this, "translationY",
                        getTranslationY(), getTranslationY() + openedView.getHeight())
                        .setDuration(250)
        );
        animSet.setInterpolator(new FastOutSlowInInterpolator());
        animSet.start();
        openedView = null;
    }

    Activity activity;
    InkView inkView;

    public void init(Activity activity, InkView inkView) {
        this.activity = activity;
        this.inkView = inkView;
    }
}
