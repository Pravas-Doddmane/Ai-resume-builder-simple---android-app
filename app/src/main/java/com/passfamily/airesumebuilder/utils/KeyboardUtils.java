package com.passfamily.airesumebuilder.utils;

import android.app.Activity;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

public class KeyboardUtils {

    public static void hideKeyboard(Activity activity) {
        if (activity == null || activity.isFinishing()) return;

        View view = activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            view.clearFocus();
        }
    }

    public static void setupHideKeyboardOnTouch(View rootView, Activity activity) {
        rootView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                hideKeyboard(activity);
            }
            return false;
        });
    }

    // Method to handle touch events and hide keyboard when tapping outside EditText
    public static boolean dispatchTouchEvent(Activity activity, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = activity.getCurrentFocus();
            if (v instanceof EditText) {
                int[] outLocation = new int[2];
                v.getLocationOnScreen(outLocation);
                float x = event.getRawX();
                float y = event.getRawY();

                // Check if the touch is outside the EditText
                if (x < outLocation[0] || x > outLocation[0] + v.getWidth() ||
                        y < outLocation[1] || y > outLocation[1] + v.getHeight()) {
                    v.clearFocus();
                    hideKeyboard(activity);
                }
            }
        }
        return false;
    }
}