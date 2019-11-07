package ru.er_log.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import ru.er_log.bluetooth.util.FileUtil;
import ru.er_log.bluetooth.util.ScreenshotUtil;
import ru.er_log.bluetooth.util.Util;
import ru.er_log.bluetooth.component.eProtocolTypeLayer;

import static ru.er_log.bluetooth.IServerConnectorCallback.CODE_ERROR_LISTEN;
import static ru.er_log.bluetooth.MainActivity.DISCOVERY_BL_REQUEST_CODE;
import static ru.er_log.bluetooth.MainActivity.DISCOVER_DURATION;
import static ru.er_log.bluetooth.MainActivity.REJECT_WORD;
import static ru.er_log.bluetooth.MainActivity.TAG;

public class ServerActivity extends AppCompatActivity
{
    private final class Client
    {
        private final BluetoothSocket socketConnected; // Current active socket.
        private final eProtocolTypeLayer protocolTypeLayer;
        private final BluetoothCommunicationService bcs;
        private final FileUtil.FileCollector fileCollector;

        Client(BluetoothSocket socketConnected, Handler handler)
        {
            if (socketConnected == null) throw new NullPointerException("Socket was null");

            this.socketConnected = socketConnected;

            protocolTypeLayer = new eProtocolTypeLayer();
            bcs = new BluetoothCommunicationService(socketConnected, handler);
            fileCollector = new FileUtil.FileCollector();
        }

        void close()
        {
            try { socketConnected.close(); }
            catch (IOException ignored) {}
        }
    }

    private Button launchButton;

    private BluetoothAdapter bluetoothAdapter;
    private Client client;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        initComponents();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if (client != null)
        {
            client.close();
            client = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        if (requestCode == DISCOVERY_BL_REQUEST_CODE)
        {
            if (resultCode == RESULT_CANCELED)
            {
                Toast.makeText(this, "Unexpected behavior, code " + resultCode, Toast.LENGTH_SHORT).show();
                return;
            }

            launchServer();
        }
    }

    private void initComponents()
    {
        launchButton = (Button) findViewById(R.id.buttonLaunchServer);
    }

    private void enableBluetooth(int requestCode)
    {
        Util.enableBluetooth(this, bluetoothAdapter, requestCode);
    }

    private void enableDiscoverability(int requestCode)
    {
        Util.enableDiscoverability(this, bluetoothAdapter, DISCOVER_DURATION, requestCode);
    }

    private void launchServer()
    {
        freezeUI(true);
        ServerConnector server = new ServerConnector(bluetoothAdapter, connectorCallbackClient, connectorCallbackServer);
        server.start();
    }

    public void clickLaunchServer(View view)
    {
        enableDiscoverability(DISCOVERY_BL_REQUEST_CODE);
    }

    private void freezeUI(boolean freeze)
    {
        boolean enable = !freeze;
        launchButton.setEnabled(enable);
    }

