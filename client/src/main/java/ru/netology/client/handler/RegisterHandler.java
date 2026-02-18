package ru.netology.client.handler;

import ru.netology.client.Client;
import ru.netology.common.abs.SocketHandler;
import ru.netology.common.abs.LoggableRunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class RegisterHandler extends SocketHandler implements LoggableRunner {
    public RegisterHandler(Socket socket) {
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
                    throw new IOException();
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
            synchronized (socket) {
                socket.notify();
                socket.wait();
            }
            logger.info("Выход из чата");
        } catch (IOException e) {
            String errMessage = "Ошибка потока ввода/вывода: " + e.getMessage();
            logger.severe(errMessage);
            System.err.println(errMessage);
        } catch (InterruptedException e) {
            String errMessage = "Работа некорректно прервана " +  e.getMessage();
            logger.severe(errMessage);
            System.err.println(errMessage);
        }
    }
}
