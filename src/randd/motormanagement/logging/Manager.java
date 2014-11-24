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
                handler.setFilter(logFilter);
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
            builder.append(": ");
            builder.append(formatMessage(record));
            builder.append('\n');
            return builder.toString();
        }
    }
    

    private  static class LogFilter implements Filter {

        @Override
        public boolean isLoggable(LogRecord record) {
            return record.getLoggerName().startsWith(randd.motormanagement.swing.MeasurementPanel.class.getName());
        }

        
    }
    
    
    private static final Filter logFilter = new LogFilter();
    
    private static FileHandler fileHandler;
    private static SimpleFormatter formatter;

}
