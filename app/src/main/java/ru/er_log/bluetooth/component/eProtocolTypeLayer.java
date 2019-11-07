package ru.er_log.bluetooth.component;

import android.util.Log;

import java.nio.ByteBuffer;

import static ru.er_log.bluetooth.MainActivity.TAG;

public class eProtocolTypeLayer
{
    /**
     * Messages of this parsedType encapsulates into eProtocol and have following format:
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
            if (type == null) throw new NullPointerException("'parsedType' was null");

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
        private byte[] payload;
        private final byte[] type;
        private Types parsedType;

        private Receiver()
        {
            this.message = null;
            this.payload = null;
            this.type = new byte[4];
            this.parsedType = null;
        }

        // Return TRUE if message correct.
        public boolean set(byte[] message)
        {
            reset();
            this.message = message;
            this.payload = new byte[message.length - type.length];
            return parse();
        }

        public void reset()
        {
            this.message = null;
            this.parsedType = null;
            this.payload = null;
        }

        public Types type()
        {
            return this.parsedType;
        }

        public byte[] payload()
        {
            return this.payload;
        }

        private boolean parse()
        {
            if (message == null || message.length < type.length)
                return false;

            for (int ptr = -1; ++ptr < message.length;)
            {
                if (ptr < type.length)
                    type[ptr] = message[ptr];
                else
                    payload[ptr - type.length] = message[ptr];
            }

            this.parsedType = null;
            int typeInt = ByteBuffer.wrap(type).getInt();
            for (Types t : Types.values())
                if (t.getIndex() == typeInt)
                {
                    this.parsedType = t;
                    break;
                }

            if (parsedType == null)
            {
                Log.w(TAG, "Got message of unknown parsedType (parsedType.index=" + typeInt + ")");
                reset();
                return false;
            }

            return true;
        }
    }
}
