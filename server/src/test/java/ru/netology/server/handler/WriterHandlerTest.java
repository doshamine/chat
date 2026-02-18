package ru.netology.server.handler;

import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import ru.netology.common.message.Message;
import ru.netology.server.BaseTest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class WriterHandlerTest extends BaseTest {
    private final BlockingQueue<Message> messageQueue = new LinkedBlockingDeque<>();
    PrintStream err = System.err;
    ByteArrayOutputStream errContent = new ByteArrayOutputStream();

    @BeforeEach
    public void setUp() {
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    void tearDown() {
        messageQueue.clear();
        System.setErr(err);
        errContent.reset();
    }

    @Test
    @DisplayName("Обработка исключения IOException")
    public void givenIOException_whenRun_thenPrintMessage() {
        try (Socket socketMock = Mockito.mock(Socket.class)) {
            Mockito.when(socketMock.getOutputStream()).thenThrow(IOException.class);

            Thread writerThread = new Thread(new WriterHandler(socketMock, messageQueue));
            writerThread.start();
            writerThread.join();

            Assertions.assertTrue(errContent.toString().contains("поток"));
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    @Test
    @DisplayName("Обработка исключения InterruptedException")
    public void givenInterruptedException_whenRun_thenPrintMessage() {
        try (Socket socketMock = Mockito.mock(Socket.class)) {
            Mockito.when(socketMock.getOutputStream()).thenReturn(new ByteArrayOutputStream());

            BlockingQueue<Message> queueSpy = Mockito.spy(messageQueue);
            Mockito.doReturn(false).when(queueSpy).isEmpty();
            Mockito.doThrow(InterruptedException.class).when(queueSpy).take();
            Thread writerThread = new Thread(new WriterHandler(socketMock, queueSpy));
            writerThread.start();
            writerThread.join();

            Assertions.assertTrue(errContent.toString().contains("Прерван"));
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    @Test
    @DisplayName("Отправка сообщения из очереди")
    public void givenMessage_whenRun_thenSendMessage() {
        String username = "username";
        String text = "message";
        Message message = new Message(username, text);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (Socket socketMock = Mockito.mock(Socket.class)) {
            Mockito.when(socketMock.isClosed()).thenReturn(false, true);
            Mockito.when(socketMock.getOutputStream()).thenReturn(outputStream);
            messageQueue.put(message);

            Thread writerThread = new Thread(new WriterHandler(socketMock, messageQueue));
            writerThread.start();
            writerThread.join();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        Assertions.assertTrue(outputStream.toString().contains(text));
    }
}
