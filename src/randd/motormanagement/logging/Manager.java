/*
** Copyright © Bart Kampers
*/
package randd.motormanagement.logging;

import java.util.logging.*;


public class Manager {
    

    public static void setup(String[] paths) throws java.io.IOException {
        filterPaths = paths;
        
        Logger rootLogger = Logger.getLogger("");
        for (Handler handler : rootLogger.getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                handler.setFilter(LOG_FILTER);
                handler.setFormatter(FORMATTER);
            }
        }
        
        Logger logger = Logger.getLogger("randd");
        logger.setLevel(Level.INFO);
        fileHandler = new FileHandler("Logging.log");
        fileHandler.setFormatter(FORMATTER);
        logger.addHandler(fileHandler);
    }

    
    static class DefaultFormatter extends Formatter {

        @Override
        public String format(LogRecord record) {
            StringBuilder builder = new StringBuilder();
            builder.append(DATE_FORMAT.format(new java.util.Date(record.getMillis())));
            builder.append(": ");
            builder.append(formatMessage(record));
            builder.append('\n');
            return builder.toString();
        }
    }
    

    private static class LogFilter implements Filter {

        @Override
        public boolean isLoggable(LogRecord record) {
            if (filterPaths != null) {
                for (String path : filterPaths) {
                    if (record.getLoggerName().startsWith(path)) {
                        return true;
                    }
                }
            }
            return false;
        }

        
    }
    
    
    private static String[] filterPaths;
    private static FileHandler fileHandler;
    
    private static final Formatter FORMATTER = new DefaultFormatter();
    private static final Filter LOG_FILTER = new LogFilter();
    private static final java.text.SimpleDateFormat DATE_FORMAT = new java.text.SimpleDateFormat("yyyyMMdd HHmmss SSS");

}
