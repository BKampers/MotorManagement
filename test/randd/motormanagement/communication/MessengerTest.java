/*
** Copyright © Bart Kampers
*/

package randd.motormanagement.communication;

import bka.communication.json.Messenger;
import bka.communication.json.Transporter;
import bka.communication.*;
import org.json.*;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mockito.*;
import static org.mockito.Mockito.when;


public class MessengerTest {

    private static Transporter transporter;
    private Messenger messenger;


    public MessengerTest() {
    }


    @BeforeClass
    public static void setUpClass() throws InterruptedException, JSONException {
        transporter = Mockito.mock(Transporter.class);
        when(transporter.getName()).thenReturn("TransporterMock");
    }


    @AfterClass
    public static void tearDownClass() {
        transporter = null;
    }


    @Before
    public void setUp() {
        messenger = new Messenger(transporter);
    }


    @After
    public void tearDown() {
        messenger = null;
    }


    /**
     * Test receiving notification.
     * @throws bka.communication.ChannelException
     * @throws java.lang.InterruptedException
     * @throws org.json.JSONException
     */
    @Test
    public void testNotification() throws ChannelException, InterruptedException, JSONException {
        JSONObject notification = new JSONObject();
        notification.put("Notification", "Mocked notification");
        MessengerListener listener = new MessengerListener();
        when(transporter.nextReceivedObject()).thenReturn(EMPTY);
        messenger.setListener(listener);
        messenger.start();
        when(transporter.nextReceivedObject()).thenReturn(notification);
        Thread.sleep(10);
        when(transporter.nextReceivedObject()).thenReturn(EMPTY);
        messenger.stop();
        assertEquals(notification, listener.receivedNotification);
    }


    /**
     * Test receiving response for sent message
     * @throws bka.communication.ChannelException
     * @throws java.lang.InterruptedException
     * @throws org.json.JSONException
     */
    @Test
    public void testResponse() throws ChannelException, InterruptedException, JSONException {
        JSONObject message = new JSONObject();
        message.put("Message", "Request");
        message.put("Subject", "Test");
        JSONObject response = new JSONObject();
        response.put("Response", "Request");
        response.put("Subject", "Test");
        MessengerListener listener = new MessengerListener();
        when(transporter.nextReceivedObject()).thenReturn(EMPTY);
        messenger.setListener(listener);
        messenger.start();
        messenger.send(message);
        when(transporter.nextReceivedObject()).thenReturn(response);
        Thread.sleep(10);
        when(transporter.nextReceivedObject()).thenReturn(EMPTY);
        messenger.stop();
        assertEquals(message, listener.receivedMessage);
        assertEquals(response, listener.receivedResponse);
    }


    /**
     * Test notification not received when messenger not opened.
     * @throws java.lang.InterruptedException
     * @throws org.json.JSONException
     */
    @Test
    public void testNotStarted() throws InterruptedException, JSONException  {
        JSONObject notification = new JSONObject();
        notification.put("Notification", "Mocked notification");
        MessengerListener listener = new MessengerListener();
        when(transporter.nextReceivedObject()).thenReturn(EMPTY);
        messenger.setListener(listener);
        when(transporter.nextReceivedObject()).thenReturn(notification);
        Thread.sleep(10);
        when(transporter.nextReceivedObject()).thenReturn(EMPTY);
        assertNull(listener.receivedNotification);
    }


    /**
     * Test notification not received when messenger after close.
     * @throws bka.communication.ChannelException
     * @throws java.lang.InterruptedException
     * @throws org.json.JSONException
     */
    @Test
    public void testStop() throws ChannelException, InterruptedException, JSONException  {
        JSONObject notification = new JSONObject();
        notification.put("Notification", "Mock");
        MessengerListener listener = new MessengerListener();
        when(transporter.nextReceivedObject()).thenReturn(EMPTY);
        messenger.setListener(listener);
        messenger.start();
        messenger.stop();
        when(transporter.nextReceivedObject()).thenReturn(notification);
        Thread.sleep(10);
        when(transporter.nextReceivedObject()).thenReturn(EMPTY);
        assertNull(listener.receivedNotification);
    }


    private class MessengerListener implements Messenger.Listener {

        @Override
        public void notifyMessage(JSONObject message) {
            receivedNotification = message;
        }

        @Override
        public void notifyResponse(JSONObject message, JSONObject response) {
            receivedMessage = message;
            receivedResponse = response;
        }

        JSONObject receivedNotification;
        JSONObject receivedMessage;
        JSONObject receivedResponse;

    }


    private static final JSONObject EMPTY = new JSONObject();
 
}