package ru.netology.server.handler;

import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import ru.netology.common.message.Message;
import ru.netology.server.BaseTest;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

public class ReaderHandlerTest extends BaseTest {
    private final String username = "username";
    private final ConcurrentHashMap<String, BlockingQueue<Message>> queuesMap = new ConcurrentHashMap<>();
    PrintStream err = System.err;
    ByteArrayOutputStream errContent = new ByteArrayOutputStream();

    @BeforeEach
    public void setUp() {
        queuesMap.put(username, new LinkedBlockingDeque<>());
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    void tearDown() {
        queuesMap.clear();
        System.setErr(err);
        errContent.reset();
    }

    @Test
    @DisplayName("Обработка исключения IOException")
    public void givenIOException_whenRun_thenPrintError() {
        try (Socket socketMock = Mockito.mock(Socket.class)) {
            Mockito.when(socketMock.getInputStream()).thenThrow(IOException.class);

            Thread readerThread = new Thread(new ReaderHandler(
                socketMock, username, queuesMap
            ));
            readerThread.start();
            readerThread.join();

            Assertions.assertTrue(errContent.toString().contains(username));
            Assertions.assertTrue(errContent.toString().contains("поток"));
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    @Test
    @DisplayName("Обработка исключения InterruptedException")
    public void givenInterruptedException_whenRun_thenPrintError() {
        try (Socket socketMock = Mockito.mock(Socket.class)) {
            String message = "message";
            ByteArrayInputStream inputMock = new ByteArrayInputStream(message.getBytes());
            Mockito.when(socketMock.getInputStream()).thenReturn(inputMock);

            BlockingQueue<Message> queueSpy = Mockito.spy(queuesMap.get(username));
            Mockito.doThrow(InterruptedException.class).when(queueSpy).put(Mockito.any(Message.class));
            queuesMap.put(username, queueSpy);

            Thread readerThread = new Thread(new ReaderHandler(
                socketMock, username, queuesMap
            ));
            readerThread.start();
            readerThread.join();

            Assertions.assertTrue(errContent.toString().contains(username));
            Assertions.assertTrue(errContent.toString().contains("прерван"));
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    @Test
    @DisplayName("Сообщение пользователя кладется в очередь")
    public void givenMessage_whenRun_thenPut() {
        try (Socket socketMock = Mockito.mock(Socket.class)) {
            String username2 = "username2";
            queuesMap.put(username2, new LinkedBlockingDeque<>());
            String message = "message";
            Message expectedMessage = new Message(username, message);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(message.getBytes());
            Mockito.when(socketMock.getInputStream()).thenReturn(inputStream);

            Thread readerThread = new Thread(new ReaderHandler(
                socketMock, username, queuesMap
            ));
            readerThread.start();
            readerThread.join();

            Assertions.assertFalse(queuesMap.get(username).isEmpty());
            Assertions.assertFalse(queuesMap.get(username2).isEmpty());
            Assertions.assertEquals(queuesMap.get(username).take().getText(), expectedMessage.getText());
            Assertions.assertEquals(queuesMap.get(username2).take().getUsername(), expectedMessage.getUsername());
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
