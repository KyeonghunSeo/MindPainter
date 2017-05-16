package com.hellowo.mindpainter.utils;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.DatePicker;

import java.util.Calendar;

public class DialogUtil {

    /**
     * 다이얼로그 보여주기
     * @param dialog 다이어로그
     * @param is_cancelable 백키로 종료하기
     * @param is_dim 배경 어둡게 하지 않기
     * @param is_backgroun_transparent 배경 투명하게 하기
     * @param is_touchable_outside 외부 터치 가능하게 하기
     */
    public static void showDialog(Dialog dialog, final boolean is_cancelable,
                                  boolean is_dim, boolean is_backgroun_transparent,
                                  boolean is_touchable_outside){
        try{
            dialog.setCancelable(is_cancelable); // 백키로 종료하기
            dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
                    if(!is_cancelable && keyEvent.getAction() == KeyEvent.KEYCODE_BACK){
                        return true;
                    }
                    return false;
                }
            });
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // 타이틀 숨기기
            if(!is_dim){ // 배경 어둡게 하지 않기
                dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            }
            if(is_backgroun_transparent){ // 배경 투명하게 하기
                dialog.getWindow().setBackgroundDrawable
                        (new ColorDrawable(android.graphics.Color.TRANSPARENT));
            }
            if(is_touchable_outside){ // 외부 터치 가능하게 하기
                dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
            }
            dialog.show();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static DatePickerDialog createDialogWithoutDateField(Context context,
                                                                DatePickerDialog.OnDateSetListener callBack,
                                                                Calendar calendar) {
        DatePickerDialog dpd = new DatePickerDialog(context, callBack,
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        try {
            java.lang.reflect.Field[] datePickerDialogFields = dpd.getClass().getDeclaredFields();
            for (java.lang.reflect.Field datePickerDialogField : datePickerDialogFields) {
                if (datePickerDialogField.getName().equals("mDatePicker")) {
                    datePickerDialogField.setAccessible(true);
                    DatePicker datePicker = (DatePicker) datePickerDialogField.get(dpd);
                    java.lang.reflect.Field[] datePickerFields = datePickerDialogField.getType().getDeclaredFields();
                    for (java.lang.reflect.Field datePickerField : datePickerFields) {
                        Log.i("test", datePickerField.getName());
                        if ("mDaySpinner".equals(datePickerField.getName())) {
                            datePickerField.setAccessible(true);
                            Object dayPicker = datePickerField.get(datePicker);
                            ((View) dayPicker).setVisibility(View.GONE);
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
        }
        return dpd;
    }

}
