/*
** Copyright © Bart Kampers
*/
package randd.motormanagement.logging;

import java.util.logging.*;


public class Manager {

    static public void setup() throws java.io.IOException {

        // get the global logger to configure it
        Logger logger = Logger.getLogger("randd");

        // suppress the logging output to the console
        Logger rootLogger = Logger.getLogger("");
        for (Handler handler : rootLogger.getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                handler.setFormatter(new ConsoleFormatter());
            }
        }
        
        logger.setLevel(Level.INFO);
        fileTxt = new FileHandler("Logging.txt");
//        consoleHandler = new ConsoleHandler();
//        consoleHandler.setFormatter(new MyFormatter());
//        logger.addHandler(consoleHandler);
//        fileHTML = new FileHandler("Logging.html");

        // create a TXT formatter
        formatterTxt = new SimpleFormatter();
        fileTxt.setFormatter(formatterTxt);
        logger.addHandler(fileTxt);
        

//        MyFormatter formatter = new MyFormatter();
//        consoleHandler.setFormatter(formatter);
//        logger.addHandler(consoleHandler);
        // create an HTML formatter
        //      formatterHTML = new MyHtmlFormatter();
        //      fileHTML.setFormatter(formatterHTML);
        //      logger.addHandler(fileHTML);
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
    

    static private FileHandler fileTxt;
    static private SimpleFormatter formatterTxt;

//    static private FileHandler fileHTML;
//    static private Formatter formatterHTML;

}
