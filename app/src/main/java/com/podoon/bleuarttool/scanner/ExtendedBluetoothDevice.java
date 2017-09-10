package com.podoon.bleuarttool.scanner;

import android.bluetooth.BluetoothDevice;

import no.nordicsemi.android.support.v18.scanner.ScanResult;

/**
 * Created by zhuohf1 on 2017/9/5.
 */

public class ExtendedBluetoothDevice {
    /* package */ static final int NO_RSSI = -1000;
    public final BluetoothDevice device;
    public String name;
    public int rssi;
    public boolean isBonded;

    public ExtendedBluetoothDevice(final ScanResult scanResult) {
        this.device = scanResult.getDevice();
        this.name = scanResult.getScanRecord() != null ? scanResult.getScanRecord().getDeviceName() : null;
        this.rssi = scanResult.getRssi();
        this.isBonded = false;
    }

    public ExtendedBluetoothDevice(final BluetoothDevice device) {
        this.device = device;
        this.name = device.getName();
        this.rssi = NO_RSSI;
        this.isBonded = true;
    }

    public boolean matches(final ScanResult scanResult) {
        return device.getAddress().equals(scanResult.getDevice().getAddress());
    }


}

