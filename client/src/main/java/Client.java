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
    public static BlockingQueue<String> queue = new LinkedBlockingQueue<>();
    private static Logger logger = Logger.getLogger(Client.class.getName());
    private static FileHandler fileHandler;

    public static void main(String[] args) {
        Properties props = Client.getProperties("conf.properties");
        String host  = props.getProperty("server.host");
        String port = props.getProperty("server.port");
        Scanner sc = new Scanner(System.in);
        configureLogger();

        try (
            Socket socket = new Socket(host, Integer.parseInt(port));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        ) {
            logger.info("Подключение к серверу " + host + ":" + port);

            while (true) {
                String prompt = in.readLine();

                if (prompt == null) {
                    throw new Exception("Соединение с сервером прервано");
                }

                System.out.print(prompt);

                if (prompt.equals("Добро пожаловать!")) {
                    System.out.println();
                    break;
                }

                String username = sc.nextLine();
                out.println(username);
                out.flush();
                logger.info("Попытка входа с именем " + username);
            }

            logger.info("Вход в чат");
            Thread receiverThread = new Thread(new Receiver(socket));
            Thread senderThread = new Thread(new Sender(socket));

            receiverThread.start();
            senderThread.start();

            senderThread.join();
            receiverThread.interrupt();
            logger.info("Выход из чата");

        } catch (Exception e) {
            logger.severe("Ошибка соединения с сервером");
        }
    }

    static Properties getProperties(String propertiesFilename) {
        Properties props = new Properties();
        try (InputStream is = Client.class.getClassLoader()
                .getResourceAsStream(propertiesFilename)) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return props;
    }

    private static class Receiver implements Runnable {
        final Socket socket;

        public Receiver(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            Properties props = Client.getProperties("conf.properties");
            int socketTimeout = Integer.parseInt(props.getProperty("socket.timeout"));
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
                        queue.put(msg);
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

        public Sender(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            Properties props = Client.getProperties("conf.properties");
            String exitCommand = props.getProperty("client.exit");
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

                    while (!queue.isEmpty()) {
                        System.out.println(queue.take());
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
        Properties props = getProperties("conf.properties");
        String logFilename = props.getProperty("log.filename");

        try {
            fileHandler = new FileHandler(logFilename, true);
        } catch (IOException e) {
            System.err.println("Ошибка при открытии файла: " + e.getMessage());
        }

        fileHandler.setFormatter(new SimpleFormatter());
        logger.addHandler(fileHandler);
        logger.setUseParentHandlers(false);
    }
}
