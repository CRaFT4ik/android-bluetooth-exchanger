package ru.er_log.bluetooth;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;

import static ru.er_log.bluetooth.MainActivity.SERVICE_UUID;
import static ru.er_log.bluetooth.MainActivity.TAG;

public final class ClientConnector extends Thread implements IClientConnectorCallback
{
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private final BluetoothAdapter bluetoothAdapter;
    private final IClientConnectorCallback callback;
    private final Handler callbackHandler;

    public ClientConnector(BluetoothAdapter bluetoothAdapter, BluetoothDevice device)
    {
        this(bluetoothAdapter, device, null);
    }

    public ClientConnector(BluetoothAdapter bluetoothAdapter, BluetoothDevice device, IClientConnectorCallback callback)
    {
        this.callbackHandler = new Handler(Looper.getMainLooper());

        this.bluetoothAdapter = bluetoothAdapter;
        this.callback = callback;

        // Use a temporary object that is later assigned to mmSocket
        // because mmSocket is final.
        BluetoothSocket tmp = null;
        mmDevice = device;

        try
        {
            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            // SERVICE_UUID is the app's UUID string, also used in the server code.
            tmp = device.createRfcommSocketToServiceRecord(SERVICE_UUID);
        } catch (IOException e)
        {
            Log.e(TAG, "Socket's create() method failed", e);
        }
        mmSocket = tmp;
    }

    public void run()
    {
        // Cancel discovery because it otherwise slows down the connection.
        bluetoothAdapter.cancelDiscovery();

        try
        {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            mmSocket.connect();
        } catch (IOException connectException)
        {
            // Unable to connect; close the socket and return.
            try
            {
                mmSocket.close();
            } catch (IOException closeException)
            {
                Log.e(TAG, "Could not close the client socket", closeException);
            }

            onError(IClientConnectorCallback.CODE_ERROR_CONNECT);
            return;
        }

        onConnect(mmSocket);

//        // The connection attempt succeeded. Perform work associated with
//        // the connection in a separate thread.
//        useConnectedSocket(mmSocket);
    }

    // Closes the client socket and causes the thread to finish.
    public void cancel()
    {
        try
        {
            mmSocket.close();
            onDisconnect();
        } catch (IOException e)
        {
            Log.e(TAG, "Could not close the client socket", e);
            onError(IClientConnectorCallback.CODE_ERROR_CLOSE_SOCKET);
        }
    }

    @Override
    public void onConnect(final BluetoothSocket socket)
    {
        if (callback == null) return;

        // We use .post() because we are in working thread and want to grant opportunities to modify UI.
        callbackHandler.post(new Runnable() { @Override public void run()
        {
            callback.onConnect(socket);
        }});
    }

    @Override
    public void onDisconnect()
    {
        if (callback == null) return;

        callbackHandler.post(new Runnable() { @Override public void run()
        {
            callback.onDisconnect();
        }});
    }

    @Override
    public void onError(final int code)
    {
        if (callback == null) return;

        callbackHandler.post(new Runnable() { @Override public void run()
        {
            callback.onError(code);
        }});
    }
}