package com.sinric.esp32_android_app;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;


public class BluetoothScan {
    public static final int SCAN_FEATURE_ERROR = 0x00;
    public static final int SCAN_ADAPTER_ERROR = 0x01;
    public static final int SCAN_NEED_ENADLE = 0x02;
    public static final int SCAN_BEGIN_SCAN = 0x03;
    public static final int AUTO_ENABLE_FAILURE = 0x04;

    private static BluetoothAdapter mBluetoothAdapter;
    private static BluetoothScanCallBack mBluetoothScanCallBack;

    public static void startScan(boolean autoEnable, BluetoothScanCallBack callBack) {
        mBluetoothScanCallBack = callBack;
        if (!isBluetoothSupport(autoEnable)) {
            return;
        }
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            Log.e("BluetoothScan", "mBluetoothAdapter is null.");
        }
    }

    public static void stopScan() {
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        } else {
            Log.e("BluetoothScan","mBluetoothAdapter is null.");
        }
    }

    private static boolean isBluetoothSupport(Boolean autoEnable) {
        if (!MyApplication.context().getPackageManager().
                hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            mBluetoothScanCallBack.onLeScanInitFailure(SCAN_FEATURE_ERROR);
            return false;
        }

        final BluetoothManager bluetoothManager = (BluetoothManager) MyApplication.context().
                getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter != null) {
            if (!mBluetoothAdapter.isEnabled()) {
                if (autoEnable) {
                    if (mBluetoothAdapter.enable()) {
                        mBluetoothScanCallBack.onLeScanInitSuccess(SCAN_BEGIN_SCAN);
                        return true;
                    } else {
                        mBluetoothScanCallBack.onLeScanInitSuccess(AUTO_ENABLE_FAILURE);
                        return false;
                    }
                } else {
                    mBluetoothScanCallBack.onLeScanInitSuccess(SCAN_NEED_ENADLE);
                    return false;
                }
            } else {
                mBluetoothScanCallBack.onLeScanInitSuccess(SCAN_BEGIN_SCAN);
                return true;
            }
        } else {
            mBluetoothScanCallBack.onLeScanInitFailure(SCAN_ADAPTER_ERROR);
            return false;
        }
    }

    // Device scan callback.
    private static BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            if (mBluetoothScanCallBack != null) {
                mBluetoothScanCallBack.onLeScanResult(device, rssi, scanRecord);
            } else {
                Log.e("BluetoothScan","mBluetoothScanCallBack is null.");
            }
        }
    };

    public interface BluetoothScanCallBack {
        void onLeScanInitFailure(int failureCode);
        void onLeScanInitSuccess(int successCode);
        void onLeScanResult(BluetoothDevice device, int rssi, byte[] scanRecord);
    }
}

