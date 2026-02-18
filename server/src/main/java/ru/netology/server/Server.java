package ru.netology.server;

import ru.netology.common.message.Message;
import ru.netology.server.handler.ClientHandler;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class Server {
    private static final String propertiesFilename = "conf.properties";
    private final Logger logger = Logger.getLogger(Server.class.getName());

    private final Map<String, BlockingQueue<Message>> queuesMap = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        int port = Integer.parseInt(getProperty("server.port"));
        Server server = new Server();
        server.start(port);
    }

    public void start(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            logger.info("Сервер запущен");

            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                logger.info("Новое подключение");
                executor.execute(new ClientHandler(socket, queuesMap));
            }
        } catch (IOException e) {
            logger.severe("Не удалось запустить сервер: " + e.getMessage());
        } finally {
            executor.shutdown();
            logger.info("Завершение работы вспомогательных потоков");
        }
    }

    public static String getProperty(String property) {
        Properties props = new Properties();
        try (InputStream is = Server.class.getClassLoader()
            .getResourceAsStream(propertiesFilename)) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        return props.getProperty(property);
    }
}
