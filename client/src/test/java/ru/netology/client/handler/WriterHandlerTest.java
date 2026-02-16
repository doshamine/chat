package ru.netology.client.handler;

import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import ru.netology.client.BaseTest;

import java.io.*;
import java.net.Socket;

public class WriterHandlerTest extends BaseTest {

    @Test
    @DisplayName("Вывод сообщения об ошибке в случае закрытого потока вывода")
    public void givenClosedOutputStream_whenRun_thenLogInputError() {
        try (Socket socketMock = Mockito.mock(Socket.class)) {
            Mockito.when(socketMock.getOutputStream()).thenThrow(IOException.class);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            System.setErr(new PrintStream(output));

            Thread writerThread = new Thread(new WriterHandler(socketMock));
            writerThread.start();
            writerThread.join();

            Assertions.assertTrue(
                output.toString().contains("поток"),
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
            ByteArrayOutputStream outputMock = new ByteArrayOutputStream();
            Mockito.when(socketMock.getOutputStream()).thenReturn(outputMock);
            String exitCommand = BaseTest.getProperty("client.exit");
            System.setIn(new ByteArrayInputStream(exitCommand.getBytes()));
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            System.setOut(new PrintStream(output));

            Thread writerThread = new Thread(new WriterHandler(socketMock));
            writerThread.start();
            writerThread.join();

            Assertions.assertTrue(
                output.toString().contains("Выход"),
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
            System.setIn(new ByteArrayInputStream(message.getBytes()));

            Thread writerThread = new Thread(new WriterHandler(socketMock));
            writerThread.start();
            writerThread.join();

            Assertions.assertEquals(
                message, outputMock.toString().trim(),
                "Сообщение не отправлено на сервер"
            );
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
