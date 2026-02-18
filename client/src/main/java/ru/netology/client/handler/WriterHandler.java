package ru.netology.client.handler;

import ru.netology.client.Client;
import ru.netology.common.abs.SocketHandler;
import ru.netology.common.abs.LoggableRunner;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class WriterHandler extends SocketHandler implements LoggableRunner {
    public WriterHandler(Socket socket) {
        super(socket);
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
                    System.out.println("Выход");
                    break;
                }

                out.println(text);
                out.flush();
                logger.info("Отправлено сообщение");
            }
        } catch (IOException e) {
            String errMessage = "Ошибка потока вывода: " +  e.getMessage();
            logger.severe(errMessage);
            System.err.println(errMessage);
        }
    }
}
