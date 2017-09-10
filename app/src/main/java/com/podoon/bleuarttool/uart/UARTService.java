package com.podoon.bleuarttool.uart;

import android.app.NotificationManager;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.podoon.bleuarttool.R;
import com.podoon.bleuarttool.global.Constants;
import com.podoon.bleuarttool.profile.BleManager;
import com.podoon.bleuarttool.profile.BleProfileService;

import no.nordicsemi.android.log.Logger;

/**
 * Created by zhuohf1 on 2017/9/5.
 */

public class UARTService extends BleProfileService implements UARTManagerCallbacks {
    private static final String TAG = "UARTService";

    public static final String BROADCAST_UART_TX = "no.nordicsemi.android.nrftoolbox.uart.BROADCAST_UART_TX";
    public static final String BROADCAST_UART_RX = "no.nordicsemi.android.nrftoolbox.uart.BROADCAST_UART_RX";
    public static final String EXTRA_DATA = "no.nordicsemi.android.nrftoolbox.uart.EXTRA_DATA";

    /** A broadcast message with this action and the message in {@link Intent#EXTRA_TEXT} will be sent t the UART device. */
    public final static String ACTION_SEND = "no.nordicsemi.android.nrftoolbox.uart.ACTION_SEND";
    /** A broadcast message with this action is triggered when a message is received from the UART device. */
    private final static String ACTION_RECEIVE = "no.nordicsemi.android.nrftoolbox.uart.ACTION_RECEIVE";
    /** Action send when user press the DISCONNECT button on the notification. */
    public final static String ACTION_DISCONNECT = "no.nordicsemi.android.nrftoolbox.uart.ACTION_DISCONNECT";
    /** A source of an action. */
    public final static String EXTRA_SOURCE = "no.nordicsemi.android.nrftoolbox.uart.EXTRA_SOURCE";
    public final static int SOURCE_NOTIFICATION = 0;
    public final static int SOURCE_WEARABLE = 1;
    public final static int SOURCE_3RD_PARTY = 2;

    private final static int NOTIFICATION_ID = 349; // random
    private final static int OPEN_ACTIVITY_REQ = 67; // random
    private final static int DISCONNECT_REQ = 97; // random

    //private GoogleApiClient mGoogleApiClient;
    private UARTManager mManager;

    // 本地的一个localBinder实现，为了在onBinder的时候返回
    private final LocalBinder mBinder = new UARTBinder();

    public class UARTBinder extends LocalBinder implements UARTInterface {
        // 增加了一个send方法
        @Override
        public void send(final String text) {
            mManager.send(text);
        }
    }

    @Override
    protected LocalBinder getBinder() {
        return mBinder;
    }