    // Callback for ServerConnector thread. Describes client connections.
    private IClientConnectorCallback connectorCallbackClient = new IClientConnectorCallback()
    {
        @Override
        public void onConnect(BluetoothSocket socket)
        {
            synchronized (ServerActivity.class)
            {
                client = new Client(socket, serverHandler);

                String name = (null != socket.getRemoteDevice().getName()) ? socket.getRemoteDevice().getName() : socket.getRemoteDevice().getAddress();
                Toast.makeText(ServerActivity.super.getApplicationContext(), "Client connected: " + name + "\n" + "Waiting for requests...", Toast.LENGTH_LONG).show();

                client.bcs.read();
            }
        }

        @Override
        public void onDisconnect()
        {
            synchronized (ServerActivity.class)
            {
                client = null;

                Toast.makeText(ServerActivity.super.getApplicationContext(), "Disconnected from the device", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onError(int code)
        {
            synchronized (ServerActivity.class)
            {
                client = null;

                if (code == CODE_ERROR_LISTEN)
                    Toast.makeText(ServerActivity.super.getApplicationContext(), "Listening process failed", Toast.LENGTH_SHORT).show();
                else if (code == CODE_ERROR_CLOSE_SOCKET)
                    Toast.makeText(ServerActivity.super.getApplicationContext(), "Error while closing server socket", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(ServerActivity.super.getApplicationContext(), "Error occurred: " + code, Toast.LENGTH_SHORT).show();
            }
        }
    };

    // Callback for ServerConnector thread. Describes server listening process.
    private IServerConnectorCallback connectorCallbackServer = new IServerConnectorCallback()
    {
        @Override
        public void onServerListen()
        {
            Toast.makeText(ServerActivity.super.getApplicationContext(), "Starting listening...", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServerDisconnect()
        {
            Toast.makeText(ServerActivity.super.getApplicationContext(), "Server closed for accept new connections", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServerError(int code)
        {
            Toast.makeText(ServerActivity.super.getApplicationContext(), "Error occurred: " + code, Toast.LENGTH_SHORT).show();
        }
    };

    // Handle messages from BluetoothCommunicationService.
    private Handler serverHandler = new Handler(new Handler.Callback()
    {
        @Override
        public boolean handleMessage(@NonNull Message msg)
        {
            if (msg.what == BluetoothCommunicationService.MessageConstants.MESSAGE_TOAST)
            {
                String msgStr = msg.getData().getString("toast");
                if (msgStr == null) return false;

                int length = msgStr.length() < 30 ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG;
                Toast.makeText(ServerActivity.super.getApplicationContext(), msgStr, length).show();
                return true;
            }

            if (msg.what == BluetoothCommunicationService.MessageConstants.MESSAGE_READ)
            {
                byte[] bytes = (byte[]) msg.obj;

                if (!client.protocolTypeLayer.getReceiver().set(bytes))
                    Log.w(TAG, "Got invalid message, parsing can't continue");
                else
                    messageParser(client.protocolTypeLayer.getReceiver().type(), client.protocolTypeLayer.getReceiver().payload());

                return true;
            }

            if (msg.what == BluetoothCommunicationService.MessageConstants.MESSAGE_WRITE)
            {
                Toast.makeText(ServerActivity.super.getApplicationContext(), "Message sent (" + msg.arg1 + " bytes)", Toast.LENGTH_SHORT).show();
                return true;
            }

            if (msg.what == BluetoothCommunicationService.MessageConstants.MESSAGE_DISCONNECTED)
            {
                Toast.makeText(ServerActivity.super.getApplicationContext(), "Client disconnected", Toast.LENGTH_SHORT).show();
                finish();
            }

            return false;
        }
    });

    private void messageParser(eProtocolTypeLayer.Types type, byte[] payload)
    {
        if (type == eProtocolTypeLayer.Types.TEXT)
        {
            String text;
            if (payload == null)
            {
                text = "Internal error: got 'null' object as message";
                Log.e(TAG, "Type TEXT: " + text);
            } else
            {
                text = "Received: " + new String(payload);
                Log.i(TAG, "Type TEXT: " + text);
            }

            int length = text.length() < 30 ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG;
            Toast.makeText(ServerActivity.super.getApplicationContext(), text, length).show();

//          This is working example for sending answer to the received text message.
//            String toSendText = "This is your ASK, bro!\nПеревод: все ОК, сообщения работают :)";
//            byte[] toSendData = client.protocolTypeLayer.getTransmitter().form(eProtocolTypeLayer.Types.TEXT, toSendText.getBytes());
//            client.bcs.write(toSendData);
        }
        else if (type == eProtocolTypeLayer.Types.FILE_REQUEST)
        {
            if (payload == null) return;

            if (!Util.isExternalStorageWritable())
            {
                Log.e(TAG, "Type FILE_REQUEST: External storage is not mounted for saving files! Request rejected.");
                byte[] messageBytes = client.protocolTypeLayer.getTransmitter().form(eProtocolTypeLayer.Types.FILE_RESPONSE, REJECT_WORD.getBytes());
                client.bcs.write(messageBytes);
                return;
            }

            String fileName = new String(payload);
            if (!FileUtil.isFilenameValid(fileName))
            {
                Log.i(TAG, "Type FILE_REQUEST: Request rejected, received filename is invalid");
                byte[] messageBytes = client.protocolTypeLayer.getTransmitter().form(eProtocolTypeLayer.Types.FILE_RESPONSE, REJECT_WORD.getBytes());
                client.bcs.write(messageBytes);
                return;
            }

            Log.i(TAG, "Type FILE_REQUEST: Got request for receive file with name '" + fileName + "'");

            client.fileCollector.set(fileName, null, null);
            byte[] messageBytes = client.protocolTypeLayer.getTransmitter().form(eProtocolTypeLayer.Types.FILE_RESPONSE, fileName.getBytes());
            client.bcs.write(messageBytes);
        }
        else if (type == eProtocolTypeLayer.Types.FILE_CONTENT)
        {
            if (!Util.isExternalStorageWritable())
                Log.e(TAG, "Type FILE_CONTENT: External storage is not mounted for saving files!");

            String fileName = client.fileCollector.getName();
            if (fileName == null || payload == null)
            {
                Log.e(TAG, "Type FILE_CONTENT: fileName == null ("+(fileName == null)+ ") OR data == null ("+(payload == null) + ")");
                return;
            }

            File documents = Util.getPrivateDocumentsDir(this);
            Toast.makeText(ServerActivity.super.getApplicationContext(), "Saving file from the remote device...", Toast.LENGTH_SHORT).show();
            FileUtil.saveFile(documents, fileName, payload);

            Uri selectedUri = Uri.fromFile(documents);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(selectedUri, "resource/folder");
            if (intent.resolveActivityInfo(getPackageManager(), 0) != null)
                startActivity(intent);
            else // If there is no any file explorer app installed on this device.
                Toast.makeText(this, "Please, open file manually. We didn't found any file explorer :(\nPath: " + documents, Toast.LENGTH_LONG).show();
        }
        else if (type == eProtocolTypeLayer.Types.SCREENSHOT_REQUEST)
        {
            Toast.makeText(ServerActivity.super.getApplicationContext(), "Sending screenshot to the remote device", Toast.LENGTH_LONG).show();

            byte[] screenshotBytes = ScreenshotUtil.takeScreenshot(this).toByteArray();
            byte[] messageBytes = client.protocolTypeLayer.getTransmitter().form(eProtocolTypeLayer.Types.SCREENSHOT_RESPONSE, screenshotBytes);
            client.bcs.write(messageBytes);
        } else
        {
            Log.e(TAG, "Got message of unknown type (=" + type + ")");
        }
    }
}
