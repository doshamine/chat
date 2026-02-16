package ru.netology.common.abs;

import java.util.logging.Logger;

public interface LoggableRunner extends Runnable {
    Logger logger = Logger.getLogger(LoggableRunner.class.getName());
}
