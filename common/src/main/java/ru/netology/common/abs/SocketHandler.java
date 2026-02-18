package ru.netology.common.abs;

import java.net.Socket;

public abstract class SocketHandler {
    protected final Socket socket;

    protected SocketHandler(Socket socket) {
        this.socket = socket;
    }
}
