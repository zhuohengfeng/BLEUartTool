package com.podoon.bleuarttool.profile;

import android.bluetooth.BluetoothGatt;

/**
 * Created by zhuohf1 on 2017/9/5.
 */

public class GattError {

    // Starts at line 106 of gatt_api.h file
    /**
     * Converts the connection status given by the {@link android.bluetooth.BluetoothGattCallback#onConnectionStateChange(BluetoothGatt, int, int)} to error name.
     * @param error the status number
     * @return the error name as stated in the gatt_api.h file
     */
    public static String parseConnectionError(final int error) {
        switch (error) {
            case BluetoothGatt.GATT_SUCCESS:
                return "SUCCESS";
            case 0x01:
                return "GATT CONN L2C FAILURE";
            case 0x08:
                return "GATT CONN TIMEOUT";
            case 0x13:
                return "GATT CONN TERMINATE PEER USER";
            case 0x16:
                return "GATT CONN TERMINATE LOCAL HOST";
            case 0x3E:
                return "GATT CONN FAIL ESTABLISH";
            case 0x22:
                return "GATT CONN LMP TIMEOUT";
            case 0x0100:
                return "GATT CONN CANCEL ";
            case 0x0085:
                return "GATT ERROR"; // Device not reachable
            default:
                return "UNKNOWN (" + error + ")";
        }
    }

    // Starts at line 29 of the gatt_api.h file
    /**
     * Converts the bluetooth communication status given by other BluetoothGattCallbacks to error name. It also parses the DFU errors.
     * @param error the status number
     * @return the error name as stated in the gatt_api.h file
     */
    public static String parse(final int error) {
        switch (error) {
            case 0x0001:
                return "GATT INVALID HANDLE";
            case 0x0002:
                return "GATT READ NOT PERMIT";
            case 0x0003:
                return "GATT WRITE NOT PERMIT";
            case 0x0004:
                return "GATT INVALID PDU";
            case 0x0005:
                return "GATT INSUF AUTHENTICATION";
            case 0x0006:
                return "GATT REQ NOT SUPPORTED";
            case 0x0007:
                return "GATT INVALID OFFSET";
            case 0x0008:
                return "GATT INSUF AUTHORIZATION";
            case 0x0009:
                return "GATT PREPARE Q FULL";
            case 0x000a:
                return "GATT NOT FOUND";
            case 0x000b:
                return "GATT NOT LONG";
            case 0x000c:
                return "GATT INSUF KEY SIZE";
            case 0x000d:
                return "GATT INVALID ATTR LEN";
            case 0x000e:
                return "GATT ERR UNLIKELY";
            case 0x000f:
                return "GATT INSUF ENCRYPTION";
            case 0x0010:
                return "GATT UNSUPPORT GRP TYPE";
            case 0x0011:
                return "GATT INSUF RESOURCE";
            case 0x0087:
                return "GATT ILLEGAL PARAMETER";
            case 0x0080:
                return "GATT NO RESOURCES";
            case 0x0081:
                return "GATT INTERNAL ERROR";
            case 0x0082:
                return "GATT WRONG STATE";
            case 0x0083:
                return "GATT DB FULL";
            case 0x0084:
                return "GATT BUSY";
            case 0x0085:
                return "GATT ERROR";
            case 0x0086:
                return "GATT CMD STARTED";
            case 0x0088:
                return "GATT PENDING";
            case 0x0089:
                return "GATT AUTH FAIL";
            case 0x008a:
                return "GATT MORE";
            case 0x008b:
                return "GATT INVALID CFG";
            case 0x008c:
                return "GATT SERVICE STARTED";
            case 0x008d:
                return "GATT ENCRYPTED NO MITM";
            case 0x008e:
                return "GATT NOT ENCRYPTED";
            case 0x008f:
                return "GATT CONGESTED";
            case 0x00FD:
                return "GATT CCCD CFG ERROR";
            case 0x00FE:
                return "GATT PROCEDURE IN PROGRESS";
            case 0x00FF:
                return "GATT VALUE OUT OF RANGE";
            case 0x0101:
                return "TOO MANY OPEN CONNECTIONS";
            default:
                return "UNKNOWN (" + error + ")";
        }
    }
}
