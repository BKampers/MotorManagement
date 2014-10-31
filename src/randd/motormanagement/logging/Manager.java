/*
** Copyright © Bart Kampers
*/
package randd.motormanagement.logging;

import java.util.logging.*;


public class Manager {

    static public void setup() throws java.io.IOException {
        Logger logger = Logger.getLogger("randd");

        Logger rootLogger = Logger.getLogger("");
        for (Handler handler : rootLogger.getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                handler.setFormatter(new ConsoleFormatter());
            }
        }
        
        logger.setLevel(Level.INFO);
        fileHandler = new FileHandler("Logging.log");
        formatter = new SimpleFormatter();
        fileHandler.setFormatter(formatter);
        logger.addHandler(fileHandler);
    }

    
    static class ConsoleFormatter extends Formatter {

        @Override
        public String format(LogRecord record) {
            StringBuilder builder = new StringBuilder();
            builder.append(Long.toHexString(record.getMillis()));
            builder.append(formatMessage(record));
            builder.append('\n');
            return builder.toString();
        }
    }
    

    static private FileHandler fileHandler;
    static private SimpleFormatter formatter;

}
