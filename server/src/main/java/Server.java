import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public static final Map<String, Socket> sockets = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        Properties props = getProperties("conf.properties");
        int port = Integer.parseInt(props.getProperty("server.port"));

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            Thread acceptThread = new Thread(() -> {
                ExecutorService executor = Executors.newCachedThreadPool();

                while (true) {
                    try {
                        Socket acceptedSocket = serverSocket.accept();
                        executor.submit(new ClientHandler(acceptedSocket));
                    } catch (IOException e) {
                        if (serverSocket.isClosed()) {
                            break;
                        }
                    }
                }
                executor.shutdown();

            });
            acceptThread.start();
            acceptThread.join();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
            e.printStackTrace();
        }
        return props;
    }
}