    @Override
    protected BleManager<UARTManagerCallbacks> initializeManager() {
        return mManager = new UARTManager(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        registerReceiver(mDisconnectActionBroadcastReceiver, new IntentFilter(ACTION_DISCONNECT));
        registerReceiver(mIntentBroadcastReceiver, new IntentFilter(ACTION_SEND));

//        mGoogleApiClient = new GoogleApiClient.Builder(this)
//                .addApi(Wearable.API)
//                .build();
//        mGoogleApiClient.connect();
    }

    @Override
    public void onDestroy() {
        // when user has disconnected from the sensor, we have to cancel the notification that we've created some milliseconds before using unbindService
        cancelNotification();
        unregisterReceiver(mDisconnectActionBroadcastReceiver);
        unregisterReceiver(mIntentBroadcastReceiver);

        //mGoogleApiClient.disconnect();

        super.onDestroy();
    }

    @Override
    protected void onRebind() {
        // when the activity rebinds to the service, remove the notification
        //cancelNotification();
    }

    @Override
    protected void onUnbind() {
        // when the activity closes we need to show the notification that user is connected to the sensor
        //createNotification(R.string.uart_notification_connected_message, 0);
    }

    @Override
    public boolean shouldEnableBatteryLevelNotifications(final BluetoothDevice device) {
        // No UI in UART profile for Battery Level information
        return false;
    }

    @Override
    public void onDeviceConnected(final BluetoothDevice device) {
        super.onDeviceConnected(device);
        //sendMessageToWearables(Constants.UART.DEVICE_CONNECTED, notNull(getDeviceName()));
    }

    @Override
    protected boolean stopWhenDisconnected() {
        return false;
    }

    @Override
    public void onDeviceDisconnected(final BluetoothDevice device) {
        super.onDeviceDisconnected(device);
        //sendMessageToWearables(Constants.UART.DEVICE_DISCONNECTED, notNull(getDeviceName()));
    }

    @Override
    public void onLinklossOccur(final BluetoothDevice device) {
        super.onLinklossOccur(device);
        //sendMessageToWearables(Constants.UART.DEVICE_LINKLOSS, notNull(getDeviceName()));
    }

    private String notNull(final String name) {
        if (!TextUtils.isEmpty(name))
            return name;
        return getString(R.string.not_available);
    }

    public void onDataReceived(final BluetoothDevice device, final String data) {
        final Intent broadcast = new Intent(BROADCAST_UART_RX);
        broadcast.putExtra(EXTRA_DEVICE, getBluetoothDevice());
        broadcast.putExtra(EXTRA_DATA, data);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);

        // send the data received to other apps, e.g. the Tasker
        final Intent globalBroadcast = new Intent(ACTION_RECEIVE);
        globalBroadcast.putExtra(BluetoothDevice.EXTRA_DEVICE, getBluetoothDevice());
        globalBroadcast.putExtra(Intent.EXTRA_TEXT, data);
        sendBroadcast(globalBroadcast);
    }

    @Override
    public void onDataSent(final BluetoothDevice device, final String data) {
        final Intent broadcast = new Intent(BROADCAST_UART_TX);
        broadcast.putExtra(EXTRA_DEVICE, getBluetoothDevice());
        broadcast.putExtra(EXTRA_DATA, data);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
    }

    /**
     * Sends the given message to all connected wearables. If the path is equal to Constants.UART#DEVICE_DISCONNECTED} the service will be stopped afterwards.
     * @param path message path
     * @param message the message
     */
//    private void sendMessageToWearables(final @NonNull String path, final @NonNull String message) {
//        if(mGoogleApiClient.isConnected()) {
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
//                    for(Node node : nodes.getNodes()) {
//                        Logger.v(getLogSession(), "[WEAR] Sending message '" + path + "' to " + node.getDisplayName());
//                        final MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), path, message.getBytes()).await();
//                        if(result.getStatus().isSuccess()){
//                            Logger.i(getLogSession(), "[WEAR] Message sent");
//                        } else {
//                            Logger.w(getLogSession(), "[WEAR] Sending message failed: " + result.getStatus().getStatusMessage());
//                            Log.w(TAG, "Failed to send " + path + " to " + node.getDisplayName());
//                        }
//                    }
//                    if (Constants.UART.DEVICE_DISCONNECTED.equals(path))
//                        stopService();
//                }
//            }).start();
//        } else {
//            if (Constants.UART.DEVICE_DISCONNECTED.equals(path))
//                stopService();
//        }
//    }

