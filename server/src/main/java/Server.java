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
    private static Map<String, BlockingQueue<Message>> map = new ConcurrentHashMap<>();
    private static Map<String, Socket> users = new ConcurrentHashMap<>();
    private static Logger logger = Logger.getLogger(Server.class.getName());
    private static FileHandler fileHandler;

    public static void main(String[] args) {
        ExecutorService executor = Executors.newCachedThreadPool();
        Properties props = getProperties("conf.properties");
        configureLogger();

        int port = Integer.parseInt(props.getProperty("server.port"));
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            logger.info("Сервер запущен");

            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                logger.info("Новое подключение");
                executor.execute(new ClientHandler(socket));
            }
        } catch (IOException e) {
            logger.severe("Не удалось запустить сервер: " + e.getMessage());
        } finally {
            executor.shutdown();
            logger.info("Завершение работы вспомогательных потоков");
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
    }

    private static class ClientHandler implements Runnable {
        final Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
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
                while (map.containsKey(username)) {
                    out.println("Никнейм уже занят! Введи другой: ");
                    out.flush();
                    username = in.readLine();
                }
                map.put(username, new LinkedBlockingQueue<>());
                users.put(username, socket);
                out.println("Добро пожаловать!");
                out.flush();
                logger.info(username + ": вход в чат");

                Thread writerThread = new Thread(new ServerWriter(socket, username));
                Thread readerThread = new Thread(new ServerReader(socket, username));

                writerThread.start();
                readerThread.start();

                writerThread.join();
                readerThread.join();
                logger.info(username + ": выход из чата");

                users.get(username).close();
                map.remove(username);
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

        public ServerReader(Socket socket, String username) {
            this.socket = socket;
            this.username = username;
        }

        @Override
        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            ) {
                while (true) {
                    String text;
                    text = in.readLine();

                    if (text == null) {
                        break;
                    }

                    Message message = new Message(username, text);
                    for (Map.Entry<String, BlockingQueue<Message>> entry : map.entrySet()) {
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

        public ServerWriter(Socket socket, String username) {
            this.socket = socket;
            this.username = username;
        }

        @Override
        public void run() {
            Properties props = getProperties("conf.properties");
            String timeFormat = props.getProperty("time.format");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timeFormat);

            try (
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {

                while (!socket.isClosed()) {
                    if (!map.get(username).isEmpty()) {
                        Message msg = map.get(username).take();

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
