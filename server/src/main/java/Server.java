import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

public class Server {
    private static final Logger logger = Logger.getLogger(Server.class.getName());
    private static final String propertiesFilename = "conf.properties";

    private final Map<String, BlockingQueue<Message>> queuesMap = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        configureLogger();
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

    private static void configureLogger() {
        String logFilename = getProperty("log.filename");

        try {
            FileHandler fileHandler = new FileHandler(logFilename, true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            System.err.println("Ошибка при открытии файла: " + e.getMessage());
        }
    }

    private static class ClientHandler implements Runnable {
        final Socket socket;
        final Map<String, BlockingQueue<Message>> queuesMap;

        public ClientHandler(Socket socket, Map<String, BlockingQueue<Message>> queuesMap) {
            this.socket = socket;
            this.queuesMap = queuesMap;
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
                out.println("Добро пожаловать!");
                out.flush();
                logger.info(username + ": вход в чат");

                Thread writerThread = new Thread(new ServerWriter(socket, username, queuesMap.get(username)));
                Thread readerThread = new Thread(new ServerReader(socket, username, queuesMap));

                writerThread.start();
                readerThread.start();

                writerThread.join();
                readerThread.join();
                logger.info(username + ": выход из чата");

                socket.close();
                queuesMap.remove(username);
            } catch (IOException e) {
                logger.severe("Ошибка работы потоков ввода/вывода: " + e.getMessage());
            } catch (InterruptedException e) {
                logger.warning("Прерывание работы чтения записи: " + e.getMessage());
            }
        }
    }

    private static class ServerReader implements Runnable {
        final Socket socket;
        final String username;
        final Map<String, BlockingQueue<Message>> queuesMap;

        public ServerReader(Socket socket, String username, Map<String, BlockingQueue<Message>> queuesMap) {
            this.socket = socket;
            this.username = username;
            this.queuesMap = queuesMap;
        }

        @Override
        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
            ) {
                while (true) {
                    String text;
                    text = in.readLine();

                    if (text == null) {
                        break;
                    }

                    Message message = new Message(username, text);
                    for (Map.Entry<String, BlockingQueue<Message>> entry : queuesMap.entrySet()) {
                        entry.getValue().put(message);
                    }
                    logger.info(username + ": отправка сообщения");
                }
            } catch (IOException e) {
                logger.severe(username + ": ошибка открытия потока ввода");
            } catch (InterruptedException e) {
                logger.severe(username + ": прервана работа с очередью сообщений");
            }
        }
    }

    private static class ServerWriter implements Runnable {
        final Socket socket;
        final String username;
        final BlockingQueue<Message> messageQueue;

        public ServerWriter(Socket socket, String username, BlockingQueue<Message> messageQueue) {
            this.socket = socket;
            this.username = username;
            this.messageQueue = messageQueue;
        }

        @Override
        public void run() {
            String timeFormat = getProperty("time.format");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timeFormat);

            try (
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                while (!socket.isClosed()) {
                    if (!messageQueue.isEmpty()) {
                        Message msg = messageQueue.take();

                        out.println(String.format(
                            "%s (%s): %s", msg.getUsername(),
                            msg.getCreatedAt().format(formatter),
                            msg.getText()
                        ));
                        out.flush();
                        logger.info(username + ": прием сообщения");
                    }
                }

            } catch (IOException e) {
                logger.severe(username + ": ошибка открытия потока вывода");
            } catch (InterruptedException e) {
                logger.severe(username + ": прервана работа с очередью сообщений");
            }
        }
    }
}
