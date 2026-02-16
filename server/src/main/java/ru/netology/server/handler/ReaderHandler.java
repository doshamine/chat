package ru.netology.server.handler;

import ru.netology.common.abs.Connector;
import ru.netology.common.abs.LoggableRunner;
import ru.netology.common.message.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

public class ReaderHandler extends Connector implements LoggableRunner {
    final String username;
    final Map<String, BlockingQueue<Message>> queuesMap;

    public ReaderHandler(Socket socket, String username, Map<String, BlockingQueue<Message>> queuesMap) {
        super(socket);
        this.username = username;
        this.queuesMap = queuesMap;
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            while (true) {
                String text;
                text = in.readLine();

                if (text == null) {
                    break;
                }

                Message message = new Message(username, text);
                for (Map.Entry<String, BlockingQueue<Message>> entry : queuesMap.entrySet()) {
                    entry.getValue().put(message);
                }
                logger.info(username + ": отправка сообщения");
            }
        } catch (IOException e) {
            logger.severe(username + ": ошибка открытия потока ввода");
        } catch (InterruptedException e) {
            logger.severe(username + ": прервана работа с очередью сообщений");
        }
    }
}