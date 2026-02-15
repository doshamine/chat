import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import ru.netology.client.handler.ReaderHandler;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReaderHandlerTest extends BaseTest {
    private static final String logFilename = getProperty("log.file");
    private BlockingQueue<String> queue;

    @BeforeEach
    public void setUp() {
        File file = new File(logFilename);
        try {
            FileWriter fw = new FileWriter(file);
            fw.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

        queue = new LinkedBlockingQueue<>();
    }

    @Test
    @DisplayName("Вывод сообщения об ошибке в случае некорректного таймаута сокета")
    public void givenIncorrectTimeout_whenRun_thenLogSocketException() {
        try (Socket socketMock = Mockito.mock(Socket.class)) {
            Mockito.doThrow(SocketException.class).when(socketMock).setSoTimeout(Mockito.anyInt());
            ByteArrayInputStream inputMock = new ByteArrayInputStream("".getBytes());
            Mockito.when(socketMock.getInputStream()).thenReturn(inputMock);

            ReaderHandler readerHandler = new ReaderHandler(socketMock, queue);
            readerHandler.run();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

        try (BufferedReader fileInput = new BufferedReader(new InputStreamReader(new FileInputStream(logFilename)))) {
            fileInput.readLine();
            String logContent = fileInput.readLine();

            Assertions.assertTrue(
                logContent.contains("WARNING"), "Сообщение должно иметь уровень WARNING"
            );
            Assertions.assertTrue(
                logContent.contains("таймаут"), "Сообщение должно содержать пояснение"
            );
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    @Test
    @DisplayName("Вывод сообщения об ошибке в случае закрытого потока ввода")
    public void givenClosedSocket_whenRun_thenLogInputError() {
        try (Socket socketMock = Mockito.mock(Socket.class)) {
            Mockito.when(socketMock.getInputStream()).thenThrow(IOException.class);

            ReaderHandler readerHandler = new ReaderHandler(socketMock, queue);
            readerHandler.run();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

        try (BufferedReader fileInput = new BufferedReader(new InputStreamReader(new FileInputStream(logFilename)))) {
            fileInput.readLine();
            String logContent = fileInput.readLine();

            Assertions.assertTrue(
                logContent.contains("SEVERE"), "Сообщение должно иметь уровень SEVERE"
            );
            Assertions.assertTrue(
                logContent.contains("поток"), "Сообщение должно содержать пояснение"
            );
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    @Test
    @DisplayName("Входящие сообщения помещаются в очередь")
    public void givenMessage_whenRun_thenPutMessage() {
        try (Socket socketMock = Mockito.mock(Socket.class)) {
            ByteArrayInputStream inputMock = new ByteArrayInputStream("message".getBytes());
            Mockito.when(socketMock.getInputStream()).thenReturn(inputMock);

            ReaderHandler readerHandler = new ReaderHandler(socketMock, queue);
            readerHandler.run();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

        Assertions.assertFalse(queue.isEmpty());

        try (BufferedReader fileInput = new BufferedReader(new InputStreamReader(new FileInputStream(logFilename)))) {
            fileInput.readLine();
            String logContent = fileInput.readLine();

            Assertions.assertTrue(
                logContent.contains("INFO"), "Сообщение должно иметь уровень INFO"
            );
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
