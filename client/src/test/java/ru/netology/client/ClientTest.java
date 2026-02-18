package ru.netology.client;

import org.junit.jupiter.api.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class ClientTest {
    PrintStream err = System.err;
    ByteArrayOutputStream errContent = new ByteArrayOutputStream();

    @BeforeEach
    void setUp() {
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    void tearDown() {
        System.setErr(err);
    }

    @Test
    @DisplayName("Вывод сообщения в случае ошибки подключения к серверу")
    public void givenIOException_whenStart_thenPrintError() {
        String host = "host";
        int port = 111;

        Client client = new Client();
        client.start(host, port);

        Assertions.assertTrue(
            errContent.toString().contains("сервер"),
            "Сообщение должно содержать пояснение"
        );
    }
}
