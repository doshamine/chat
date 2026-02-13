import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Client {
    private static final Logger logger = Logger.getLogger(Client.class.getName());
    private static final String propertiesFilename = "conf.properties";

    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();

    public static void main(String[] args) {
        configureLogger();
        String host  = getProperty("server.host");
        int port = Integer.parseInt(getProperty("server.port"));

        Client client = new Client();
        client.start(host, port);
    }

    public void start(String host, int port) {
        String greeting = getProperty("server.greeting");
        System.out.println(greeting);
        Scanner sc = new Scanner(System.in);

        try (
            Socket socket = new Socket(host, port);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            logger.info("Подключение к серверу " + host + ":" + port);
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

            Thread receiverThread = new Thread(new Receiver(socket, messageQueue));
            Thread senderThread = new Thread(new Sender(socket, messageQueue));

            receiverThread.start();
            senderThread.start();
            senderThread.join();

            receiverThread.interrupt();
            logger.info("Выход из чата");

        } catch (IOException e) {
            logger.severe("Соединение с сервером прервано: " + e.getMessage());
        } catch (InterruptedException e) {
            logger.warning("Прервана работа потока: " + e.getMessage());
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

    private static class Receiver implements Runnable {
        final Socket socket;
        BlockingQueue<String> messageQueue;

        public Receiver(Socket socket, BlockingQueue<String> messageQueue) {
            this.socket = socket;
            this.messageQueue = messageQueue;
        }

        @Override
        public void run() {
            int socketTimeout = Integer.parseInt(getProperty("socket.timeout"));

            try {
                socket.setSoTimeout(socketTimeout);
            } catch (SocketException e) {
                logger.warning("Не удалось настроить таймаут сокета");
            }

            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                logger.info("Готов к приему сообщений");
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        String msg = in.readLine();

                        if (msg == null) {
                            logger.severe("Соединение с сервером прервано");
                            break;
                        }

                        logger.info("Получено сообщение");
                        messageQueue.put(msg);
                    } catch (IOException e) {
                        if (!(e instanceof SocketTimeoutException)) {
                            logger.warning("Ошибка при получении сообщения");
                        }
                    } catch (InterruptedException e) {
                        logger.severe("Получение сообщения прервано");
                    }
                }
                logger.info("Прием сообщений завершен");
            } catch (IOException e) {
                logger.severe("Ошибка потока ввода");
            }
        }
    }

    private static class Sender implements Runnable {
        final Socket socket;
        BlockingQueue<String> messageQueue;

        public Sender(Socket socket, BlockingQueue<String> messageQueue) {
            this.socket = socket;
            this.messageQueue = messageQueue;
        }

        @Override
        public void run() {
            String exitCommand = getProperty("client.exit");
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
                logger.severe("Ошибка потока вывода");
            } catch (InterruptedException e) {
                logger.warning("Работа с очередью сообщений прервана");
            }
        }
    }

    private static void configureLogger() {
        String logFilename = getProperty("log.filename");

        try {
            FileHandler fileHandler = new FileHandler(logFilename, true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            System.err.println("Ошибка при открытии файла: " + e.getMessage());
        }

        logger.setUseParentHandlers(false);
    }
}
