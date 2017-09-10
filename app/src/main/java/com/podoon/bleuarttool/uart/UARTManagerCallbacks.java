package com.podoon.bleuarttool.uart;

import android.bluetooth.BluetoothDevice;

import com.podoon.bleuarttool.profile.BleManagerCallbacks;

/**
 * Created by zhuohf1 on 2017/9/5.
 */

public interface UARTManagerCallbacks extends BleManagerCallbacks {

    void onDataReceived(final BluetoothDevice device, final String data);

    void onDataSent(final BluetoothDevice device, final String data);
}
