/*
** © Bart Kampers
*/

package bka.communication;

import javax.swing.*;

/**
 * Plugin for Monitor Console.
 * Handles incomming data.
 * Optionally provides a view.
 */
public interface Plugin {

    /**
     * Handle received data from channel
     *
     * @param data
     */
    void receive(byte[] data);

    /**
     * @return View for the pugin if required, null otherwise
     */
    JPanel getView();

    void updateView();

}
