package ru.er_log.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;

import static ru.er_log.bluetooth.MainActivity.NAME;
import static ru.er_log.bluetooth.MainActivity.SERVICE_UUID;
import static ru.er_log.bluetooth.MainActivity.TAG;

public final class ServerConnector extends Thread implements IClientConnectorCallback, IServerConnectorCallback
{
    private BluetoothSocket mmClientSocket; // Socket describes concrete connection with the client.
    private final BluetoothServerSocket mmServerSocket; // Socket for establish connection.
    private final IClientConnectorCallback callbackClient;
    private final IServerConnectorCallback callbackServer;
    private final Handler callbackHandler;

    public ServerConnector(BluetoothAdapter bluetoothAdapter)
    {
        this(bluetoothAdapter, null, null);
    }

    public ServerConnector(BluetoothAdapter bluetoothAdapter, IClientConnectorCallback callbackClient)
    {
        this(bluetoothAdapter, callbackClient, null);
    }

    public ServerConnector(BluetoothAdapter bluetoothAdapter, IServerConnectorCallback callbackServer)
    {
        this(bluetoothAdapter, null, callbackServer);
    }

    public ServerConnector(BluetoothAdapter bluetoothAdapter, IClientConnectorCallback callbackClient, IServerConnectorCallback callbackServer)
    {
        this.callbackHandler = new Handler(Looper.getMainLooper());

        this.callbackClient = callbackClient;
        this.callbackServer = callbackServer;

        BluetoothServerSocket tmp = null;
        try
        {
            // SERVICE_UUID is the app's UUID string, also used by the client code.
            tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, SERVICE_UUID);
            onServerListen();
        } catch (IOException e)
        {
            Log.e(TAG, "Socket's listen() method failed", e);
            onServerError(IServerConnectorCallback.CODE_ERROR_LISTEN);
        }
        mmServerSocket = tmp;
    }

    public void run()
    {
        // Keep listening until exception occurs or a socket is returned.
        while (true)
        {
            try
            {
                mmClientSocket = mmServerSocket.accept();
                onConnect(mmClientSocket);
            } catch (IOException e)
            {
                Log.e(TAG, "Socket's accept() method failed", e);
                onError(IClientConnectorCallback.CODE_ERROR_CONNECT);
                break;
            }

            // If connection with the client is established, then close the ServerSocket
            // so as not receive new connections with clients.
            if (mmClientSocket != null)
            {
//                // A connection was accepted. Perform work associated with
//                // the connection in a separate thread.
//                useConnectedSocket(socket);
                cancelServer();
                break;
            }
        }
    }

    // Closes the connect socket and causes the thread to finish.
    public void cancelServer()
    {
        try
        {
            mmServerSocket.close();
            onServerDisconnect();
        } catch (IOException e)
        {
            Log.e(TAG, "Could not close the connect socket", e);
            onServerError(IServerConnectorCallback.CODE_ERROR_CLOSE_SOCKET);
        }
    }

    public void cancelClient()
    {
        if (mmClientSocket == null) return;

        try
        {
            mmClientSocket.close();
            onDisconnect();
        } catch (IOException e)
        {
            Log.e(TAG, "Could not close the connect socket", e);
            onError(IClientConnectorCallback.CODE_ERROR_CLOSE_SOCKET);
        }
    }

    @Override
    public void onConnect(final BluetoothSocket socket)
    {
        if (callbackClient == null) return;

        // We use .post() because we are in working thread and want to grant opportunities to modify UI.
        callbackHandler.post(new Runnable() { @Override public void run()
        {
            callbackClient.onConnect(socket);
        }});
    }

    @Override
    public void onDisconnect()
    {
        if (callbackClient == null) return;

        callbackHandler.post(new Runnable() { @Override public void run()
        {
            callbackClient.onDisconnect();
        }});
    }

    @Override
    public void onError(final int code)
    {
        if (callbackClient == null) return;

        callbackHandler.post(new Runnable() { @Override public void run()
        {
            callbackClient.onError(code);
        }});
    }

    @Override
    public void onServerListen()
    {
        if (callbackServer == null) return;

        callbackHandler.post(new Runnable() { @Override public void run()
        {
            callbackServer.onServerListen();
        }});
    }

    @Override
    public void onServerDisconnect()
    {
        if (callbackServer == null) return;

        callbackHandler.post(new Runnable() { @Override public void run()
        {
            callbackServer.onServerDisconnect();
        }});
    }

    @Override
    public void onServerError(final int code)
    {
        if (callbackServer == null) return;

        callbackHandler.post(new Runnable() { @Override public void run()
        {
            callbackServer.onServerError(code);
        }});
    }
}
