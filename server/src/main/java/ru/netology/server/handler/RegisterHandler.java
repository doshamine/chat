package ru.netology.server.handler;

import ru.netology.common.abs.SocketHandler;
import ru.netology.common.abs.LoggableRunner;
import ru.netology.common.message.Message;
import ru.netology.server.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class RegisterHandler extends SocketHandler implements LoggableRunner {
    private final Map<String, BlockingQueue<Message>> queuesMap;
    private final BlockingQueue<String> usernameQueue;

    public RegisterHandler(
        Socket socket, Map<String, BlockingQueue<Message>> queuesMap,
        BlockingQueue<String> usernameQueue
    ) {
        super(socket);
        this.queuesMap = queuesMap;
        this.usernameQueue = usernameQueue;
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
            String greeting = Server.getProperty("server.greeting");
            out.println(greeting);
            out.flush();
            usernameQueue.put(username);

            synchronized (socket) {
                socket.wait();
            }
            queuesMap.remove(username);
        } catch (IOException e) {
            String errMessage = "Ошибка работы потоков ввода/вывода: " + e.getMessage();
            logger.severe(errMessage);
            System.err.println(errMessage);
        } catch (InterruptedException e) {
            String errMessage = "Прерывание работы чтения/записи: " + e.getMessage();
            logger.warning(errMessage);
            System.err.println(errMessage);
        }
    }
}
