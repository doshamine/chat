package ru.netology.client.handler;

import ru.netology.client.Client;
import ru.netology.common.handler.Handler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ServerHandler extends Handler {
    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();

    public ServerHandler(Socket socket) {
        super(socket);
    }

    @Override
    public void run() {
        String greeting = Client.getProperty("server.greeting");
        Scanner sc = new Scanner(System.in);

        try (
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            while (true) {
                String prompt = in.readLine();

                if (prompt == null) {
                    throw new SocketException("Соединение с сервером прервано");
                }

                System.out.print(prompt);

                if (prompt.equals(greeting)) {
                    System.out.println();
                    break;
                }

                String username = sc.nextLine();
                out.println(username);
                out.flush();
                logger.info("Попытка входа с именем " + username);
            }
            logger.info("Вход в чат");

            Thread receiverThread = new Thread(new ReaderHandler(socket, messageQueue));
            Thread senderThread = new Thread(new WriterHandler(socket, messageQueue));

            receiverThread.start();
            senderThread.start();
            senderThread.join();

            receiverThread.interrupt();
            logger.info("Выход из чата");

        } catch (IOException e) {
            logger.severe("Ошибка ввода/вывода: " + e.getMessage());
            System.err.println("Ошибка ввода/вывода");
        } catch (InterruptedException e) {
            logger.warning("Работа с сообщениями прервана: " + e.getMessage());
            System.err.println("Работа с сообщениями прервана");
        }
    }
}
