/*
** Copyright © Bart Kampers
*/

package randd.motormanagement.communication;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;
import org.json.JSONObject;


class Messenger {
    
    
    interface Listener { 
        void notifyMessage(JSONObject message);
        void notifyResponse(JSONObject message, JSONObject response);
    }
    
    
    static final String MESSAGE  = "Message";
    static final String RESPONSE = "Response";
    static final String SUBJECT  = "Subject";

    
    Messenger(Transporter transporter) {
        this.transporter = transporter;
        String channelName = transporter.getName().replaceAll("\\.", "-");
        logger = Logger.getLogger(Messenger.class.getName() + "." + channelName);
     }
    
    
    void setListener(Listener listener) {
        this.listener = listener;
    }
    

    void open() throws bka.communication.ChannelException {
        transporter.open();
        Thread receiveThread = new Thread(receiveTask);
        Thread transactionThread = new Thread(transactionTask);
        receiveThread.start();
        transactionThread.start();
    }
    
    
    void close() throws bka.communication.ChannelException {
        transactionTask.stop();
        receiveTask.stop();
        transporter.close();
    }
    
    
    /**
     * Enqueues JSON message for sending and waits for response. 
     * @param message
     * @return response or time out object
     * @throws InterruptedException
     */
    void send(JSONObject message) throws InterruptedException {
        Transaction transaction = new Transaction(message);
//        synchronized (transaction) {
            transactions.add(transaction);
//            transaction.wait(MAXIMUM_RESPONSE_TIME * 2); // Should not timeout as long as transactionTask is running.
//        }
//        assert (transaction.response != null);
//        return transaction.response;
    }
    
    
    private class ReceiveTask implements Runnable {
        
        @Override
        public void run() {
            try {
                while (running && transporter != null) {
                    JSONObject receivedObject = transporter.nextReceivedObject();
                    if (receivedObject.length() > 0) {
                        logger.log(Level.FINEST, "<< {0}", receivedObject);
                        boolean responded = false;
                        if (outstanding.transaction != null) {
                            String message = outstanding.transaction.message.optString(MESSAGE);
                            String subject = outstanding.transaction.message.optString(SUBJECT);
                            if (isResponse(receivedObject, message, subject)) {
                                synchronized (outstanding) {
                                    outstanding.transaction.response = receivedObject;
                                    outstanding.notify();
                                    responded = true;
                                }
                            }
                        }
                        if (! responded && listener != null) {
                            listener.notifyMessage(receivedObject);
                        }
                    }
                }
            }
            catch (InterruptedException ex) {
                logger.log(Level.SEVERE, getClass().getName(), ex);
                running = false;
            }
        }
        
        void stop() {
            running = false;
        }
        
        private boolean isResponse(JSONObject object, String message, String subject) {
            return 
                object != null &&
                message != null &&
                subject != null &&
                message.equals(object.opt(RESPONSE)) &&
                subject.equals(object.opt(SUBJECT));
        }

        private volatile boolean running = true;
        
    }
    
    
    private class TransactionTask implements Runnable {

        /**
         * Sends transactions in queue and waits for response.
         * Assigns response to transaction if received within MAXIMUM_RESPONSE_TIME.
         * Assigns timeout object otherwise.
         * Next transaction in queue is sent after previous is responded or timed out. 
         */
        @Override
        public void run() {
            try {
                while (running) {
                    Transaction transaction;
                    transaction = transactions.take();
                    if (transaction.message != null) {
                        logger.log(Level.FINEST, ">> {0}", transaction.message);
                        sendAndWait(transaction);
                        //ensureResponse(transaction);
                        if (transaction.response != null) {
                            notifyResponse(transaction);
                        }
                        else {
                            logger.log(
                                    Level.WARNING,
                                    "Response timeout\nmessage = {0}\ntimeout = {1} ms",
                                    new Object[]{transaction.message, MAXIMUM_RESPONSE_TIME});
                        }
                    }
                }
            }
            catch (InterruptedException ex) {
                logger.log(Level.SEVERE, getClass().getName(), ex);
                running = false;
            }
        }
        
        void stop() {
            running = false;
            transactions.add(new Transaction(null)); // deblock if waiting for transaction
        }

        private void sendAndWait(Transaction transaction) {
            try {
                synchronized (outstanding) {
                    outstanding.transaction = transaction;
                    transporter.send(transaction.message);
                    outstanding.wait(MAXIMUM_RESPONSE_TIME);
                }
            }
            catch (InterruptedException ex) {
                logger.log(Level.WARNING, getClass().getName(), ex);
            }
        }
        
        private void ensureResponse(Transaction transaction) {
            if (transaction.response == null) {
                try {
                    transaction.response = new JSONObject();
                    transaction.response.put("TransactionTimeout", MAXIMUM_RESPONSE_TIME);
                }
                catch (JSONException ex) {
                    // Will not occur since key in put method is not null
                    logger.log(Level.WARNING, getClass().getName(), ex);
                }
            }
        }

        private void notifyResponse(Transaction transaction) {
//            synchronized (transaction) {
                outstanding.transaction = null;
//                transaction.notify();
//            }
                if (listener != null) {
                    listener.notifyResponse(transaction.message, transaction.response);
                }
        }

        private volatile boolean running = true;

    }


    private class Transaction {
        
        Transaction(JSONObject message) {
            this.message = message;
        }
        
        JSONObject message;
        JSONObject response = null;
    }
    
    
    private class Outstanding {
        Transaction transaction = null;
    }
    
    
    private final Transporter transporter;

    private Listener listener = null;
    
    private final TransactionTask transactionTask = new TransactionTask();
    private final ReceiveTask receiveTask = new ReceiveTask();
    private final BlockingQueue<Transaction> transactions = new LinkedBlockingQueue<>();
    
    private final Outstanding outstanding = new Outstanding();

    private static Logger logger;
    
    private static final long MAXIMUM_RESPONSE_TIME = 5000; // ms

}
