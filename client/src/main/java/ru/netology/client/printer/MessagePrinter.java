package ru.netology.client.printer;

import ru.netology.common.abs.LoggableRunner;

import java.util.concurrent.BlockingQueue;

import static ru.netology.client.Client.getProperty;

public class MessagePrinter implements LoggableRunner {
    private final BlockingQueue<String> messageQueue;

    public MessagePrinter(BlockingQueue<String> messageQueue) {
        this.messageQueue = messageQueue;
    }

    @Override
    public void run() {

        while (!Thread.currentThread().isInterrupted()) {
            if (!messageQueue.isEmpty()) {
                System.out.println("\n---new messages---");
                try {
                    while (!messageQueue.isEmpty()) {
                        System.out.println(messageQueue.take());
                    }
                } catch (InterruptedException e) {
                    String errMessage = "Работа с очередью сообщений прервана: " + e.getMessage();
                    logger.warning(errMessage);
                    System.err.println(errMessage);
                    break;
                }
                System.out.println("------");
            }
            try {
                Thread.sleep(Integer.parseInt(getProperty("print.timeout")));
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
