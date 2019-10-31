package ru.er_log.bluetooth.component;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.Nullable;
import android.util.Log;

public class eBluetoothDevice
{
    private BluetoothDevice bluetoothDevice;

    public eBluetoothDevice(BluetoothDevice bluetoothDevice)
    {
        if (bluetoothDevice == null)
            throw new NullPointerException("bluetoothDevice was null");

        this.bluetoothDevice = bluetoothDevice;
    }

    public BluetoothDevice getDevice()
    {
        return bluetoothDevice;
    }

    @Override
    public String toString()
    {
        if (bluetoothDevice == null)
            throw new NullPointerException("bluetoothDevice was null");

        StringBuilder stringBuilder = new StringBuilder();

        String devName = bluetoothDevice.getName();
        if (devName != null)
            stringBuilder.append(devName).append(" :: ");

        return stringBuilder.append(bluetoothDevice.getAddress()).toString();
    }

    @Override
    public int hashCode()
    {
        if (bluetoothDevice == null)
            throw new NullPointerException("bluetoothDevice was null");

        return bluetoothDevice.getAddress().intern().hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj)
    {
        if (obj == null) return false;
        return this.hashCode() == obj.hashCode();
    }
}
