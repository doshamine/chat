package ru.netology.server.handler;

import ru.netology.common.abs.Connector;
import ru.netology.common.abs.LoggableRunner;
import ru.netology.common.message.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientHandler extends Connector implements LoggableRunner {
    final Map<String, BlockingQueue<Message>> queuesMap;

    public ClientHandler(Socket socket, Map<String, BlockingQueue<Message>> queuesMap) {
        super(socket);
        this.queuesMap = queuesMap;
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            out.println("Привет! Введи никнейм: ");
            out.flush();
            String username = in.readLine();

            while (queuesMap.containsKey(username)) {
                out.println("Никнейм уже занят! Введи другой: ");
                out.flush();
                username = in.readLine();
            }

            queuesMap.put(username, new LinkedBlockingQueue<>());
            out.println("Добро пожаловать!");
            out.flush();
            logger.info(username + ": вход в чат");

            Thread writerThread = new Thread(new WriterHandler(socket, queuesMap.get(username)));
            Thread readerThread = new Thread(new ReaderHandler(socket, username, queuesMap));

            writerThread.start();
            readerThread.start();

            writerThread.join();
            readerThread.join();
            logger.info(username + ": выход из чата");

            socket.close();
            queuesMap.remove(username);
        } catch (IOException e) {
            logger.severe("Ошибка работы потоков ввода/вывода: " + e.getMessage());
        } catch (InterruptedException e) {
            logger.warning("Прерывание работы чтения записи: " + e.getMessage());
        }
    }
}