package ru.netology.client.handler;

import ru.netology.client.Client;
import ru.netology.common.abs.SocketHandler;
import ru.netology.common.abs.LoggableRunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.BlockingQueue;

public class ReaderHandler extends SocketHandler implements LoggableRunner {
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
            String errMessage = "Не удалось настроить таймаут сокета: " + e.getMessage();
            logger.severe(errMessage);
            System.err.println(errMessage);
            return;
        }

        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            logger.info("Готов к приему сообщений");
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String msg = in.readLine();

                    if (msg == null) {
                        break;
                    }

                    logger.info("Получено сообщение");
                    messageQueue.put(msg);
                } catch (IOException e) {
                    if (!(e instanceof SocketTimeoutException)) {
                        String errMessage = "Ошибка при получении сообщения: " + e.getMessage();
                        logger.warning(errMessage);
                        break;
                    }
                } catch (InterruptedException e) {
                    String errMessage = "Получение сообщения прервано: " + e.getMessage();
                    logger.severe(errMessage);
                    System.err.println(errMessage);
                    break;
                }
            }
            logger.info("Прием сообщений завершен");
        } catch (IOException e) {
            String errMessage = "Ошибка потока ввода " + e.getMessage();
            logger.severe(errMessage);
            System.err.println(errMessage);
        }
    }
}
