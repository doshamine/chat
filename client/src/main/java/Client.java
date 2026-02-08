import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        Properties props = getProperties("conf.properties");
        String host  = props.getProperty("server.host");
        String port = props.getProperty("server.port");
        Scanner sc = new Scanner(System.in);

        try (
            Socket socket = new Socket(host, Integer.parseInt(port));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        ) {
            System.out.print(in.readLine());
            out.println(sc.nextLine());
            out.flush();
            System.out.println(in.readLine());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Properties getProperties(String propertiesFilename) {
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
