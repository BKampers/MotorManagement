/*
** Copyright © Bart Kampers
*/

package randd.motormanagement.swing;


import java.util.*;
import java.util.logging.*;


class Bundle {

    
    static Bundle getInstance() {
        if (instance == null) {
            instance = new Bundle();
        }
        return instance;
    }
    
    
    String get(String key) {
        try {
            return BUNDLE.getString(key);
        }
        catch (MissingResourceException ex) {
            Logger.getLogger(Bundle.class.getName()).log(Level.WARNING, ex.toString());
            return key;
        }
    }
    
    
    private static Bundle instance;

    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("randd/motormanagement/Bundle");
    
}
