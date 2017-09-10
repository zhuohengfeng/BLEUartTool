package com.podoon.bleuarttool.uart;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.support.v4.widget.CursorAdapter;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.podoon.bleuarttool.R;

import java.util.Calendar;

import no.nordicsemi.android.log.LogContract;

/**
 * Created by zhuohf1 on 2017/9/5.
 */

public class UARTLogAdapter extends CursorAdapter {
    private static final SparseIntArray mColors = new SparseIntArray();

    static {
        mColors.put(LogContract.Log.Level.DEBUG, 0xFF009CDE);
        mColors.put(LogContract.Log.Level.VERBOSE, 0xFFB8B056);
        mColors.put(LogContract.Log.Level.INFO, Color.BLACK);
        mColors.put(LogContract.Log.Level.APPLICATION, 0xFF238C0F);
        mColors.put(LogContract.Log.Level.WARNING, 0xFFD77926);
        mColors.put(LogContract.Log.Level.ERROR, Color.RED);
    }

    public UARTLogAdapter(Context context) {
        super(context, null, 0);
    }

    @Override
    public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
        final View view = LayoutInflater.from(context).inflate(R.layout.log_item, parent, false);

        final ViewHolder holder = new ViewHolder();
        holder.time = (TextView) view.findViewById(R.id.time);
        holder.data = (TextView) view.findViewById(R.id.data);
        view.setTag(holder);
        return view;
    }

    /*
    *
    *   从源码中可以看出CursorAdapter是继承了BaseAdapter后覆盖它的getView方法在getView方法中调用了newView和bindView方法，
    *   我们在写CursorAdapter时必须实现它的两个方法
    *
    *   (1)newView：并不是每次都被调用的，它只在实例化的时候调用,数据增加的时候也会调用,但是在重绘(比如修改条目里的TextView的内容)的时候不会被调用
(2)bindView：从代码中可以看出在绘制Item之前一定会调用bindView方法它在重绘的时候也同样被调用
    * */

    @Override
    public void bindView(final View view, final Context context, final Cursor cursor) {
        final ViewHolder holder = (ViewHolder) view.getTag();
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(cursor.getLong(1 /* TIME */));
        holder.time.setText(context.getString(R.string.log, calendar));

        final int level = cursor.getInt(2 /* LEVEL */);
        holder.data.setText(cursor.getString(3 /* DATA */));
        holder.data.setTextColor(mColors.get(level));
    }

    @Override
    public boolean isEnabled(int position) {
        return false;
    }

    private class ViewHolder {
        private TextView time;
        private TextView data;
    }

}
