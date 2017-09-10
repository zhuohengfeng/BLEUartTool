package com.podoon.bleuarttool.profile;

import android.support.annotation.StringRes;

/**
 * Created by zhuohf1 on 2017/9/5.
 */

public interface ILogger {

    /**
     * Logs the given message with given log level into the all managed devices' log session.
     * @param level the log level
     * @param message the message to be logged
     */
    void log(final int level, final String message);

    /**
     * Logs the given message with given log level into the all managed devices' log session.
     * @param level the log level
     * @param messageRes string resource id
     * @param params additional (optional) parameters used to fill the message
     */
    void log(final int level, @StringRes final int messageRes, final Object... params);
}
