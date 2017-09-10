package com.podoon.bleuarttool;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.widget.AppCompatTextView;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.podoon.bleuarttool.profile.BleManagerCallbacks;
import com.podoon.bleuarttool.profile.BleProfileService;
import com.podoon.bleuarttool.scanner.ScannerFragment;
import com.podoon.bleuarttool.uart.UARTInterface;
import com.podoon.bleuarttool.uart.UARTLocalLogContentProvider;
import com.podoon.bleuarttool.uart.UARTLogAdapter;
import com.podoon.bleuarttool.uart.UARTService;
import com.podoon.bleuarttool.util.DebugLogger;

import java.util.UUID;

import no.nordicsemi.android.log.ILogSession;
import no.nordicsemi.android.log.LocalLogSession;
import no.nordicsemi.android.log.LogContract;
import no.nordicsemi.android.log.Logger;

public class MainActivity extends BaseActivity implements
        ScannerFragment.OnDeviceSelectedListener, BleManagerCallbacks, LoaderManager.LoaderCallbacks<Cursor>  {
    private static final String TAG = "MainActivity";

    private static final String SIS_DEVICE_NAME = "device_name";
    private static final String SIS_DEVICE = "device";
    private static final String LOG_URI = "log_uri";


    private UARTService.UARTBinder mServiceBinder;

    private AppCompatTextView mDeviceNameView;
    //private TextView mBatteryLevelView;
    //private Button mConnectButton;

    private ListView mLogsListView;

    private BluetoothDevice mBluetoothDevice;
    private String mDeviceName;

    private static final String SIS_LOG_SCROLL_POSITION = "sis_scroll_position";
    private static final int LOG_SCROLL_NULL = -1;
    private static final int LOG_SCROLLED_TO_BOTTOM = -2;

    private static final int LOG_REQUEST_ID = 1;
    private static final String[] LOG_PROJECTION = { LogContract.Log._ID, LogContract.Log.TIME, LogContract.Log.LEVEL, LogContract.Log.DATA };

    /** The service UART interface that may be used to send data to the target. */
    private UARTInterface mUARTInterface;
    /** The adapter used to populate the list with log entries. */
    private CursorAdapter mLogAdapter;
    /** The log session created to log events related with the target device. */
    private ILogSession mLogSession;

    private EditText mField;
    private Button mSendButton;

    /** The last list view position. */
    private int mLogScrollPosition;


    private final BroadcastReceiver mCommonBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            // Check if the broadcast applies the connected device
            if (!isBroadcastForThisDevice(intent))
                return;

            final BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BleProfileService.EXTRA_DEVICE);
            final String action = intent.getAction();
            switch (action) {
                case BleProfileService.BROADCAST_CONNECTION_STATE: {
                    final int state = intent.getIntExtra(BleProfileService.EXTRA_CONNECTION_STATE, BleProfileService.STATE_DISCONNECTED);

                    switch (state) {
                        case BleProfileService.STATE_CONNECTED: {
                            mDeviceName = intent.getStringExtra(BleProfileService.EXTRA_DEVICE_NAME);
                            onDeviceConnected(bluetoothDevice);
                            break;
                        }
                        case BleProfileService.STATE_DISCONNECTED: {
                            onDeviceDisconnected(bluetoothDevice);
                            mDeviceName = null;
                            break;
                        }
                        case BleProfileService.STATE_LINK_LOSS: {
                            onLinklossOccur(bluetoothDevice);
                            break;
                        }
                        case BleProfileService.STATE_CONNECTING: {
                            onDeviceConnecting(bluetoothDevice);
                            break;
                        }
                        case BleProfileService.STATE_DISCONNECTING: {
                            onDeviceDisconnecting(bluetoothDevice);
                            break;
                        }
                        default:
                            // there should be no other actions
                            break;
                    }
                    break;
                }
                case BleProfileService.BROADCAST_SERVICES_DISCOVERED: {
                    final boolean primaryService = intent.getBooleanExtra(BleProfileService.EXTRA_SERVICE_PRIMARY, false);
                    final boolean secondaryService = intent.getBooleanExtra(BleProfileService.EXTRA_SERVICE_SECONDARY, false);

                    if (primaryService) {
                        onServicesDiscovered(bluetoothDevice, secondaryService);
                    } else {
                        onDeviceNotSupported(bluetoothDevice);
                    }
                    break;
                }
                case BleProfileService.BROADCAST_DEVICE_READY: {
                    onDeviceReady(bluetoothDevice);
                    break;
                }
                case BleProfileService.BROADCAST_BOND_STATE: {
                    final int state = intent.getIntExtra(BleProfileService.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
                    switch (state) {
                        case BluetoothDevice.BOND_BONDING:
                            onBondingRequired(bluetoothDevice);
                            break;
                        case BluetoothDevice.BOND_BONDED:
                            onBonded(bluetoothDevice);
                            break;
                    }
                    break;
                }
                case BleProfileService.BROADCAST_BATTERY_LEVEL: {
                    final int value = intent.getIntExtra(BleProfileService.EXTRA_BATTERY_LEVEL, -1);
                    if (value > 0)
                        onBatteryValueReceived(bluetoothDevice, value);
                    break;
                }
                case BleProfileService.BROADCAST_ERROR: {
                    final String message = intent.getStringExtra(BleProfileService.EXTRA_ERROR_MESSAGE);
                    final int errorCode = intent.getIntExtra(BleProfileService.EXTRA_ERROR_CODE, 0);
                    onError(bluetoothDevice, message, errorCode);
                    break;
                }
            }
        }
    };

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @SuppressWarnings("unchecked")
        @Override
        public void onServiceConnected(final ComponentName name, final IBinder service) {
            final UARTService.UARTBinder bleService = mServiceBinder = (UARTService.UARTBinder) service;
            mBluetoothDevice = bleService.getBluetoothDevice();
            mLogSession = mServiceBinder.getLogSession();
            Logger.d(mLogSession, "Activity bound to the service");
            onServiceBinded(bleService);

            mUARTInterface = bleService;
            // Start the loader
            if (mLogSession != null) {
                getSupportLoaderManager().restartLoader(LOG_REQUEST_ID, null, MainActivity.this);
            }


            // Update UI
            mDeviceName = bleService.getDeviceName();
            mDeviceNameView.setText(mDeviceName);
            //mConnectButton.setText(R.string.action_disconnect);

            // And notify user if device is connected
            if (bleService.isConnected()) {
                onDeviceConnected(mBluetoothDevice);
            } else {
                // If the device is not connected it means that either it is still connecting,
                // or the link was lost and service is trying to connect to it (autoConnect=true).
                onDeviceConnecting(mBluetoothDevice);
            }
        }

        @Override
        public void onServiceDisconnected(final ComponentName name) {
            // Note: this method is called only when the service is killed by the system,
            // not when it stops itself or is stopped by the activity.
            // It will be called only when there is critically low memory, in practice never
            // when the activity is in foreground.
            Logger.d(mLogSession, "Activity disconnected from the service");

            mUARTInterface = null;

            mDeviceNameView.setText(getDefaultDeviceName());
            //mConnectButton.setText(R.string.action_connect);

            mServiceBinder = null;
            mDeviceName = null;
            mBluetoothDevice = null;
            mLogSession = null;
            onServiceUnbinded();
        }
    };

    @Override
    protected final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ensureBLESupported();
        if (!isBLEEnabled()) {
            showBLEDialog();
        }

        // Restore the old log session
        if (savedInstanceState != null) {
            final Uri logUri = savedInstanceState.getParcelable(LOG_URI);
            mLogSession = Logger.openSession(getApplicationContext(), logUri);

            mLogScrollPosition = savedInstanceState.getInt(SIS_LOG_SCROLL_POSITION);
        }

        // The onCreateView class should... create the view
        onCreateView(savedInstanceState);

        //final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        //setSupportActionBar(toolbar);

        LocalBroadcastManager.getInstance(this).registerReceiver(mCommonBroadcastReceiver, makeIntentFilter());
    }

    // 初始化View
    protected void onCreateView(final Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);

        // set GUI
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        mLogsListView = findViewById(R.id.list_logs);
        mLogAdapter = new UARTLogAdapter(this);
        mLogsListView.setAdapter(mLogAdapter);

        //mConnectButton = (Button) findViewById(R.id.action_connect);
        mDeviceNameView = (AppCompatTextView) findViewById(R.id.tv_device_name);
        // mBatteryLevelView = (TextView) findViewById(R.id.battery);


        final EditText field = mField = (EditText) findViewById(R.id.field);
        field.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(final TextView v, final int actionId, final KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    onSendClicked();
                    return true;
                }
                return false;
            }
        });

        final Button sendButton = mSendButton = (Button) findViewById(R.id.action_send);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                onSendClicked();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

		/*
		 * If the service has not been started before, the following lines will not start it. However, if it's running, the Activity will bind to it and
		 * notified via mServiceConnection.
		 */
        final Intent service = new Intent(this, getServiceClass());
        bindService(service, mServiceConnection, 0); // we pass 0 as a flag so the service will not be created if not exists

		/*
		 * * - When user exited the UARTActivity while being connected, the log session is kept in the service. We may not get it before binding to it so in this
		 * case this event will not be logged (mLogSession is null until onServiceConnected(..) is called). It will, however, be logged after the orientation changes.
		 */
    }

    @Override
    protected void onPause() {
        super.onPause();

        try {
            // We don't want to perform some operations (e.g. disable Battery Level notifications) in the service if we are just rotating the screen.
            // However, when the activity will disappear, we may want to disable some device features to reduce the battery consumption.
            if (mServiceBinder != null)
                mServiceBinder.setActivityIsChangingConfiguration(isChangingConfigurations());

            unbindService(mServiceConnection);
            mServiceBinder = null;
            mUARTInterface = null;

            Logger.d(mLogSession, "Activity unbound from the service");
            onServiceUnbinded();
            mDeviceName = null;
            mBluetoothDevice = null;
            mLogSession = null;

            // 隐藏输入法
            InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mField.getWindowToken(), 0);

        } catch (final IllegalArgumentException e) {
            // do nothing, we were not connected to the sensor
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mCommonBroadcastReceiver);
    }

    private static IntentFilter makeIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleProfileService.BROADCAST_CONNECTION_STATE);
        intentFilter.addAction(BleProfileService.BROADCAST_SERVICES_DISCOVERED);
        intentFilter.addAction(BleProfileService.BROADCAST_DEVICE_READY);
        intentFilter.addAction(BleProfileService.BROADCAST_BOND_STATE);
        intentFilter.addAction(BleProfileService.BROADCAST_BATTERY_LEVEL);
        intentFilter.addAction(BleProfileService.BROADCAST_ERROR);
        return intentFilter;
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);

        if(mLogsListView != null){
            final boolean scrolledToBottom = mLogsListView.getCount() > 0 && mLogsListView.getLastVisiblePosition() == mLogsListView.getCount() - 1;
            outState.putInt(SIS_LOG_SCROLL_POSITION, scrolledToBottom ? LOG_SCROLLED_TO_BOTTOM : mLogsListView.getFirstVisiblePosition());
        }

        outState.putString(SIS_DEVICE_NAME, mDeviceName);
        outState.putParcelable(SIS_DEVICE, mBluetoothDevice);
        if (mLogSession != null)
            outState.putParcelable(LOG_URI, mLogSession.getSessionUri());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mDeviceName = savedInstanceState.getString(SIS_DEVICE_NAME);
        mBluetoothDevice = savedInstanceState.getParcelable(SIS_DEVICE);
    }

    // 点击开始搜索设备
    public void onScannerClicked(final View view) {
        if (isBLEEnabled()) {
            if (mServiceBinder == null) {
                showDeviceScanningDialog(getFilterUUID());
            } else {
                mServiceBinder.disconnect();
            }
        } else {
            showBLEDialog();
        }
    }

    // 设置要过滤的UUID
    protected UUID getFilterUUID() {
        return null; // not used
    }

    // 点击开始发送数据
    public void onSendClicked() {
        final String text = mField.getText().toString();

        mUARTInterface.send(text);

        mField.setText(null);
        mField.requestFocus();
    }

    // 显示搜索框
    private void showDeviceScanningDialog(final UUID filter) {
        final ScannerFragment dialog = ScannerFragment.getInstance(filter);
        dialog.show(getSupportFragmentManager(), "scan_fragment");
    }

    protected void onServiceBinded(final UARTService.UARTBinder binder) {
        mServiceBinder = binder;
    }

    protected void onServiceUnbinded() {
        mServiceBinder = null;
    }

    protected boolean isBroadcastForThisDevice(final Intent intent) {
        final BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BleProfileService.EXTRA_DEVICE);
        return mBluetoothDevice != null && mBluetoothDevice.equals(bluetoothDevice);
    }

    // 需要绑定的是UART service
    protected Class<? extends BleProfileService> getServiceClass() {
        return UARTService.class;
    }

    protected UARTService.UARTBinder getService() {
        return mServiceBinder;
    }

    // 判断是否已经连接上设备了
    protected boolean isDeviceConnected() {
        return mServiceBinder != null && mServiceBinder.isConnected();
    }

    // 获取当前连接设备的名称
    protected String getDeviceName() {
        return mDeviceName;
    }

    // 显示一个默认的设备名
    protected int getDefaultDeviceName() {
        return R.string.uart_default_name;
    }







    // -------------------------------------------------
    /**
     * Returns the title resource id that will be used to create logger session. If 0 is returned (default) logger will not be used.
     *
     * @return the title resource id
     */
    protected int getLoggerProfileTitle() {
        return R.string.uart_feature_title;
    }

    /**
     * This method may return the local log content provider authority if local log sessions are supported.
     *
     * @return local log session content provider URI
     */
    protected Uri getLocalAuthorityLogger() {
        return UARTLocalLogContentProvider.AUTHORITY_URI;
    }
    // -------------------------------------------------


    //--------------------ScannerFragment.OnDeviceSelectedListener start--------------------
    @Override
    public void onDeviceSelected(final BluetoothDevice device, final String name) {
        final int titleId = getLoggerProfileTitle();
        if (titleId > 0) {
            mLogSession = Logger.newSession(getApplicationContext(), getString(titleId), device.getAddress(), name);
            // If nRF Logger is not installed we may want to use local logger
            if (mLogSession == null && getLocalAuthorityLogger() != null) {
                mLogSession = LocalLogSession.newSession(getApplicationContext(), getLocalAuthorityLogger(), device.getAddress(), name);
            }
        }
        mBluetoothDevice = device;
        mDeviceName = name;
        mDeviceNameView.setText(name != null ? name : getString(R.string.not_available));
        //mConnectButton.setText(R.string.action_connecting);

        // The device may not be in the range but the service will try to connect to it if it reach it
        Logger.d(mLogSession, "Creating service...");
        final Intent service = new Intent(this, getServiceClass());
        service.putExtra(BleProfileService.EXTRA_DEVICE_ADDRESS, device.getAddress());
        service.putExtra(BleProfileService.EXTRA_DEVICE_NAME, name);
        if (mLogSession != null)
            service.putExtra(BleProfileService.EXTRA_LOG_URI, mLogSession.getSessionUri());
        startService(service);
        Logger.d(mLogSession, "Binding to the service...");
        bindService(service, mServiceConnection, 0);
    }

    @Override
    public void onDialogCanceled() {
        // do nothing
    }
    //-------------------ScannerFragment.OnDeviceSelectedListener End------------------

    //-------------------LoaderManager.LoaderCallbacks<Cursor>-------------------------
    @Override
    public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
        switch (id) {
            case LOG_REQUEST_ID: {
                return new CursorLoader(this, mLogSession.getSessionEntriesUri(), LOG_PROJECTION, null, null, LogContract.Log.TIME);
            }
        }
        return null;
    }

    @Override
    public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
        // Here we have to restore the old saved scroll position, or scroll to the bottom if before adding new events it was scrolled to the bottom.
        if(mLogsListView == null){
            return;
        }

        final int position = mLogScrollPosition;
        final boolean scrolledToBottom = position == LOG_SCROLLED_TO_BOTTOM || (mLogsListView.getCount() > 0 && mLogsListView.getLastVisiblePosition() == mLogsListView.getCount() - 1);

        mLogAdapter.swapCursor(data);

        if (position > LOG_SCROLL_NULL) {
            mLogsListView.setSelectionFromTop(position, 0);
        } else {
            if (scrolledToBottom)
                mLogsListView.setSelection(mLogsListView.getCount() - 1);
        }
        mLogScrollPosition = LOG_SCROLL_NULL;
    }

    @Override
    public void onLoaderReset(final Loader<Cursor> loader) {
        mLogAdapter.swapCursor(null);
    }
    //-------------------LoaderManager.LoaderCallbacks<Cursor> END-------------------------

    //-------------下面是BleManagerCallbacks回调 START----------------------
    @Override
    public void onDeviceConnecting(final BluetoothDevice device) {
        // empty default implementation
    }

    @Override
    public void onDeviceConnected(final BluetoothDevice device) {
        mDeviceNameView.setText(mDeviceName);
        //mConnectButton.setText(R.string.action_disconnect);

        mField.setEnabled(true);
        mSendButton.setEnabled(true);
    }

    @Override
    public void onDeviceDisconnecting(final BluetoothDevice device) {
        // empty default implementation

        mField.setEnabled(false);
        mSendButton.setEnabled(false);
    }

    @Override
    public void onDeviceDisconnected(final BluetoothDevice device) {
        //mConnectButton.setText(R.string.action_connect);
        mDeviceNameView.setText(getDefaultDeviceName());
        //if (mBatteryLevelView != null)
        //    mBatteryLevelView.setText(R.string.not_available);

        try {
            Logger.d(mLogSession, "Unbinding from the service...");
            unbindService(mServiceConnection);
            mServiceBinder = null;

            Logger.d(mLogSession, "Activity unbound from the service");
            onServiceUnbinded();
            mDeviceName = null;
            mBluetoothDevice = null;
            mLogSession = null;
        } catch (final IllegalArgumentException e) {
            // do nothing. This should never happen but does...
        }
    }

    @Override
    public void onDeviceNotSupported(final BluetoothDevice device) {
        showToast(R.string.not_supported);
    }

    @Override
    public void onLinklossOccur(final BluetoothDevice device) {
        //if (mBatteryLevelView != null)
        //    mBatteryLevelView.setText(R.string.not_available);
    }

    @Override
    public void onServicesDiscovered(final BluetoothDevice device, final boolean optionalServicesFound) {
        // empty default implementation
    }

    @Override
    public void onDeviceReady(final BluetoothDevice device) {
        // empty default implementation
    }

    @Override
    public void onBondingRequired(final BluetoothDevice device) {
        // empty default implementation
    }

    @Override
    public void onBonded(final BluetoothDevice device) {
        // empty default implementation
    }

    @Override
    public final boolean shouldEnableBatteryLevelNotifications(final BluetoothDevice device) {
        // This method will never be called.
        // Please see BleProfileService#shouldEnableBatteryLevelNotifications(BluetoothDevice) instead.
        throw new UnsupportedOperationException("This method should not be called");
    }

    @Override
    public void onBatteryValueReceived(final BluetoothDevice device, final int value) {
        //if (mBatteryLevelView != null)
        //    mBatteryLevelView.setText(getString(R.string.battery, value));
    }

    @Override
    public void onError(final BluetoothDevice device, final String message, final int errorCode) {
        DebugLogger.e(TAG, "Error occurred: " + message + ",  error code: " + errorCode);
        showToast(message + " (" + errorCode + ")");
    }
    //-------------下面是BleManagerCallbacks回调 END----------------------

}
