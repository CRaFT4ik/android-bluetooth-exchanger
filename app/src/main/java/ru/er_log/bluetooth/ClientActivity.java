package ru.er_log.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import ru.er_log.bluetooth.component.FileUtil;
import ru.er_log.bluetooth.component.ScreenshotUtil;
import ru.er_log.bluetooth.component.Utils;
import ru.er_log.bluetooth.component.eBluetoothDevice;
import ru.er_log.bluetooth.component.eProtocolTypeLayer;

import static ru.er_log.bluetooth.MainActivity.CHOOSE_FILE_REQUEST_CODE;
import static ru.er_log.bluetooth.MainActivity.DISCOVER_DURATION;
import static ru.er_log.bluetooth.MainActivity.ENABLE_BL_REQUEST_CODE;
import static ru.er_log.bluetooth.MainActivity.REJECT_WORD;
import static ru.er_log.bluetooth.MainActivity.TAG;

public class ClientActivity extends AppCompatActivity
{
    private Button buttonConnect;
    private Button buttonRefresh;
    private Button buttonSendFile;
    private Button buttonReceiveScreenshot;
    private ListView listViewDevices;

    private BluetoothSocket socketConnected; // Current active socket.
    private BluetoothCommunicationService bcs;
    private eProtocolTypeLayer protocolTypeLayer;
    private BluetoothAdapter bluetoothAdapter;
    private eBluetoothDevice selectedDevice;
    private FileUtil.FileCollector fileCollector;

