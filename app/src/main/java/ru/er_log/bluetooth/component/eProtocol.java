package ru.er_log.bluetooth.component;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static ru.er_log.bluetooth.MainActivity.TAG;

public class eProtocol
{
    /**
     * Message format of this protocol is:
     *      [32 bits for LENGTH][pure data with LENGTH size]
     *  <=>
     *      [int][bytes of pure data].
     */

    private final Transmitter transmitter;
    private final Receiver receiver;
    private final List<byte[]> newMessagesList;

    public eProtocol()
    {
        this.transmitter = new Transmitter();
        this.receiver = new Receiver();
        this.newMessagesList = new ArrayList<>();
    }

    public Transmitter getTransmitter()
    {
        return transmitter;
    }

    public Receiver getReceiver()
    {
        return receiver;
    }

    /* Note: messages list will be cleared after a call. */
    public List<byte[]> getRecentlyFormedMessagesList()
    {
        List<byte[]> copiedList = new ArrayList<>(newMessagesList);
        newMessagesList.clear();

        return copiedList;
    }

    public final class Transmitter
    {
        private Transmitter() {}

        /* Form message according protocol by adding LENGTH ahead. */
        public byte[] form(byte[] data)
        {
            if (data == null) throw new NullPointerException("'data' was null");

            ByteBuffer bytes = ByteBuffer.allocate(4 + data.length);
            bytes.putInt(data.length);
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

        private byte pLengthPtr;
        private final byte[] pLength;
        private final ByteArrayOutputStream pData;

        private Receiver()
        {
            this.pLengthPtr = 0;
            this.pLength = new byte[4];
            this.pData = new ByteArrayOutputStream();
        }

        private void shiftMessage()
        {
            newMessagesList.add(pData.toByteArray());
            reset();
        }

        private int getLengthInt()
        {
            if (pLengthPtr == pLength.length)
                return ByteBuffer.wrap(pLength).getInt();
            else
                return -1;
        }

        /* Parse bytes and return number of formed messages. */
        public int put(byte[] buf, int len)
        {
            if (len <= 0) return 0;
            if (buf == null) throw new NullPointerException("'buf' was null");
            if (buf.length < len) throw new IllegalArgumentException("'len' more than max. 'buf' size");

            int bufPtr = -1, counter = 0;
            while (++bufPtr < len)
            {
                if (pLengthPtr < pLength.length)
                    pLength[pLengthPtr++] = buf[bufPtr];
                else
                    pData.write(buf[bufPtr]);

                // If message formed then we are ready to shift it & form new.
                if (pData.size() == getLengthInt())
                {
                    shiftMessage();
                    counter++;
                }
            }

            return counter;
        }

        /* Terminate forming process for current message and can start a new message,
         * even if not all data was received. */
        public void putAndReset()
        {
            if (pLengthPtr == pLength.length && pData.size() > 0)
                shiftMessage();
            else
                reset();
        }

        /* Just terminate forming process for current message. */
        public void reset()
        {
            this.pLengthPtr = 0;
            this.pData.reset();
        }
    }
}
