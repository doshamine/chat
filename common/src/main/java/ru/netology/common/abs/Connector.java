package ru.netology.common.abs;

import java.net.Socket;

public abstract class Connector {
    protected final Socket socket;

    protected Connector(Socket socket) {
        this.socket = socket;
    }
}
