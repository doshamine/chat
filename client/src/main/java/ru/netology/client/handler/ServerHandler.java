package ru.netology.client.handler;

import ru.netology.client.printer.MessagePrinter;
import ru.netology.common.abs.SocketHandler;
import ru.netology.common.abs.LoggableRunner;

import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ServerHandler extends SocketHandler implements LoggableRunner {
    public ServerHandler(Socket socket) {
        super(socket);
    }

    @Override
    public void run() {
        final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();

        try {
            Thread registerThread = new Thread(new RegisterHandler(socket));
            Thread readerThread = new Thread(new ReaderHandler(socket, messageQueue));
            Thread writerThread = new Thread(new WriterHandler(socket));
            Thread printerThread = new Thread(new MessagePrinter(messageQueue));

            registerThread.start();
            synchronized (socket) {
                socket.wait();
            }

            readerThread.start();
            printerThread.start();
            writerThread.start();

            writerThread.join();

            printerThread.interrupt();
            readerThread.interrupt();

            synchronized (socket) {
                socket.notify();
            }
        } catch (InterruptedException e) {
            logger.warning("Работа с сообщениями прервана: " + e.getMessage());
            System.err.println("Работа с сообщениями прервана");
        }
    }
}
