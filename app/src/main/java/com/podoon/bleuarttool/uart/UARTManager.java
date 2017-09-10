package com.podoon.bleuarttool.uart;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.text.TextUtils;

import com.podoon.bleuarttool.profile.BleManager;
import com.podoon.bleuarttool.util.DebugLogger;

import java.io.UnsupportedEncodingException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.UUID;

import no.nordicsemi.android.log.Logger;

/**
 * Created by zhuohf1 on 2017/9/5.
 */

public class UARTManager extends BleManager<UARTManagerCallbacks> {
    /** Nordic UART Service UUID */
    private final static UUID UART_SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    /** RX characteristic UUID */
    private final static UUID UART_RX_CHARACTERISTIC_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    /** TX characteristic UUID */
    private final static UUID UART_TX_CHARACTERISTIC_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
    /** The maximum packet size is 20 bytes. */
    private static final int MAX_PACKET_SIZE = 20;

    private BluetoothGattCharacteristic mRXCharacteristic, mTXCharacteristic;
    private byte[] mOutgoingBuffer;
    private int mBufferOffset;

    public UARTManager(final Context context) {
        super(context);
    }

    @Override
    protected BleManagerGattCallback getGattCallback() {
        return mGattCallback;
    }

    /**
     * BluetoothGatt callbacks for connection/disconnection, service discovery, receiving indication, etc
     */
    // gatt，一个设备有多个service，一个service有多个characteristic，当调用device的connect方法后会执行这个回调
    private final BleManagerGattCallback mGattCallback = new BleManagerGattCallback() {

        @Override
        protected Deque<Request> initGatt(final BluetoothGatt gatt) {
            final LinkedList<Request> requests = new LinkedList<>();
            requests.add(Request.newEnableNotificationsRequest(mTXCharacteristic));
            return requests;
        }

        @Override
        public boolean isRequiredServiceSupported(final BluetoothGatt gatt) {
            final BluetoothGattService service = gatt.getService(UART_SERVICE_UUID); // uart service
            if (service != null) {
                mRXCharacteristic = service.getCharacteristic(UART_RX_CHARACTERISTIC_UUID); // read character
                mTXCharacteristic = service.getCharacteristic(UART_TX_CHARACTERISTIC_UUID); // write character
            }

            boolean writeRequest = false;
            boolean writeCommand = false;
            if (mRXCharacteristic != null) {
                final int rxProperties = mRXCharacteristic.getProperties(); // 获取接收的特征属性
                writeRequest = (rxProperties & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0;
                writeCommand = (rxProperties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0;

                // Set the WRITE REQUEST type when the characteristic supports it. This will allow to send long write (also if the characteristic support it).
                // In case there is no WRITE REQUEST property, this manager will divide texts longer then 20 bytes into up to 20 bytes chunks.
                if (writeRequest)
                    mRXCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            }

            boolean support = mRXCharacteristic != null && mTXCharacteristic != null && (writeRequest || writeCommand);
            DebugLogger.d("zhfzhf", "isRequiredServiceSupported support="+support);
            return support;
        }

        @Override
        protected void onDeviceDisconnected() {
            mRXCharacteristic = null;
            mTXCharacteristic = null;
        }

        @Override
        public void onCharacteristicWrite(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            // When the whole buffer has been sent
            final byte[] buffer = mOutgoingBuffer;
            if (mBufferOffset == buffer.length) {
                try {
                    final String data = new String(buffer, "UTF-8");
                    Logger.a(mLogSession, "\"" + data + "\" sent");
                    mCallbacks.onDataSent(gatt.getDevice(), data);
                } catch (final UnsupportedEncodingException e) {
                    // do nothing
                }
                mOutgoingBuffer = null;
            } else { // Otherwise...
                final int length = Math.min(buffer.length - mBufferOffset, MAX_PACKET_SIZE);
                enqueue(Request.newWriteRequest(mRXCharacteristic, buffer, mBufferOffset, length));
                mBufferOffset += length;
            }
        }

        @Override
        public void onCharacteristicNotified(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            final String data = characteristic.getStringValue(0);
            Logger.a(mLogSession, "\"" + data + "\" received");
            mCallbacks.onDataReceived(gatt.getDevice(), data);
        }
    };

    @Override
    protected boolean shouldAutoConnect() {
        // We want the connection to be kept
        return true;
    }

    /**
     * Sends the given text to RX characteristic.
     * @param text the text to be sent
     */
    public void send(final String text) {
        // Are we connected?
        if (mRXCharacteristic == null)
            return;

        // An outgoing buffer may not be null if there is already another packet being sent. We do nothing in this case.
        if (!TextUtils.isEmpty(text) && mOutgoingBuffer == null) {
            final byte[] buffer = mOutgoingBuffer = text.getBytes();
            mBufferOffset = 0;

            // Depending on whether the characteristic has the WRITE REQUEST property or not, we will either send it as it is (hoping the long write is implemented),
            // or divide it into up to 20 bytes chunks and send them one by one.
            final boolean writeRequest = (mRXCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0;

            if (!writeRequest) { // no WRITE REQUEST property
                final int length = Math.min(buffer.length, MAX_PACKET_SIZE);
                mBufferOffset += length;
                enqueue(Request.newWriteRequest(mRXCharacteristic, buffer, 0, length));
            } else { // there is WRITE REQUEST property, let's try Long Write
                mBufferOffset = buffer.length;
                enqueue(Request.newWriteRequest(mRXCharacteristic, buffer, 0, buffer.length));
            }
        }
    }
}
