package ru.netology.client.handler;

import ru.netology.client.printer.MessagePrinter;
import ru.netology.common.abs.Connector;
import ru.netology.common.abs.LoggableRunner;

import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ServerHandler extends Connector implements LoggableRunner {
    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
    static final Object monitor = new Object();

    private final RegistrationHandler registerHandler;
    private final ReaderHandler readHandler;
    private final WriterHandler writeHandler;
    private final MessagePrinter messagePrinter;

    public ServerHandler(Socket socket) {
        super(socket);
        registerHandler = new RegistrationHandler(socket);
        readHandler = new ReaderHandler(socket, messageQueue);
        writeHandler = new WriterHandler(socket);
        messagePrinter = new MessagePrinter(messageQueue);
    }

    @Override
    public void run() {
        try {
            Thread registerThread = new Thread(registerHandler);
            Thread readerThread = new Thread(readHandler);
            Thread writerThread = new Thread(writeHandler);
            Thread printerThread = new Thread(messagePrinter);

            registerThread.start();
            synchronized (monitor) {
                monitor.wait();
            }

            readerThread.start();
            printerThread.start();
            writerThread.start();

            writerThread.join();

            printerThread.interrupt();
            readerThread.interrupt();

            synchronized (monitor) {
                monitor.notify();
            }
        } catch (InterruptedException e) {
            logger.warning("Работа с сообщениями прервана: " + e.getMessage());
            System.err.println("Работа с сообщениями прервана");
        }
    }
}
