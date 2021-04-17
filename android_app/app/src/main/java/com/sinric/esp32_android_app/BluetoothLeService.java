package com.sinric.esp32_android_app;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;


import java.util.List;
import java.util.UUID;


public class BluetoothLeService extends Service {
    public final static String ACTION_GATT_CONNECTING = "action_gatt_connecting";
    public final static String ACTION_GATT_CONNECTED = "action_gatt_connected";
    public final static String ACTION_GATT_DISCONNECTED = "action_gatt_disconnected";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "action_gatt_services_discovered";
    public final static String ACTION_DATA_AVAILABLE = "action_data_available";
    public final static String EXTRA_DATA = "extra_data";

    public final static UUID UUID_NOTIFY =
            UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8");

    public BluetoothGattCharacteristic mNotifyCharacteristic;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;

    private final IBinder mBinder = new LocalBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return  mBinder;
    }

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    public boolean initialize() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e("BluetoothLeService", "Unable to initialize BluetoothManager.");
                return false;
            }
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
             Log.e("BluetoothLeService","Unable to obtain a BluetoothAdapter.");
            return false;
        }

        Log.i("BluetoothLeService", "Initialize BluetoothLeService success!");
        return true;
    }

    public boolean connect(String address) {
        if (mBluetoothAdapter == null || address == null) {
             Log.e("BluetoothLeService","BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
             Log.w("BluetoothLeService","Trying to use an existing mBluetoothGatt for connection.");
            return mBluetoothGatt.connect();
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
             Log.e("BluetoothLeService","Device not found,Unable to connect.");
            return false;
        }
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
         Log.w("BluetoothLeService","Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        return true;
    }

    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
             Log.e("BluetoothLeService","BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    public void close() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;;
        }
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                broadcastUpdate(ACTION_GATT_CONNECTED);
                 Log.i("BluetoothLeService","Connected to GATT server.");
                mBluetoothGatt.discoverServices();
                 Log.i("BluetoothLeService","Attempting to start service discovery:");
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                broadcastUpdate(ACTION_GATT_DISCONNECTED);
                 Log.w("BluetoothLeService","Disconnected from GATT server.");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                 Log.e("BluetoothLeService","onServicesDiscovered received : " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                 Log.i("BluetoothLeService","onCharacteristicRead()");
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
             Log.i("BluetoothLeService","onCharacteristicChanged()");
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
             Log.i("BluetoothLeService","onCharacteristicWrite()");
            super.onCharacteristicWrite(gatt, characteristic, status);
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        final byte[] data = characteristic.getValue();
        intent.putExtra(EXTRA_DATA, data);
        sendBroadcast(intent);
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) {
            return null;
        }

        List<BluetoothGattService> gattServices = mBluetoothGatt.getServices();

        for (BluetoothGattService gattService : gattServices) {
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();

            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                String uuid = gattCharacteristic.getUuid().toString();
                Log.i("BluetoothLeService","uuid : " + uuid);

                if(uuid.equalsIgnoreCase(UUID_NOTIFY.toString())){
                    mNotifyCharacteristic = gattCharacteristic;
                    mBluetoothGatt.setCharacteristicNotification(gattCharacteristic, true);
                    Log.i("BluetoothLeService","setCharacteristicNotification : " + uuid);
                    UUID magic_uuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
                    BluetoothGattDescriptor descriptor = gattCharacteristic.getDescriptor(magic_uuid);
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    mBluetoothGatt.writeDescriptor(descriptor);
                }
            }
        }
        return gattServices;
    }

    public void writeCharacteristic(byte[] data) {
        mNotifyCharacteristic.setValue(data);
        mBluetoothGatt.writeCharacteristic(mNotifyCharacteristic);
    }
}
