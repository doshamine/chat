package ru.netology.server.handler;

import ru.netology.common.abs.SocketHandler;
import ru.netology.common.abs.LoggableRunner;
import ru.netology.common.message.Message;
import ru.netology.server.Server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;

public class WriterHandler extends SocketHandler implements LoggableRunner {
    final BlockingQueue<Message> messageQueue;

    public WriterHandler(Socket socket, BlockingQueue<Message> messageQueue) {
        super(socket);
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
                    logger.info("Прием сообщения");
                }
            }

        } catch (IOException e) {
            String errMessage = "Ошибка открытия потока вывода";
            logger.severe(errMessage);
            System.err.println(errMessage);
        } catch (InterruptedException e) {
            String errMessage = "Прервана работа с очередью сообщений";
            logger.severe(errMessage);
            System.err.println(errMessage);
        }
    }
}