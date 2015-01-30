/*
** Copyright © Bart Kampers
*/

package randd.motormanagement.swing;


import java.util.*;


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
            java.util.logging.Logger.getLogger(Bundle.class.getName()).log(java.util.logging.Level.WARNING, ex.toString());
            return key;
        }
    }
    
    
    private static Bundle instance;

    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("randd/motormanagement/Bundle");
    
}
