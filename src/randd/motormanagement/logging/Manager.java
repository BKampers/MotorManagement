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
                handler.setFilter(logFilter);
                handler.setFormatter(new ConsoleFormatter());
            }
        }
        
        Logger logger = Logger.getLogger("randd");
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
    
    private static final Filter logFilter = new LogFilter();
    
    private static FileHandler fileHandler;
    private static SimpleFormatter formatter;

}