    /**
     * Creates the notification
     *
     * @param messageResId
     *            message resource id. The message must have one String parameter,<br />
     *            f.e. <code>&lt;string name="name"&gt;%s is connected&lt;/string&gt;</code>
     * @param defaults
     *            signals that will be used to notify the user
     */
//    private void createNotification(final int messageResId, final int defaults) {
//        final Intent parentIntent = new Intent(this, FeaturesActivity.class);
//        parentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        final Intent targetIntent = new Intent(this, UARTActivity.class);
//
//        final Intent disconnect = new Intent(ACTION_DISCONNECT);
//        disconnect.putExtra(EXTRA_SOURCE, SOURCE_NOTIFICATION);
//        final PendingIntent disconnectAction = PendingIntent.getBroadcast(this, DISCONNECT_REQ, disconnect, PendingIntent.FLAG_UPDATE_CURRENT);
//
//        // both activities above have launchMode="singleTask" in the AndroidManifest.xml file, so if the task is already running, it will be resumed
//        final PendingIntent pendingIntent = PendingIntent.getActivities(this, OPEN_ACTIVITY_REQ, new Intent[] { parentIntent, targetIntent }, PendingIntent.FLAG_UPDATE_CURRENT);
//        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
//        builder.setContentIntent(pendingIntent);
//        builder.setContentTitle(getString(R.string.app_name)).setContentText(getString(messageResId, getDeviceName()));
//        builder.setSmallIcon(R.drawable.ic_stat_notify_uart);
//        builder.setShowWhen(defaults != 0).setDefaults(defaults).setAutoCancel(true).setOngoing(true);
//        builder.addAction(new NotificationCompat.Action(R.drawable.ic_action_bluetooth, getString(R.string.uart_notification_action_disconnect), disconnectAction));
//
//        final Notification notification = builder.build();
//        final NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        nm.notify(NOTIFICATION_ID, notification);
//    }

    /**
     * Cancels the existing notification. If there is no active notification this method does nothing
     */
    private void cancelNotification() {
        final NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancel(NOTIFICATION_ID);
    }

    /**
     * This broadcast receiver listens for {@link #ACTION_DISCONNECT} that may be fired by pressing Disconnect action button on the notification.
     */
    private final BroadcastReceiver mDisconnectActionBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final int source = intent.getIntExtra(EXTRA_SOURCE, SOURCE_NOTIFICATION);
            switch (source) {
                case SOURCE_NOTIFICATION:
                    Logger.i(getLogSession(), "[Notification] Disconnect action pressed");
                    break;
                case SOURCE_WEARABLE:
                    Logger.i(getLogSession(), "[WEAR] '" + Constants.ACTION_DISCONNECT + "' message received");
                    break;
            }
            if (isConnected())
                getBinder().disconnect();
            else
                stopSelf();
        }
    };

    /**
     * Broadcast receiver that listens for {@link #ACTION_SEND} from other apps. Sends the String or int content of the {@link Intent#EXTRA_TEXT} extra to the remote device.
     * The integer content will be sent as String (65 -> "65", not 65 -> "A").
     */
    private BroadcastReceiver mIntentBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final boolean hasMessage = intent.hasExtra(Intent.EXTRA_TEXT);
            if (hasMessage) {
                String message = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (message == null) {
                    final int intValue = intent.getIntExtra(Intent.EXTRA_TEXT, Integer.MIN_VALUE); // how big is the chance of such data?
                    if (intValue != Integer.MIN_VALUE)
                        message = String.valueOf(intValue);
                }

                if (message != null) {
                    final int source = intent.getIntExtra(EXTRA_SOURCE, SOURCE_3RD_PARTY);
                    switch (source) {
                        case SOURCE_WEARABLE:
                            Logger.i(getLogSession(), "[WEAR] '" + Constants.UART.COMMAND + "' message received with data: \"" + message + "\"");
                            break;
                        case SOURCE_3RD_PARTY:
                        default:
                            Logger.i(getLogSession(), "[Broadcast] " + ACTION_SEND + " broadcast received with data: \"" + message + "\"");
                            break;
                    }
                    mManager.send(message);
                    return;
                }
            }
            // No data od incompatible type of EXTRA_TEXT
            if (!hasMessage)
                Logger.i(getLogSession(), "[Broadcast] " + ACTION_SEND + " broadcast received no data.");
            else
                Logger.i(getLogSession(), "[Broadcast] " + ACTION_SEND + " broadcast received incompatible data type. Only String and int are supported.");
        }
    };
}