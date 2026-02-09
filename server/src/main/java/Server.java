import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    public static final BlockingQueue<Message> queue = new LinkedBlockingQueue<>();

    public static void main(String[] args) throws IOException, InterruptedException {
        Properties props = getProperties("conf.properties");
        int port = Integer.parseInt(props.getProperty("server.port"));
        ServerSocket serverSocket = new ServerSocket(port);

        Thread acceptThread = new Thread(() -> {
            while (!serverSocket.isClosed()) {
                try {
                    Socket socket = serverSocket.accept();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

                    writer.println("Привет! Введи никнейм: ");
                    writer.flush();
                    String name = reader.readLine();
                    writer.println("Добро пожаловать, " + name + "!");
                    writer.flush();

                    Thread sendThread = new Thread(() -> {
                        Socket s = socket;
                        PrintWriter out = writer;

                        while (!s.isClosed()) {
                            if (!Server.queue.isEmpty()) {
                                Message msg = null;
                                try {
                                    msg = Server.queue.take();
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }

                                out.println(String.format(
                                    "%s (%s): %s", msg.getUsername(),
                                    msg.getCreatedAt(), msg.getText()
                                ));
                                out.flush();
                            }
                        }
                    });
                    sendThread.start();

                    Thread receiveThread = new Thread(() -> {
                        Socket s = socket;
                        BufferedReader in = reader;

                        while (true) {
                            String text = null;
                            try {
                                text = in.readLine();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            if (text == null) {
                                try {
                                    s.close();
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                                break;
                            }
                            Message message = new Message(name, text);
                            try {
                                Server.queue.put(message);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
                    receiveThread.start();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        acceptThread.start();
        acceptThread.join();
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
