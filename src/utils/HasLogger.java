package utils;

import java.util.logging.Level;
import java.util.logging.Logger;

public interface HasLogger {

    /**
     * Erstellt einen Logger pro Klasse in dem die Methode aufgerufen wird und verwendet diesen danach immer wieder.
     * @return
     */
    default Logger getLogger() {
        Logger logger = Logger.getLogger(getClass().getSimpleName());
        logger.setLevel(Level.INFO);
        return logger;
    }
}
