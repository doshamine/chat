package ru.netology.client.handler;

import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import ru.netology.client.BaseTest;

import java.io.*;
import java.net.Socket;

public class WriterHandlerTest extends BaseTest {
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream err = System.err;

    @BeforeEach
    public void setUp() {
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    public void tearDown() {
        System.setErr(err);
    }

    @Test
    @DisplayName("Вывод сообщения об ошибке в случае закрытого потока вывода")
    public void givenClosedOutputStream_whenRun_thenPrintError() {
        try (Socket socketMock = Mockito.mock(Socket.class)) {
            Mockito.when(socketMock.getOutputStream()).thenThrow(IOException.class);

            Thread writerThread = new Thread(new WriterHandler(socketMock));
            writerThread.start();
            writerThread.join();

            Assertions.assertTrue(
                errContent.toString().contains("поток"),
                "Сообщение должно содержать пояснение"
            );
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    @Test
    @DisplayName("Выход из чата по команде")
    public void givenExitCommand_whenRun_thenExit() {
        try (Socket socketMock = Mockito.mock(Socket.class)) {
            Mockito.when(socketMock.getOutputStream()).thenReturn(new ByteArrayOutputStream());

            String exitCommand = BaseTest.getProperty("client.exit");
            InputStream in = System.in;
            System.setIn(new ByteArrayInputStream(exitCommand.getBytes()));

            PrintStream out = System.out;
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            System.setOut(new PrintStream(outputStream));

            Thread writerThread = new Thread(new WriterHandler(socketMock));
            writerThread.start();
            writerThread.join();

            System.setOut(out);
            System.setIn(in);

            Assertions.assertTrue(
                outputStream.toString().contains("Выход"),
                "Должно быть уведомление о выходе"
            );
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    @Test
    @DisplayName("Отправка сообщения происходит корректно")
    public void givenMessage_whenRun_thenSendMessage() {
        try (Socket socketMock = Mockito.mock(Socket.class)) {
            ByteArrayOutputStream outputMock = new ByteArrayOutputStream();
            Mockito.when(socketMock.getOutputStream()).thenReturn(outputMock);

            String message = "message";
            InputStream in = System.in;
            System.setIn(new ByteArrayInputStream(message.getBytes()));

            Thread writerThread = new Thread(new WriterHandler(socketMock));
            writerThread.start();
            writerThread.join();

            System.setIn(in);

            Assertions.assertEquals(
                message, outputMock.toString().trim(),
                "Сообщение не отправлено на сервер"
            );
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
