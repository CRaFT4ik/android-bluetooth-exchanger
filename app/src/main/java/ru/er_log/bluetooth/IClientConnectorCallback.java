package ru.er_log.bluetooth;

import android.bluetooth.BluetoothSocket;

public interface IClientConnectorCallback
{
    public static final int CODE_ERROR_CONNECT = 0;
    public static final int CODE_ERROR_CLOSE_SOCKET = 1;

    void onConnect(BluetoothSocket socket);     // When connect succeed.
    void onDisconnect();                        // When disconnect succeed.
    void onError(final int code);               // When error occurs.
}
