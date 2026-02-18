package ru.netology.client.printer;

import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import ru.netology.client.BaseTest;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MessagePrinterTest extends BaseTest {
    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
    private final PrintStream out = System.out;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    public void tearDown() {
        System.setOut(out);
        queue.clear();
    }

    @Test
    @DisplayName("Вывод сообщения из очереди")
    public void givenMessage_whenRun_thenPrint() {
        try {
            int timeout = Integer.parseInt(getProperty("print.timeout"));
            String message = "message";
            queue.put(message);

            Thread printerThread = new Thread(new MessagePrinter(queue));
            printerThread.start();
            Thread.sleep(timeout);
            printerThread.interrupt();

            Assertions.assertTrue(
                outContent.toString().contains(message),
                "Сообщение не было выведено"
            );
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
        }
    }

    @Test
    @DisplayName("Вывод нескольких сообщений из очереди")
    public void givenMessages_whenRun_thenPrint() {
        try {
            int timeout = Integer.parseInt(getProperty("print.timeout"));
            String message1 = "message1";
            String message2 = "message2";
            queue.put(message1);
            queue.put(message2);

            Thread printerThread = new Thread(new MessagePrinter(queue));
            printerThread.start();
            Thread.sleep(timeout);
            printerThread.interrupt();

            Assertions.assertTrue(
                outContent.toString().contains(message1),
                "Сообщение " + message1 + " не было выведено"
            );
            Assertions.assertTrue(
                outContent.toString().contains(message2),
                "Сообщение " + message2 + " не было выведено"
            );
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
        }
    }

    @Test
    @DisplayName("Ошибка в случае InterruptedException в очереди сообщений")
    public void givenInterruptedException_whenRun_thenPrintError() {
        try {
            BlockingQueue<String> queueSpy = Mockito.spy(queue);
            Mockito.doThrow(InterruptedException.class).when(queueSpy).take();
            Mockito.when(queueSpy.isEmpty()).thenReturn(false);
            PrintStream err = System.err;
            ByteArrayOutputStream errContent = new ByteArrayOutputStream();
            System.setErr(new PrintStream(errContent));

            Thread printerThread = new Thread(new MessagePrinter(queueSpy));
            printerThread.start();
            printerThread.join();
            System.setErr(err);

            Assertions.assertTrue(
                errContent.toString().contains("прервана"),
                "Сообщение должно содержать пояснение"
            );
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
