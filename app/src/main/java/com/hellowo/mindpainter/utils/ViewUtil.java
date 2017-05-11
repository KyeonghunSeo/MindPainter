package com.hellowo.mindpainter.utils;

import android.content.Context;
import android.util.TypedValue;

public class ViewUtil {
    public static int dpToPx(Context context, float dp) {
        float px = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics()
        );
        return (int) px;
    }
}
