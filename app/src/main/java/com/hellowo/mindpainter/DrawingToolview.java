package com.hellowo.mindpainter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

    int[] toolId = {
            R.drawable.pencil,
            R.drawable.signpen,
            R.drawable.fountailpen,
            R.drawable.brush,
            R.drawable.highlight
    };

    float[] lineWidth = {1, 2, 3, 4, 5, 10, 15};

    int toolIndex = 0;
    int colorIndex = 0;
    int lineIndex = 1;

    ImageButton toolBtn;
    ImageButton colorBtn;
    ImageButton lineBtn;
    ImageButton eraserBtn;

    HorizontalScrollView colorLy;
    LinearLayout toolLy;
    LinearLayout lineLy;

    View openedView;

    private void setLayout() {
        String infService = Context.LAYOUT_INFLATER_SERVICE;
        LayoutInflater li = (LayoutInflater) getContext().getSystemService(infService);
        View v = li.inflate(R.layout.drawing_tool_view, this, false);
        addView(v);



        colorBtn = (ImageButton)v.findViewById(R.id.colorBtn);
        colorLy = (HorizontalScrollView)v.findViewById(R.id.colorLy);
        colorLy.setVisibility(GONE);

        final LinearLayout colotRootView = (LinearLayout) colorLy.getChildAt(0);
        colotRootView.getChildAt(colorIndex).setBackgroundResource(R.drawable.black_circle_stroke);
        colorBtn.setColorFilter(Color.parseColor(colors[colorIndex]));

        for (int i = 0; i < colors.length; i++) {
            final int finalI = i;
            final int color = Color.parseColor(colors[i]);

            ((ImageView)colotRootView.getChildAt(i)).setColorFilter(color);

            colotRootView.getChildAt(i).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    inkView.setColorWithListener(color);
                    colorBtn.setColorFilter(color);

                    colotRootView.getChildAt(colorIndex).setBackgroundResource(R.drawable.blank);
                    colotRootView.getChildAt(finalI).setBackgroundResource(R.drawable.black_circle_stroke);

                    colorIndex = finalI;
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



        toolBtn = (ImageButton)v.findViewById(R.id.toolBtn);
        toolLy = (LinearLayout)v.findViewById(R.id.toolLy);
        toolLy.setVisibility(GONE);

        ((FrameLayout)toolLy.getChildAt(toolIndex)).getChildAt(0)
                .setBackgroundResource(R.drawable.black_circle_stroke);
        toolBtn.setBackgroundResource(R.drawable.black_circle_stroke);

        for (int i = 0; i < toolId.length; i++) {
            final int finalI = i;
            final ImageView itemView = (ImageView) ((FrameLayout)toolLy.getChildAt(finalI)).getChildAt(0);

            itemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    inkView.setToolWithLister(finalI);
                    toolBtn.setImageResource(toolId[finalI]);

                    ((FrameLayout)toolLy.getChildAt(toolIndex)).getChildAt(0).setBackgroundResource(R.drawable.blank);
                    itemView.setBackgroundResource(R.drawable.black_circle_stroke);

                    toolIndex = finalI;
                }
            });
        }

        toolBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(openedView == toolLy) {
                    close();
                }else{
                    open(toolLy);
                }
            }
        });



        lineBtn = (ImageButton)v.findViewById(R.id.lineBtn);
        lineLy = (LinearLayout)v.findViewById(R.id.lineLy);
        lineLy.setVisibility(GONE);

        ((FrameLayout)lineLy.getChildAt(lineIndex)).getChildAt(0)
                .setBackgroundResource(R.drawable.black_circle_stroke);

        for (int i = 0; i < lineWidth.length; i++) {
            final int finalI = i;
            final ImageView itemView = (ImageView) ((FrameLayout)lineLy.getChildAt(finalI)).getChildAt(0);

            itemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    inkView.setStrokeWidthWithListener(lineWidth[finalI]);

                    float TopPadding = 20 - (lineWidth[finalI] / 2 + lineWidth[finalI] % 2);
                    float BottomPadding = 20 - lineWidth[finalI] / 2;

                    lineBtn.setPadding(0, ViewUtil.dpToPx(activity, TopPadding),
                            0, ViewUtil.dpToPx(activity, BottomPadding));

                    ((FrameLayout)lineLy.getChildAt(lineIndex)).getChildAt(0).setBackgroundResource(R.drawable.blank);
                    itemView.setBackgroundResource(R.drawable.black_circle_stroke);

                    lineIndex = finalI;
                }
            });
        }

        lineBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(openedView == lineLy) {
                    close();
                }else{
                    open(lineLy);
                }
            }
        });
    }

    private void open(View view) {
        view.setVisibility(VISIBLE);

        if(openedView == null) {

            openedView = view;
            final AnimatorSet animSet = new AnimatorSet();
            animSet.playTogether(
                    ObjectAnimator.ofFloat(this, "translationY",
                            getTranslationY(), getTranslationY() - ViewUtil.dpToPx(activity, 70))
                            .setDuration(250)
            );
            animSet.setInterpolator(new FastOutSlowInInterpolator());
            animSet.start();

        }else {

            openedView.setVisibility(GONE);
            openedView = view;

        }
    }

    private void close() {
        final AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(
                ObjectAnimator.ofFloat(this, "translationY",
                        getTranslationY(), getTranslationY() + ViewUtil.dpToPx(activity, 70))
                        .setDuration(250)
        );
        animSet.setInterpolator(new FastOutSlowInInterpolator());
        animSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                openedView.setVisibility(GONE);
                openedView = null;
            }
        });
        animSet.start();
    }

    Activity activity;
    InkView inkView;

    public void init(Activity activity, InkView inkView) {
        this.activity = activity;
        this.inkView = inkView;
    }
}
