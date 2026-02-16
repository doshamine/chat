package ru.netology.client.handler;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.netology.client.BaseTest;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.Socket;

public class ServerHandlerTest extends BaseTest {
    @Test
    @DisplayName("Вывод сообщения при прерывании работы")
    public void givenInterruptedException_whenRun_thenPrintError() {
        try (Socket socketMock = Mockito.mock(Socket.class)) {
            RegistrationHandler registrationHandlerMock = Mockito.mock(RegistrationHandler.class);
            Mockito.doThrow(InterruptedException.class).when(registrationHandlerMock).run();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            System.setErr(new PrintStream(output));

            Thread handlerThread = new Thread(new ServerHandler(socketMock));
            handlerThread.start();
            handlerThread.join();

            Assertions.assertTrue(
                output.toString().contains("прервана"),
                "Сообщение должно содержать пояснение"
            );
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
