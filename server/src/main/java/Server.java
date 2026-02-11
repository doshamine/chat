import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

public class Server {
    private static Map<String, BlockingQueue<Message>> map = new ConcurrentHashMap<>();
    private static Map<String, Socket> users = new ConcurrentHashMap<>();
    private static ExecutorService executor = Executors.newCachedThreadPool();
    private static Logger logger = Logger.getLogger(Server.class.getName());
    private static FileHandler fileHandler;

    public static void main(String[] args) {
        Properties props = getProperties("conf.properties");
        int port = Integer.parseInt(props.getProperty("server.port"));
        String timeFormat = props.getProperty("time.format");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timeFormat);
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

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            logger.log(infoLevel, "Сервер запущен");

            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                logger.log(infoLevel, "Новое подключение");

                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                out.println("Привет! Введи никнейм: ");
                out.flush();
                String username = in.readLine();
                map.put(username, new LinkedBlockingQueue<>());
                users.put(username, socket);
                out.println("Добро пожаловать, " + username + "!");
                out.flush();
                logger.log(infoLevel, username + ": вход в чат");

                executor.execute(() -> {
                    try {
                        while (!socket.isClosed()) {
                            if (!map.get(username).isEmpty()) {
                                    Message msg = map.get(username).take();

                                    synchronized (out) {
                                        out.println(String.format(
                                            "%s (%s): %s", msg.getUsername(),
                                            msg.getCreatedAt().format(formatter),
                                            msg.getText()
                                        ));
                                        out.flush();
                                        logger.log(infoLevel, username + ": прием сообщения");
                                    }
                            }
                        }
                    } catch (InterruptedException e) {
                        logger.log(warningLevel, username + ": ошибка приема сообщения");
                    } finally {
                        out.close();
                        logger.log(infoLevel, username + ": закрыт поток вывода");
                    }
                });

                executor.execute(() -> {
                    try {
                        while (true) {
                            String text;
                            synchronized (in) {
                                text = in.readLine();
                            }

                            if (text == null) {
                                logger.log(infoLevel, username + ": выход из чата");
                                socket.close();
                                break;
                            }

                            Message message = new Message(username, text);
                            for (Map.Entry<String, BlockingQueue<Message>> entry : map.entrySet()) {
                                entry.getValue().put(message);
                            }
                            logger.log(infoLevel, username + ": отправка сообщения");
                        }
                    } catch (Exception e) {
                        logger.log(warningLevel, username + ": ошибка при отправке");
                    } finally {
                        try {
                            logger.log(infoLevel, username + ": закрытие потока ввода");
                            in.close();
                        } catch (IOException e) {
                            logger.log(warningLevel, username + ": ошибка закрытия ввода");
                        }
                    }
                });
            }
        } catch (IOException e) {
            logger.log(
                severeLevel, "Не удалось запустить сервер: " + e.getMessage()
            );
        } finally {
            executor.shutdown();
            logger.log(infoLevel, "Завершение работы вспомогательных потоков");

            for (Map.Entry<String, Socket> entry : users.entrySet()) {
                try {
                    logger.log(infoLevel, entry.getKey() + ": закрытие сокета");
                    entry.getValue().close();
                } catch (IOException e) {
                    logger.log(warningLevel, entry.getKey() + ": ошибка закрытия сокета");
                }
            }
        }
    }

    private static Properties getProperties(String propertiesFilename) {
        Properties props = new Properties();
        try (InputStream is = Server.class.getClassLoader()
                .getResourceAsStream(propertiesFilename)) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        return props;
    }
}
