package com.hellowo.mindpainter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.hellowo.mindpainter.utils.ViewUtil;

import java.io.File;

import de.hdodenhof.circleimageview.CircleImageView;

public class QuestionView extends FrameLayout {
    public QuestionView(Context context) {
        super(context);
        init();
    }

    public QuestionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public QuestionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private int chipSize;
    private int chipMargin;
    private int maxWidth;
    private int maxChipWidth;

    private String question;

    public void init(){
        chipSize = ViewUtil.dpToPx(getContext(), 40);
        chipMargin = ViewUtil.dpToPx(getContext(), 5);
    }

    public void setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
    }

    public void setChipSize(int chipSize) {
        this.chipSize = chipSize;
    }

    public void makeStudentChips(@NonNull String question) {
        this.question = question;

        removeAllViews();

        if(question.length() > 0) {

            maxChipWidth = question.length() * chipSize + (question.length() - 1) * chipMargin;

            for (int i = 0; i < question.length(); i++) {
                makeStudentChip(question.charAt(i), i);
            }

        }
    }

    private void makeStudentChip(final char c, final int position) {
        TextView textView = new TextView(getContext());
        textView.setLayoutParams(new LayoutParams(chipSize, chipSize));
        textView.setBackgroundResource(R.drawable.blue_circle_fill);
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 25);
        textView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        textView.setGravity(Gravity.CENTER);
        textView.setText("?");

        if(maxChipWidth > maxWidth) {
            textView.setTranslationX((maxWidth - chipSize) / question.length() * position);
        }else {
            textView.setTranslationX(chipSize * position + chipMargin * position);
        }

        addView(textView);

    }
}
