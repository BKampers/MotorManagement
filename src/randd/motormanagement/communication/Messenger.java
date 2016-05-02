/*
** Copyright © Bart Kampers
*/

package randd.motormanagement.communication;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;


class Messenger {
    
    
    static final String DIRECTION = "Direction";
    static final String FIRE = "Fire";
    static final String CALL = "Call";
    static final String FUNCTION = "Function";    
    static final String RETURN = "Return";
    static final String PARAMETERS = "Parameters";
    static final String RETURN_VALUE = "ReturnValue";

    
    interface Listener { 
        void notifyMessage(JSONObject message);
        void notifyResponse(JSONObject message, JSONObject response);
    }
    
    
    Messenger(Transporter transporter) {
        if (transporter == null) {
            throw new IllegalArgumentException();
        }
        this.transporter = transporter;
        String transporterName = transporter.getName();
        if (transporterName != null) {
            transporterName = transporterName.replaceAll("\\.", "-");
        }
        else {
            transporterName = transporter.getClass().getSimpleName();
        }
        logger = Logger.getLogger(Messenger.class.getName() + "." + transporterName);
     }
    
    
    void setListener(Listener listener) {
        this.listener = listener;
    }
    

    /**
     * Starts messenger. The messenger is ready to send and receive until close is called;
     * @see #stop()
     * @throws bka.communication.ChannelException
     */
    void start() throws bka.communication.ChannelException {
        transporter.open();
        Thread receiveThread = new Thread(receiveTask);
        Thread transactionThread = new Thread(transactionTask);
        receiveThread.start();
        transactionThread.start();
    }
    

    /**
     * Stop sending and receiving messages.
     * @see #start()
     * @throws bka.communication.ChannelException
     */
    void stop() throws bka.communication.ChannelException {
        transactionTask.stop();
        receiveTask.stop();
        transporter.close();
    }
    
    
    /**
     * Enqueues message for sending. Received responses will be notified through Listener.
     * When this Messenger is not open the message will not be sent.
     * @see #start()
     * @see #setListener(Listener)
     * @param message
     */
    void send(JSONObject message) {
        transactions.add(new Transaction(message));
    }
    
    
    private class ReceiveTask implements Runnable {

        @Override
        public void run() {
            try {
                while (running && transporter != null) {
                    JSONObject receivedObject = transporter.nextReceivedObject();
                    if (receivedObject.length() > 0) {
                        handleReceivedObject(receivedObject);
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

        private void handleReceivedObject(JSONObject receivedObject) {
            logger.log(Level.FINEST, "<< {0}", receivedObject);
            boolean tranactionHandled = false;
            if (outstanding.transaction != null) {
                tranactionHandled = handleTransaction(receivedObject);
            }
            if (! tranactionHandled && listener != null) {
                listener.notifyMessage(receivedObject);
            }
        }

        private boolean handleTransaction(JSONObject receivedObject) {
            boolean handled = false;
            String function = outstanding.transaction.message.optString(FUNCTION);
            if (isResponse(receivedObject, function)) {
                synchronized (outstanding) {
                    outstanding.transaction.response = receivedObject;
                    outstanding.notify();
                    handled = true;
                }
            }
            return handled;
        }
        
        private boolean isResponse(JSONObject object, String function) {
            return 
                object != null &&
                function != null &&
                function.equals(object.opt(FUNCTION)) &&
                RETURN.equals(object.opt(DIRECTION));
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
                    Transaction transaction = transactions.take();
                    if (transaction.message != null) {
                        sendAndWait(transaction);
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
            logger.log(Level.FINEST, ">> {0}", transaction.message);
            transportAndWait(transaction);
            if (transaction.response != null) {
                notifyResponse(transaction);
            }
            else {
                logger.log(
                    Level.WARNING,
                    "Response timeout\nmessage = {0}\ntimeout = {1} ms",
                    new Object[] { transaction.message, MAXIMUM_RESPONSE_TIME });
            }
        }
        
        private void transportAndWait(Transaction transaction) {
            try {
                synchronized (outstanding) {
                    outstanding.transaction = transaction;
                    transporter.send(transaction.message);
                    outstanding.wait(MAXIMUM_RESPONSE_TIME);
                }
            }
            catch (InterruptedException ex) {
                logger.log(Level.WARNING, "sendAndWait", ex);
            }
        }
        
        private void notifyResponse(Transaction transaction) {
            outstanding.transaction = null;
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

    private final Logger logger;
    
    private static final long MAXIMUM_RESPONSE_TIME = 5000; // ms

}
