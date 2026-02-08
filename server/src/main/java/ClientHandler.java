import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            out.println("Привет! Введи свой никнейм: ");
            out.flush();
            String username = in.readLine().trim();
            Server.sockets.put(username, socket);
            out.println("Добро пожаловать в чат, " + username + "!");
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
