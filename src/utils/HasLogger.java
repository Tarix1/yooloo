package utils;

import java.util.logging.Level;
import java.util.logging.Logger;

public interface HasLogger {
    default Logger getLogger() {
        Logger logger = Logger.getLogger(getClass().getSimpleName());
        logger.setLevel(Level.INFO);
        return logger;
    }

}
