import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.FileHandler;
import java.util.logging.Level;
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
        String exitCommand = props.getProperty("client.exit");
        int socketTimeout = Integer.parseInt(props.getProperty("socket.timeout"));
        Scanner sc = new Scanner(System.in);
        String logFilename = props.getProperty("log.filename");

        Level infoLevel = Level.parse(props.getProperty("log.level.info"));
        Level warningLevel = Level.parse(props.getProperty("log.level.warning"));
        Level severeLevel = Level.parse(props.getProperty("log.level.severe"));

        try {
            fileHandler = new FileHandler(logFilename, true);
        } catch (IOException e) {
            logger.log(severeLevel, "Ошибка при открытии файла: " + e.getMessage());
        }
        fileHandler.setFormatter(new SimpleFormatter());
        logger.addHandler(fileHandler);

        try (
            Socket socket = new Socket(host, Integer.parseInt(port));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        ) {
            logger.log(infoLevel, "Подключение к серверу " + host + ":" + port);
            socket.setSoTimeout(socketTimeout);
            System.out.print(in.readLine());
            String username = sc.nextLine();
            out.println(username);
            out.flush();
            System.out.println(in.readLine());
            logger.log(infoLevel, "Вход в чат под именем " + username);

            Thread receiveThread = new Thread(() -> {
                logger.log(infoLevel, "Готов к приему сообщений");
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        String msg = in.readLine();

                        if (msg != null) {
                            logger.log(infoLevel, "Получено сообщение");
                            queue.put(msg);
                        }
                    } catch (IOException e) {
                        if (!(e instanceof SocketTimeoutException)) {
                            logger.log(warningLevel, "Ошибка при получении сообщения");
                        }
                    } catch (InterruptedException e) {
                        logger.log(warningLevel, "Получение сообщения прервано");
                    }
                }
                logger.log(infoLevel, "Прием сообщений завершен");
            });
            receiveThread.start();

            while (true) {
                System.out.print("> ");
                String text = sc.nextLine();
                if (text.equals(exitCommand)) {
                    logger.log(infoLevel, "Выход из чата");
                    receiveThread.interrupt();
                    break;
                }

                out.println(text);
                out.flush();
                logger.log(infoLevel, "Отправлено сообщение");

                while (!queue.isEmpty()) {
                    System.out.println(queue.take());
                }
            }
        } catch (Exception e) {
            logger.log(severeLevel, "Ошибка соединения с сервером");
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
}
