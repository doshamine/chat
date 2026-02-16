package ru.netology.client.handler;

import ru.netology.client.Client;
import ru.netology.common.abs.Connector;
import ru.netology.common.abs.LoggableRunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class RegistrationHandler extends Connector implements LoggableRunner {
    public RegistrationHandler(Socket socket) {
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
            synchronized (ServerHandler.monitor) {
                ServerHandler.monitor.notify();
                ServerHandler.monitor.wait();
            }
            logger.info("Выход из чата");
        } catch (IOException e) {
            logger.severe("Ошибка ввода/вывода: " + e.getMessage());
            System.err.println("Ошибка ввода/вывода");
        } catch (InterruptedException e) {
            String errMessage = "Работа завершена некорректно " +  e.getMessage();
            logger.severe(errMessage);
            System.err.println(errMessage);
        }
    }
}
