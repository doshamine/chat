package ru.netology.client.handler;

import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import ru.netology.client.BaseTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;

public class RegisterHandlerTest extends BaseTest {
    PrintStream err = System.err;
    ByteArrayOutputStream errContent = new ByteArrayOutputStream();

    @BeforeEach
    public void setUp() {
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    void tearDown() {
        System.setErr(err);
    }

    @Test
    @DisplayName("Обработка исключения IOException от потока ввода")
    public void givenIOExceptionFromInput_whenRun_thenPrintMessage() {
        try (Socket socketMock = Mockito.mock(Socket.class)) {
            Mockito.when(socketMock.getInputStream()).thenThrow(IOException.class);
            Mockito.when(socketMock.getOutputStream()).thenReturn(new ByteArrayOutputStream());

            Thread registerThread = new Thread(new RegisterHandler(socketMock));
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

            Thread registerThread = new Thread(new RegisterHandler(socketMock));
            registerThread.start();
            registerThread.join();

            Assertions.assertTrue(errContent.toString().contains("поток"));
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    @Test
    @DisplayName("При успешном входе в чат выводится приветствие")
    public void givenGreeting_whenRun_thenPrintGreeting() {
        try (Socket socketMock = Mockito.mock(Socket.class)) {
            Mockito.doNothing().when(socketMock).notify();
            Mockito.doNothing().when(socketMock).wait();
            String greeting = getProperty("server.greeting");
            Mockito.when(socketMock.getInputStream()).thenReturn(new ByteArrayInputStream(greeting.getBytes()));
            Mockito.when(socketMock.getOutputStream()).thenReturn(new ByteArrayOutputStream());
            PrintStream out = System.out;
            ByteArrayOutputStream outContent = new ByteArrayOutputStream();
            System.setOut(new PrintStream(outContent));

            Thread registerThread = new Thread(new RegisterHandler(socketMock));
            registerThread.start();
            registerThread.join();
            System.setOut(out);

            Assertions.assertTrue(outContent.toString().contains(greeting));
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    @Test
    @DisplayName("Обработка исключения InterruptedException от сокета")
    public void givenInterruptedException_whenRun_thenPrintMessage() {
        try (Socket socketMock = Mockito.mock(Socket.class)) {
            Mockito.doThrow(InterruptedException.class).when(socketMock).wait();
            String greeting = getProperty("server.greeting");
            Mockito.when(socketMock.getInputStream()).thenReturn(new ByteArrayInputStream(greeting.getBytes()));
            Mockito.when(socketMock.getOutputStream()).thenReturn(new ByteArrayOutputStream());

            Thread registerThread = new Thread(new RegisterHandler(socketMock));
            registerThread.start();
            registerThread.join();

            Assertions.assertTrue(errContent.toString().contains("прерван"));
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
