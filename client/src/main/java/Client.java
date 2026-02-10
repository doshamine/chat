import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Client {
    public static BlockingQueue<String> queue = new LinkedBlockingQueue<>();

    public static void main(String[] args) {
        Properties props = Client.getProperties("conf.properties");
        String host  = props.getProperty("server.host");
        String port = props.getProperty("server.port");
        String exitCommand = props.getProperty("client.exit");
        int socketTimeout = Integer.parseInt(props.getProperty("socket.timeout"));
        Scanner sc = new Scanner(System.in);

        try (
            Socket socket = new Socket(host, Integer.parseInt(port));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        ) {
            socket.setSoTimeout(socketTimeout);
            System.out.print(in.readLine());
            out.println(sc.nextLine());
            out.flush();
            System.out.println(in.readLine());

            Thread receiveThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        String msg = in.readLine();
                        queue.put(msg);
                    } catch (IOException e) {
                        if (!(e instanceof SocketTimeoutException)) {
                            System.err.println(e.getMessage());
                        }
                    } catch (InterruptedException e) {
                        System.err.println(e.getMessage());
                    }
                }
            });
            receiveThread.start();

            while (true) {
                System.out.print("> ");
                String text = sc.nextLine();
                if (text.equals(exitCommand)) {
                    receiveThread.interrupt();
                    break;
                }

                out.println(text);
                out.flush();

                while (!queue.isEmpty()) {
                    System.out.println(queue.take());
                }
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
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
