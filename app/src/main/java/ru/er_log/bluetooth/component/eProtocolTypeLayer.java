package ru.er_log.bluetooth.component;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import static ru.er_log.bluetooth.MainActivity.TAG;

public class eProtocolTypeLayer
{
    /**
     * Messages of this type encapsulates into eProtocol and have following format:
     *      [32 bits for TYPE][pure data]
     *  <=>
     *      [int][bytes of pure data].
     */

    public enum Types
    {
        TEXT(0x0),                  // Data - just the text.
        FILE_REQUEST(0x10),         // Data - request for send file with filename in payload.
        FILE_RESPONSE(0x11),        // Data - 'ok' response for request.
        FILE_CONTENT(0x12),         // Data - file content.
        SCREENSHOT_REQUEST(0x20),   // Data isn't assumed. Message - request for getting screenshot.
        SCREENSHOT_RESPONSE(0x21);

        private final int index;
        private Types(int index) { this.index = index; }
        public int getIndex() { return this.index; }
    }

    private final Transmitter transmitter;
    private final Receiver receiver;

    public eProtocolTypeLayer()
    {
        this.transmitter = new Transmitter();
        this.receiver = new Receiver();
    }

    public Transmitter getTransmitter()
    {
        return transmitter;
    }

    public Receiver getReceiver()
    {
        return receiver;
    }

    public final class Transmitter
    {
        private Transmitter() {}

        /* Form message according protocol by adding TYPE ahead. */
        public byte[] form(Types type, byte[] data)
        {
            if (type == null) throw new NullPointerException("'type' was null");

            int bufLen = (data == null) ? 4 : 4 + data.length;
            ByteBuffer bytes = ByteBuffer.allocate(bufLen);
            bytes.putInt(type.getIndex());
            if (data != null)
                bytes.put(data);

            return bytes.array();
        }
    }

    public final class Receiver
    {
        /**
         * It's possible to {@code put()} any byte stream according to the protocol
         * without worrying about sticking together.
         */

        private byte[] message;
        private Types type;
        private byte[] payload;

        private final byte[] pType;
        private final ByteArrayOutputStream pData;

        private Receiver()
        {
            this.message = null;
            this.type = null;
            this.payload = null;

            this.pType = new byte[4];
            this.pData = new ByteArrayOutputStream();
        }

        // Return TRUE if message correct.
        public boolean set(byte[] message)
        {
            reset();
            this.message = message;
            return parse();
        }

        public void reset()
        {
            pData.reset();
            this.message = null;
            this.type = null;
            this.payload = null;
        }

        public Types type()
        {
            return this.type;
        }

        public byte[] payload()
        {
            return this.payload;
        }

        private boolean parse()
        {
            if (message == null || message.length < pType.length)
                return false;

            int pTypePtr = 0, bufPtr = -1;
            while (++bufPtr < message.length)
            {
                if (pTypePtr < pType.length)
                    pType[pTypePtr++] = message[bufPtr];
                else
                    pData.write(message[bufPtr]);
            }

            this.type = null;
            int typeInt = ByteBuffer.wrap(pType).getInt();
            for (Types t : Types.values())
                if (t.getIndex() == typeInt)
                {
                    this.type = t;
                    break;
                }

            if (type == null)
            {
                Log.w(TAG, "Got message of unknown type (type.index=" + typeInt + ")");
                reset();
                return false;
            }

            if (pData.size() > 0)
                this.payload = pData.toByteArray();

            return true;
        }
    }
}
