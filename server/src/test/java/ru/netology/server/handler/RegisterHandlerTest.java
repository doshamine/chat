package ru.netology.server.handler;

import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import ru.netology.common.message.Message;
import ru.netology.server.BaseTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

public class RegisterHandlerTest extends BaseTest {
    private final Map<String, BlockingQueue<Message>> queuesMap = new ConcurrentHashMap<>();
    private final BlockingQueue<String> usernameQueue = new LinkedBlockingDeque<>();
    PrintStream err = System.err;
    ByteArrayOutputStream errContent = new ByteArrayOutputStream();

    @BeforeEach
    public void setUp() {
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    void tearDown() {
        queuesMap.clear();
        usernameQueue.clear();
        System.setErr(err);
        errContent.reset();
    }

    @Test
    @DisplayName("Обработка исключения IOException от потока ввода")
    public void givenIOExceptionFromInput_whenRun_thenPrintMessage() {
        try (Socket socketMock = Mockito.mock(Socket.class)) {
            Mockito.when(socketMock.getInputStream()).thenThrow(IOException.class);
            Mockito.when(socketMock.getOutputStream()).thenReturn(new ByteArrayOutputStream());

            Thread registerThread = new Thread(new RegisterHandler(
                socketMock, queuesMap, usernameQueue
            ));
            registerThread.start();
            registerThread.join();

            Assertions.assertTrue(errContent.toString().contains("поток"));
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    @Test
    @DisplayName("Обработка исключения IOException от потока вывода")
    public void givenIOExceptionFromOutput_whenRun_thenPrintMessage() {
        try (Socket socketMock = Mockito.mock(Socket.class)) {
            Mockito.when(socketMock.getInputStream()).thenReturn(new ByteArrayInputStream("".getBytes()));
            Mockito.when(socketMock.getOutputStream()).thenThrow(IOException.class);

            Thread registerThread = new Thread(new RegisterHandler(
                socketMock, queuesMap, usernameQueue
            ));
            registerThread.start();
            registerThread.join();

            Assertions.assertTrue(errContent.toString().contains("поток"));
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    @Test
    @DisplayName("Успешная регистрация пользователя")
    public void givenNewUsername_whenRun_thenPrintGreeting() {
        try (Socket socketMock = Mockito.mock(Socket.class)) {
            String username = "username";
            Mockito.when(socketMock.getInputStream()).thenReturn(new ByteArrayInputStream(username.getBytes()));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Mockito.when(socketMock.getOutputStream()).thenReturn(new PrintStream(outputStream));

            Thread registerThread = new Thread(new RegisterHandler(
                socketMock, queuesMap, usernameQueue
            ));
            registerThread.start();
            registerThread.join();

            String greeting = getProperty("server.greeting");
            Assertions.assertTrue(outputStream.toString().contains(greeting));
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    @Test
    @DisplayName("Регистрация одинаковых имен запрещена")
    public void givenTakenUsername_whenRun_thenPrintHint() {
        try (Socket socketMock = Mockito.mock(Socket.class)) {
            String username = "username";
            queuesMap.put(username, new LinkedBlockingDeque<>());
            String usernames = username + "\n" + username + "1";
            ByteArrayInputStream inputStream = new ByteArrayInputStream(usernames.getBytes());
            Mockito.when(socketMock.getInputStream()).thenReturn(inputStream);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Mockito.when(socketMock.getOutputStream()).thenReturn(new PrintStream(outputStream));

            Thread registerThread = new Thread(new RegisterHandler(
                socketMock, queuesMap, usernameQueue
            ));
            registerThread.start();
            registerThread.join();

            String greeting = getProperty("server.greeting");
            Assertions.assertTrue(outputStream.toString().contains(greeting));
            Assertions.assertTrue(outputStream.toString().contains("занят"));
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
