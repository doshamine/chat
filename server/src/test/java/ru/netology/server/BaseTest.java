package ru.netology.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public abstract class BaseTest {
    private static final String propertiesFilename = "conf.properties";

    protected static String getProperty(String property) {
        Properties props = new Properties();
        try (InputStream is = BaseTest.class.getClassLoader()
            .getResourceAsStream(propertiesFilename)) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        return props.getProperty(property);
    }
}
