package ru.netology.client.handler;

import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import ru.netology.client.BaseTest;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class ReaderHandlerTest extends BaseTest {
    private final BlockingQueue<String> queue =  new LinkedBlockingQueue<>();
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    @BeforeEach
    public void setup() {
        System.setErr(new PrintStream(output));
    }

    @AfterEach
    public void setUp() {
        queue.clear();
    }

    @Test
    @DisplayName("Вывод сообщения об ошибке в случае некорректного таймаута сокета")
    public void givenIncorrectTimeout_whenRun_thenLogSocketException() {
        try (Socket socketMock = Mockito.mock(Socket.class)) {
            Mockito.doThrow(SocketException.class).when(socketMock).setSoTimeout(Mockito.anyInt());
            ByteArrayInputStream inputMock = new ByteArrayInputStream("".getBytes());
            Mockito.when(socketMock.getInputStream()).thenReturn(inputMock);

            Thread readerThread = new Thread(new ReaderHandler(socketMock, queue));
            readerThread.start();
            readerThread.join();

            Assertions.assertTrue(
                output.toString().contains("таймаут"),
                "Сообщение должно содержать пояснение"
            );
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    @Test
    @DisplayName("Вывод сообщения об ошибке в случае закрытого потока ввода")
    public void givenClosedInputStream_whenRun_thenLogInputError() {
        try (Socket socketMock = Mockito.mock(Socket.class)) {
            Mockito.when(socketMock.getInputStream()).thenThrow(IOException.class);

            Thread readerThread = new Thread(new ReaderHandler(socketMock, queue));
            readerThread.start();
            readerThread.join();

            Assertions.assertTrue(
                output.toString().contains("поток"),
                "Сообщение должно содержать пояснение"
            );
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    @Test
    @DisplayName("Входящие сообщения помещаются в очередь")
    public void givenMessage_whenRun_thenPutMessage() {
        try (Socket socketMock = Mockito.mock(Socket.class)) {
            String message = "message";
            ByteArrayInputStream inputMock = new ByteArrayInputStream(message.getBytes());
            Mockito.when(socketMock.getInputStream()).thenReturn(inputMock);

            Thread readerThread = new Thread(new ReaderHandler(socketMock, queue));
            readerThread.start();
            readerThread.join();

            Assertions.assertFalse(queue.isEmpty());
            Assertions.assertEquals(message, queue.take());
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    @Test
    @DisplayName("Ошибка в случае InterruptedException в очереди сообщений")
    public void givenInterruptedException_whenRun_thenLogError() {
        try (Socket socketMock = Mockito.mock(Socket.class)) {
            ByteArrayInputStream inputMock = new ByteArrayInputStream("message".getBytes());
            Mockito.when(socketMock.getInputStream()).thenReturn(inputMock);
            BlockingQueue<String> queueSpy = Mockito.spy(queue);
            Mockito.doThrow(InterruptedException.class).when(queueSpy).put(Mockito.anyString());

            Thread readerThread = new Thread(new ReaderHandler(socketMock, queueSpy));
            readerThread.start();
            readerThread.join();

            Assertions.assertTrue(
                output.toString().contains("прервано"),
                "Сообщение должно содержать пояснение"
            );
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
