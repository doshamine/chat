import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    public static final Map<String, BlockingQueue<Message>> map = new ConcurrentHashMap<>();
    public static Set<Socket> sockets = ConcurrentHashMap.newKeySet();
    public static ExecutorService executor = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        Properties props = getProperties("conf.properties");
        int port = Integer.parseInt(props.getProperty("server.port"));
        String timeFormat = props.getProperty("time.format");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timeFormat);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                sockets.add(socket);

                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                out.println("Привет! Введи никнейм: ");
                out.flush();
                String username = in.readLine();
                map.put(username, new LinkedBlockingQueue<>());
                out.println("Добро пожаловать, " + username + "!");
                out.flush();

                executor.execute(() -> {
                    try {
                        while (!socket.isClosed()) {
                            if (!map.get(username).isEmpty()) {
                                    Message msg = map.get(username).take();
                                    out.println(String.format(
                                        "%s (%s): %s", msg.getUsername(),
                                        msg.getCreatedAt().format(formatter),
                                        msg.getText()
                                    ));
                                    out.flush();
                            }
                        }
                    } catch (InterruptedException e) {
                        System.err.println(e.getMessage());
                    } finally {
                        out.close();
                    }
                });

                executor.execute(() -> {
                    try {
                        while (true) {
                            String text = in.readLine();
                            if (text == null) {
                                socket.close();
                                break;
                            }
                            Message message = new Message(username, text);
                            for (Map.Entry<String, BlockingQueue<Message>> entry : map.entrySet()) {
                                entry.getValue().put(message);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                    } finally {
                        try {
                            in.close();
                        } catch (IOException e) {
                            System.err.println(e.getMessage());
                        }
                    }
                });
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } finally {
            executor.shutdown();

            for (Socket socket : sockets) {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.err.println(e.getMessage());
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
