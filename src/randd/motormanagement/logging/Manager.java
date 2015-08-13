/*
** Copyright © Bart Kampers
*/

package randd.motormanagement.logging;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;


public class Manager {
    

    public static void setup(Map<Level, Collection<String>> levelMap) throws java.io.IOException {
        loggers.clear();
        for (Map.Entry<Level, Collection<String>> entry : levelMap.entrySet()) {
            Level level = entry.getKey();
            java.util.Collection<String> paths = entry.getValue();
            if (paths != null) {
                for (String path : paths) {
                    Logger logger = Logger.getLogger(path);
                    logger.setLevel(level);
                    loggers.add(logger);
                }
            }
        }
        
        Logger rootLogger = Logger.getLogger("");
        for (Handler handler : rootLogger.getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                handler.setFormatter(FORMATTER);
                handler.setLevel(Level.ALL);
            }
        }
        
        Logger logger = Logger.getLogger("randd");
        fileHandler = new FileHandler("MotorManagement.log");
        fileHandler.setFormatter(FORMATTER);
        logger.addHandler(fileHandler);
    }
    
    
    static class DefaultFormatter extends Formatter {

        @Override
        public String format(LogRecord record) {
            StringBuilder builder = new StringBuilder();
            builder.append(DATE_FORMAT.format(new java.util.Date(record.getMillis())));
            builder.append(" [");
            builder.append(record.getLevel());
            builder.append("] {");
            builder.append(String.format(record.getLoggerName(), "%-8s"));
            builder.append("} ");
            builder.append(formatMessage(record));
            if (record.getThrown() != null) {
                builder.append(" thrown ");
                builder.append(record.getThrown().getMessage());
            }
            builder.append('\n');
            return builder.toString();
        }
    }

    private static final Collection<Logger> loggers = new HashSet<>();

    private static FileHandler fileHandler;
    
    private static final Formatter FORMATTER = new DefaultFormatter();
    private static final java.text.SimpleDateFormat DATE_FORMAT = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

}