    private ArrayAdapter<eBluetoothDevice> listViewDevicesAdapter;
    private final LinkedHashSet<eBluetoothDevice> devices = new LinkedHashSet<>();
    private final List<eBluetoothDevice> devicesList = new ArrayList<>(devices);

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver()
    {
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action))
            {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                try
                {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    addDeviceToList(new eBluetoothDevice(device));
                    listViewDevicesAdapter.notifyDataSetChanged();
                } catch (NullPointerException ignored)
                {
                    Log.e(TAG, "got null BluetoothDevice value");
                    Toast.makeText(context, "unexpected: got null BluetoothDevice value", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        protocolTypeLayer = new eProtocolTypeLayer();
        fileCollector = new FileUtil.FileCollector();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);

        init();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if (socketConnected != null)
            try { socketConnected.close(); socketConnected = null; }
            catch (IOException ignored) { }

        unregisterReceiver(receiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == ENABLE_BL_REQUEST_CODE)
        {
            if (resultCode != RESULT_OK)
            {
                Toast.makeText(this, "Unexpected behavior, code " + resultCode, Toast.LENGTH_SHORT).show();
                return;
            }

            addPairedDevices();
            Toast.makeText(this, "Searching...", Toast.LENGTH_SHORT).show();
            bluetoothAdapter.startDiscovery();
        }

        if (requestCode == CHOOSE_FILE_REQUEST_CODE)
        {
            if (resultCode != RESULT_OK) return;

            Uri uri = data.getData();
            assert uri != null;

            File file = null;
            try { file = FileUtil.uriToFile(this, uri); }
            catch (IOException e)
            {
                Toast.makeText(this, "Sorry, failed :(", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                return;
            }

            byte[] fileBytes = FileUtil.readFile(file);
            String fileName = FileUtil.getFileName(this, uri);
            fileCollector.set(fileName, file, fileBytes);

            Log.d(TAG, "Chosen file for sending: " + fileName);
            byte[] messageBytes = protocolTypeLayer.getTransmitter().form(eProtocolTypeLayer.Types.FILE_REQUEST, fileName.getBytes());
            bcs.write(messageBytes);
        }
    }

    private void init()
    {
        initComponents();
        enableBluetooth(ENABLE_BL_REQUEST_CODE);
    }

    private void initComponents()
    {
        final ClientActivity context = this;

        listViewDevices = (ListView) findViewById(R.id.listViewDevices);
        listViewDevicesAdapter = new ArrayAdapter<eBluetoothDevice>(context, android.R.layout.simple_list_item_1, devicesList);
        listViewDevices.setAdapter(listViewDevicesAdapter);
        listViewDevices.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                eBluetoothDevice bluetoothDevice = listViewDevicesAdapter.getItem(position);
                selectedDevice = bluetoothDevice;
                assert bluetoothDevice != null;
                String devName = bluetoothDevice.getDevice().getName();
                String outName = (devName != null) ? devName : bluetoothDevice.getDevice().getAddress();
                Toast.makeText(context, "Selected " + outName, Toast.LENGTH_SHORT).show();
            }
        });

        buttonConnect = (Button) findViewById(R.id.connect);
        buttonRefresh = (Button) findViewById(R.id.refresh);
        buttonSendFile = (Button) findViewById(R.id.send_file);
        buttonReceiveScreenshot = (Button) findViewById(R.id.receive_screenshot);
    }

    private void enableBluetooth(int requestCode)
    {
        Utils.enableBluetooth(this, bluetoothAdapter, requestCode);
    }

    private void enableDiscoverability(int requestCode)
    {
        Utils.enableDiscoverability(this, bluetoothAdapter, DISCOVER_DURATION, requestCode);
    }

    // Fill the list by paired devices.
    protected void addPairedDevices()
    {
        devices.clear();

        Set<BluetoothDevice> bluetoothDeviceSet = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice bluetoothDevice : bluetoothDeviceSet)
            addDeviceToList(new eBluetoothDevice(bluetoothDevice));

        if (!bluetoothDeviceSet.isEmpty())
            listViewDevicesAdapter.notifyDataSetChanged();
    }

    private void freezeUIConnectionControls(boolean freeze)
    {
        boolean enable = !freeze;
        listViewDevices.setEnabled(enable);
        buttonConnect.setEnabled(enable);
        buttonRefresh.setEnabled(enable);
    }

    private void freezeUISharingControls(boolean freeze)
    {
        boolean enable = !freeze;
        buttonSendFile.setEnabled(enable);
        buttonReceiveScreenshot.setEnabled(enable);
    }

    private void addDeviceToList(eBluetoothDevice device)
    {
        devices.add(device);
        devicesList.clear();
        devicesList.addAll(devices);
    }

    public void clickRefreshDevices(View view)
    {
        if (bluetoothAdapter == null || bluetoothAdapter.isDiscovering())
            return;

        enableBluetooth(ENABLE_BL_REQUEST_CODE);
    }

    public void clickMakeConnection(View view)
    {
        if (selectedDevice == null)
        {
            Toast.makeText(this, "Please, select a device first", Toast.LENGTH_SHORT).show();
            return;
        }

        freezeUIConnectionControls(true);
        Toast.makeText(this, "Connecting to " + selectedDevice, Toast.LENGTH_SHORT).show();

        ClientConnector client = new ClientConnector(bluetoothAdapter, selectedDevice.getDevice(), connectorCallback);
        client.start();
    }

    public void clickReceiveScreenshot(View view)
    {
        synchronized (ClientActivity.class)
        {
            if (socketConnected == null)
            {
                Toast.makeText(this, "You're not connected to any device", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        byte[] payload = protocolTypeLayer.getTransmitter().form(eProtocolTypeLayer.Types.SCREENSHOT_REQUEST, null);
        bcs.write(payload);
    }

    public void clickSendFile(View view)
    {
        synchronized (ClientActivity.class)
        {
            if (socketConnected == null)
            {
                Toast.makeText(this, "You're not connected to any device", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Intent intent = new Intent();
        intent.setType("*/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select a file"), CHOOSE_FILE_REQUEST_CODE);

//      This is working example for sending text message.
//        {
//            String mes = "Hello from client! :)";
//            Toast.makeText(this, "Sending \"" + mes + "\" message...", Toast.LENGTH_SHORT).show();
//            BluetoothCommunicationService bcs = new BluetoothCommunicationService(socketConnected, clientHandler);
//
//            byte[] payload = protocolTypeLayer.getTransmitter().form(eProtocolTypeLayer.Types.TEXT, mes.getBytes());
//            bcs.write(payload);
//        }
    }

    // Callback for ClientConnector thread.
    private IClientConnectorCallback connectorCallback = new IClientConnectorCallback()
    {
        @Override
        public void onConnect(BluetoothSocket socket)
        {
            synchronized (ClientActivity.class)
            {
                socketConnected = socket;
                String name = (null != socket.getRemoteDevice().getName()) ? socket.getRemoteDevice().getName() : socket.getRemoteDevice().getAddress();
                Toast.makeText(ClientActivity.super.getApplicationContext(), "Connected to the device: " + name, Toast.LENGTH_SHORT).show();

                bcs = new BluetoothCommunicationService(socketConnected, clientHandler);
                bcs.read();
            }
        }

        @Override
        public void onDisconnect()
        {
            synchronized (ClientActivity.class)
            {
                socketConnected = null;
                Toast.makeText(ClientActivity.super.getApplicationContext(), "Disconnected from the device", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onError(int code)
        {
            synchronized (ClientActivity.class)
            {
                socketConnected = null;

                if (code == CODE_ERROR_CONNECT)
                    Toast.makeText(ClientActivity.super.getApplicationContext(), "Connection failed", Toast.LENGTH_SHORT).show();
                else if (code == CODE_ERROR_CLOSE_SOCKET)
                    Toast.makeText(ClientActivity.super.getApplicationContext(), "Error while closing connection", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(ClientActivity.super.getApplicationContext(), "Error occurred: " + code, Toast.LENGTH_SHORT).show();

                freezeUIConnectionControls(false);
                freezeUISharingControls(false);
            }
        }
    };

    // Handle messages from BluetoothCommunicationService.
    private Handler clientHandler = new Handler(new Handler.Callback()
    {
        @Override
        public boolean handleMessage(@NonNull Message msg)
        {
            if (msg.what == BluetoothCommunicationService.MessageConstants.MESSAGE_TOAST)
            {
                String msgStr = msg.getData().getString("toast");
                if (msgStr == null) return false;

                int length = msgStr.length() < 30 ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG;
                Toast.makeText(ClientActivity.super.getApplicationContext(), msgStr, length).show();
                return true;
            }

            if (msg.what == BluetoothCommunicationService.MessageConstants.MESSAGE_READ)
            {
                byte[] bytes = (byte[]) msg.obj;

                if (!protocolTypeLayer.getReceiver().set(bytes))
                    Log.w(TAG, "Got invalid message, parsing can't continue");
                else
                    messageParser(protocolTypeLayer.getReceiver().type(), protocolTypeLayer.getReceiver().payload());

                return true;
            }

            if (msg.what == BluetoothCommunicationService.MessageConstants.MESSAGE_WRITE)
            {
                freezeUISharingControls(false);
                Toast.makeText(ClientActivity.super.getApplicationContext(), "Message sent (" + msg.arg1 + " bytes)", Toast.LENGTH_SHORT).show();
                return true;
            }

            if (msg.what == BluetoothCommunicationService.MessageConstants.MESSAGE_DISCONNECTED)
            {
                Toast.makeText(ClientActivity.super.getApplicationContext(), "Server closed", Toast.LENGTH_SHORT).show();
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
                text = "Got 'null' object as message";
                Log.e(TAG, text);
            } else
            {
                text = "Received: " + new String(payload);
                Log.i(TAG, "Type TEXT: " + text);
            }

            int length = text.length() < 30 ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG;
            Toast.makeText(ClientActivity.super.getApplicationContext(), text, length).show();
        } else if (type == eProtocolTypeLayer.Types.FILE_RESPONSE)
        {
            if (payload == null) return;

            String fileName = new String(payload);
            if (fileName.equals(fileCollector.getName()))
            {
                final byte[] messageBytes = protocolTypeLayer.getTransmitter().form(eProtocolTypeLayer.Types.FILE_CONTENT, fileCollector.getData());
                String fileSize = String.format(Locale.ENGLISH, "%.2f", (float) fileCollector.getData().length / (1024f * 1024f));

                Toast.makeText(this, "Sending the file (" + fileSize + " MB). Please, wait.", Toast.LENGTH_SHORT).show();
                freezeUISharingControls(true);

                new Thread(new Runnable() // We creating new thread, because happens locking of our UI but the file's content could be very big.
                {
                    @Override
                    public void run() { bcs.write(messageBytes); }
                }).start();
            } else if (fileName.equals(REJECT_WORD))
            {
                Toast.makeText(this, "Server rejected request", Toast.LENGTH_SHORT).show();
            }
        } else if (type == eProtocolTypeLayer.Types.SCREENSHOT_RESPONSE)
        {
            if (!Utils.isExternalStorageWritable())
                Log.e(TAG, "Type SCREENSHOT_RESPONSE: External storage is not mounted for saving image!");

            File screenshot = ScreenshotUtil.saveScreenshot(Utils.getPrivateScreenshotsDir(this), payload);
            if (screenshot == null)
            {
                Toast.makeText(ClientActivity.super.getApplicationContext(), "Screenshot saving failed", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(ClientActivity.super.getApplicationContext(), "Screenshot saved", Toast.LENGTH_SHORT).show();
            ScreenshotUtil.openScreenshot(this, screenshot);
        } else
        {
            Log.e(TAG, "Got message of unknown type (=" + type + ")");
        }
    }
}
