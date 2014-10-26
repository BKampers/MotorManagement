/*
** Copyright © Bart Kampers
*/

package randd.motormanagement.communication;

import java.util.concurrent.*;
import org.json.*;


class Messenger {
    
    
    interface Listener { 
        void notifyMessage(JSONObject message);
    }
    
    
    static final String MESSAGE  = "Message";
    static final String RESPONSE = "Response";
    static final String SUBJECT  = "Subject";

    
    Messenger(SerialPort serialPort) {
        this.serialPort = serialPort;        
    }
    
    
    void setListener(Listener listener) {
        this.listener = listener;
    }
    

    void open() throws bka.communication.ChannelException {
        serialPort.open();
        Thread receiveThread = new Thread(receiveTask);
        Thread transactionThread = new Thread(transactionTask);
        receiveThread.start();
        transactionThread.start();
    }
    
    
    void close() {
        transactionTask.stop();
        receiveTask.stop();
        serialPort.close();
    }
    
    
    /**
     * Enqueues JSON message for sending and waits for response. 
     * @param message
     * @return response or time out object
     * @throws InterruptedException
     */
    JSONObject send(JSONObject message) throws InterruptedException{
        Transaction transaction = new Transaction(message);
        synchronized (transaction) {
            transactions.add(transaction);
            transaction.wait(MAXIMUM_RESPONSE_TIME * 2); // Should not timeout as long as transactionTask is running.
        }
        assert (transaction.response != null);
        return transaction.response;
    }
    
    
    private class ReceiveTask implements Runnable {
        
        public void run() {
            try {
                while (running && serialPort != null) {
                    JSONObject receivedObject = serialPort.nextReceivedObject();
                    if (receivedObject.length() > 0) {
                        //TODO: log incomming message System.out.println("<< " + receivedObject);
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
                ex.printStackTrace(System.err);
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
        public void run() {
            try {
                while (running) {
                    Transaction transaction;
                    transaction = transactions.take();
                    if (transaction.message != null) {
                        // TODO: log outgoing message System.out.println(">> " + transaction.message.toString());
                        sendAndWait(transaction);
                        ensureResponse(transaction);
                        notifyResponse(transaction);
                    }
                }
            }
            catch (InterruptedException ex) {
                ex.printStackTrace(System.err);
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
                    serialPort.send(transaction.message);
                    outstanding.wait(MAXIMUM_RESPONSE_TIME);
                }
            }
            catch (InterruptedException ex) {
                ex.printStackTrace(System.err);
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
                    ex.printStackTrace(System.err);
                }
            }
        }

        private void notifyResponse(Transaction transaction) {
            synchronized (transaction) {
                outstanding.transaction = null;
                transaction.notify();
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
    
    
    private final SerialPort serialPort;

    private Listener listener = null;
    
    private final TransactionTask transactionTask = new TransactionTask();
    private final ReceiveTask receiveTask = new ReceiveTask();
    private final BlockingQueue<Transaction> transactions = new LinkedBlockingQueue<>();
    
    private final Outstanding outstanding = new Outstanding();

    private static final long MAXIMUM_RESPONSE_TIME = 2500; // ms

}
