package ru.er_log.bluetooth;

public interface IServerConnectorCallback
{
    public static final int CODE_ERROR_LISTEN = 0;
    public static final int CODE_ERROR_CLOSE_SOCKET = 1;

    void onServerListen();              // When listen succeed.
    void onServerDisconnect();          // When disconnect succeed.
    void onServerError(final int code); // When error occurs.
}
