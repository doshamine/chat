package ru.netology.client.handler;

import ru.netology.client.Client;
import ru.netology.common.handler.Handler;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;

public class WriterHandler extends Handler {
    private final BlockingQueue<String> messageQueue;

    public WriterHandler(Socket socket, BlockingQueue<String> messageQueue) {
        super(socket);
        this.messageQueue = messageQueue;
    }

    @Override
    public void run() {
        String exitCommand = Client.getProperty("client.exit");
        Scanner sc = new Scanner(System.in);

        try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            while (true) {
                logger.info("Готов к отправке сообщений");
                System.out.print("> ");
                String text = sc.nextLine();
                if (text.equals(exitCommand)) {
                    break;
                }

                out.println(text);
                out.flush();
                logger.info("Отправлено сообщение");

                while (!messageQueue.isEmpty()) {
                    System.out.println(messageQueue.take());
                }
            }
        } catch (IOException e) {
            logger.severe("Ошибка потока вывода: " +  e.getMessage());
            System.err.println("Ошибка потока ввода");
        } catch (InterruptedException e) {
            logger.warning("Работа с очередью сообщений прервана: " + e.getMessage());
            System.err.println("Работа с очередью сообщений прервана");
        }
    }
}
