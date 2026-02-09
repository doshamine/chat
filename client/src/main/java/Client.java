import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        Properties props = Client.getProperties("conf.properties");
        String host  = props.getProperty("server.host");
        String port = props.getProperty("server.port");
        String exitCommand = props.getProperty("client.exit");
        Scanner sc = new Scanner(System.in);

        try (
            Socket socket = new Socket(host, Integer.parseInt(port));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        ) {
            System.out.print(reader.readLine());
            writer.println(sc.nextLine());
            writer.flush();
            System.out.println(reader.readLine());
            Thread receiveThread = new Thread(() -> {
                BufferedReader in = reader;
                while (true) {
                    String msg = null;
                    try {
                        msg = in.readLine();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    System.out.println(msg);
                }
            });
            receiveThread.start();

            while (true) {
                System.out.print("> ");
                String text = sc.nextLine();
                if (text.equals(exitCommand)) {
                    break;
                }

                writer.println(text);
                writer.flush();
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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
