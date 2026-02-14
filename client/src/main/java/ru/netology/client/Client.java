package ru.netology.client;

import ru.netology.client.handler.ServerHandler;

import java.io.*;
import java.net.Socket;
import java.util.Properties;
import java.util.logging.Logger;

public class Client {
    private final Logger logger = Logger.getLogger(Client.class.getName());
    private static final String propertiesFilename = "conf.properties";

    public static void main(String[] args) {
        String host  = getProperty("server.host");
        int port = Integer.parseInt(getProperty("server.port"));

        Client client = new Client();
        client.start(host, port);
    }

    public void start(String host, int port) {
        try (Socket socket = new Socket(host, port)) {
            logger.info("Подключение к серверу " + host + ":" + port);

            Thread handlerThread = new Thread(new ServerHandler(socket));
            handlerThread.start();
            handlerThread.join();

        } catch (IOException e) {
            logger.severe("Соединение с сервером прервано: " + e.getMessage());
            System.err.println("Соединение с сервером прервано");
        } catch (InterruptedException e) {
            logger.warning("Работа программы прервана: " + e.getMessage());
            System.err.println("Работа программы прервана");
        }
    }

    public static String getProperty(String property) {
        Properties props = new Properties();
        try (InputStream is = Client.class.getClassLoader()
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
