package com.podoon.bleuarttool.util;

import android.util.Log;

import com.podoon.bleuarttool.global.Constants;

/**
 * Created by zhuohf1 on 2017/9/5.
 */

public class DebugLogger {
    public static void v(final String tag, final String text) {
        if (Constants.DEBUG)
            Log.v(tag, text);
    }

    public static void d(final String tag, final String text) {
        if (Constants.DEBUG) {
            Log.d(tag, text);
        }
    }

    public static void i(final String tag, final String text) {
        if (Constants.DEBUG)
            Log.i(tag, text);
    }

    public static void w(final String tag, final String text) {
        if (Constants.DEBUG) {
            Log.w(tag, text);
        }
    }

    public static void e(final String tag, final String text) {
        if (Constants.DEBUG)
            Log.e(tag, text);
    }

    public static void e(final String tag, final String text, final Throwable e) {
        if (Constants.DEBUG)
            Log.e(tag, text, e);
    }

    public static void wtf(final String tag, final String text) {
        if (Constants.DEBUG) {
            Log.wtf(tag, text);
        }
    }

    public static void wtf(final String tag, final String text, final Throwable e) {
        if (Constants.DEBUG) {
            Log.wtf(tag, text, e);
        }
    }
}

