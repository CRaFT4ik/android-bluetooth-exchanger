package ru.er_log.bluetooth;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import ru.er_log.bluetooth.component.eProtocol;

import static ru.er_log.bluetooth.MainActivity.TAG;

public class BluetoothCommunicationService
{
    private eProtocol protocol; // Protocol for correct send/receive messages.
    private Handler handler; // Handler that gets info from Bluetooth service.
    private ConnectedThread connectedThread;

    // Defines several constants used when transmitting messages between the
    // service and the UI.
    public interface MessageConstants
    {
        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;
        public static final int MESSAGE_DISCONNECTED = 3;
        public static final int MESSAGE_READ_BUFFER_FILLED = 4;

        // ... (Add other message Types here as needed.)
    }

    BluetoothCommunicationService(BluetoothSocket socket, Handler handler)
    {
        this.protocol = new eProtocol();
        this.handler = handler;
        this.connectedThread = new ConnectedThread(socket);
    }

    void read()
    {
        if (connectedThread == null)
            throw new NullPointerException();

        connectedThread.start();
    }

    void write(byte[] data)
    {
        if (connectedThread == null)
            throw new NullPointerException();

        connectedThread.write(data);
    }

    void cancel()
    {
        if (connectedThread == null)
            throw new NullPointerException();

        connectedThread.cancel();
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread
    {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream

        private ConnectedThread(BluetoothSocket socket)
        {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try
            {
                tmpIn = socket.getInputStream();
            } catch (IOException e)
            {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try
            {
                tmpOut = socket.getOutputStream();
            } catch (IOException e)
            {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run()
        {
            mmBuffer = new byte[1024];
            int numBytes; // Bytes returned from read().
            int formedMessages; // Number of formed messages in current iteration.

            Message disconnectedMsg = handler.obtainMessage(MessageConstants.MESSAGE_DISCONNECTED, -1, -1, null);

            // Keep listening to the InputStream until an exception occurs.
            while (true)
            {
                try
                {
                    // Read from the InputStream.
                    numBytes = mmInStream.read(mmBuffer);
                    if (numBytes == -1)
                    {
                        Log.d(TAG, "Input stream was disconnected: there is no more data because the end of the stream has been reached");
                        protocol.getReceiver().reset();
                        disconnectedMsg.sendToTarget();
                        break;
                    }

                    // Parse raw data by protocol rules.
                    formedMessages = protocol.getReceiver().put(mmBuffer, numBytes);
                    Log.d(TAG, "Received " + numBytes + " bytes, formed " + formedMessages + " messages");

                    // Send the obtained bytes to the UI activity.
                    while (formedMessages-- > 0)
                    {
                        Message readMsg = handler.obtainMessage(MessageConstants.MESSAGE_READ, -1, -1, protocol.getLastReceivedMessage());
                        readMsg.sendToTarget();
                    }
                } catch (IOException e)
                {
                    Log.d(TAG, "Input stream was disconnected", e);
                    protocol.getReceiver().reset();
                    disconnectedMsg.sendToTarget();
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        private void write(byte[] bytes)
        {
            try
            {
                mmOutStream.write(protocol.getTransmitter().form(bytes));
            } catch (IOException e)
            {
                Log.e(TAG, "Error occurred when sending data", e);

                // Send a failure message back to the activity.
                Message writeErrorMsg = handler.obtainMessage(MessageConstants.MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast", "Couldn't send data to the other device");
                writeErrorMsg.setData(bundle);
                handler.sendMessage(writeErrorMsg);
                return;
            }

            // Share the sent message with the UI activity.
            Message writtenMsg = handler.obtainMessage(MessageConstants.MESSAGE_WRITE, bytes.length, -1, null);
            writtenMsg.sendToTarget();
        }

        // Call this method from the main activity to shut down the connection.
        private void cancel()
        {
            try
            {
                mmSocket.close();
            } catch (IOException e)
            {
                Log.e(TAG, "Could not close the socket", e);
            }
        }
    }
}
