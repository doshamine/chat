package ru.netology.common.handler;

import java.net.Socket;
import java.util.logging.Logger;

public abstract class Handler implements Runnable {
    protected final Logger logger = Logger.getLogger(getClass().getName());
    protected final Socket socket;

    protected Handler(Socket socket) {
        this.socket = socket;
    }
}
