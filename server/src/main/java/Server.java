import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Server {
    private static final List<Socket> sockets = new ArrayList<>();

    public static void main(String[] args) {
        Properties props = getProperties("conf.properties");
        String port = props.getProperty("server.port");

        try (ServerSocket serverSocket = new ServerSocket(Integer.parseInt(port))) {
            Thread acceptThread = new Thread(() -> {
                while (true) {
                    try (Socket socket = serverSocket.accept()) {
                        sockets.add(socket);
                    } catch (IOException e) {
                        if (serverSocket.isClosed()) {
                            break;
                        }
                    }
                }
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
