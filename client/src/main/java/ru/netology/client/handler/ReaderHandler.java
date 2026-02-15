package ru.netology.client.handler;

import ru.netology.client.Client;
import ru.netology.common.handler.Handler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.BlockingQueue;

public class ReaderHandler extends Handler {
    private final BlockingQueue<String> messageQueue;

    public ReaderHandler(Socket socket, BlockingQueue<String> messageQueue) {
        super(socket);
        this.messageQueue = messageQueue;
    }

    @Override
    public void run() {
        int socketTimeout = Integer.parseInt(Client.getProperty("socket.timeout"));

        try {
            socket.setSoTimeout(socketTimeout);
        } catch (SocketException e) {
            logger.warning("Не удалось настроить таймаут сокета: " + e.getMessage());
        }

        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            logger.info("Готов к приему сообщений");
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String msg = in.readLine();

                    if (msg == null) {
                        logger.info("Соединение с сервером прервано");
                        System.err.println("Соединение с сервером прервано");
                        break;
                    }

                    logger.info("Получено сообщение");
                    messageQueue.put(msg);
                } catch (IOException e) {
                    if (!(e instanceof SocketTimeoutException)) {
                        logger.severe("Ошибка при получении сообщения");
                    }
                } catch (InterruptedException e) {
                    logger.severe("Получение сообщения прервано");
                }
            }
            logger.info("Прием сообщений завершен");
        } catch (IOException e) {
            logger.severe("Ошибка потока ввода: " + e.getMessage());
            System.err.println("Ошибка потока ввода");
        }
    }
}
