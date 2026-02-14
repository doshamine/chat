package ru.netology.server.handler;

import ru.netology.common.message.Message;
import ru.netology.server.Server;
import ru.netology.common.handler.Handler;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;

public class WriterHandler extends Handler {
    final String username;
    final BlockingQueue<Message> messageQueue;

    public WriterHandler(Socket socket, String username, BlockingQueue<Message> messageQueue) {
        super(socket);
        this.username = username;
        this.messageQueue = messageQueue;
    }

    @Override
    public void run() {
        String timeFormat = Server.getProperty("time.format");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timeFormat);

        try (
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            while (!socket.isClosed()) {
                if (!messageQueue.isEmpty()) {
                    Message msg = messageQueue.take();

                    out.println(String.format(
                        "%s (%s): %s", msg.getUsername(),
                        msg.getCreatedAt().format(formatter),
                        msg.getText()
                    ));
                    out.flush();
                    logger.info(username + ": прием сообщения");
                }
            }

        } catch (IOException e) {
            logger.severe(username + ": ошибка открытия потока вывода");
        } catch (InterruptedException e) {
            logger.severe(username + ": прервана работа с очередью сообщений");
        }
    }
}