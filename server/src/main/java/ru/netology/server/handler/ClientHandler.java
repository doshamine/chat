package ru.netology.server.handler;

import ru.netology.common.abs.SocketHandler;
import ru.netology.common.abs.LoggableRunner;
import ru.netology.common.message.Message;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientHandler extends SocketHandler implements LoggableRunner {
    final Map<String, BlockingQueue<Message>> queuesMap;
    final BlockingQueue<String> usernameQueue = new LinkedBlockingQueue<>();

    public ClientHandler(Socket socket, Map<String, BlockingQueue<Message>> queuesMap) {
        super(socket);
        this.queuesMap = queuesMap;
    }

    @Override
    public void run() {
        try {
            Thread registerThread = new Thread(new RegisterHandler(socket, queuesMap, usernameQueue));
            registerThread.start();
            String username = usernameQueue.take();
            logger.info(username + ": вход в чат");

            Thread writerThread = new Thread(new WriterHandler(socket, queuesMap.get(username)));
            Thread readerThread = new Thread(new ReaderHandler(socket, username, queuesMap));

            writerThread.start();
            readerThread.start();

            writerThread.join();
            readerThread.join();
            logger.info(username + ": выход из чата");

            synchronized (socket) {
                socket.notify();
            }
            socket.close();
        } catch (IOException e) {
            String errMessage = "Ошибка работы потоков ввода/вывода: " + e.getMessage();
            logger.severe(errMessage);
            System.err.println(errMessage);
        } catch (InterruptedException e) {
            String errMessage = "Прерывание работы чтения/записи: " + e.getMessage();
            logger.warning(errMessage);
            System.err.println(errMessage);
        }
    }
}