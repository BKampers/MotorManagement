/*
** Copyright © Bart Kampers
*/

package randd.motormanagement.swing;


import java.util.*;


public class Bundle {

    static Bundle getInstance() {
        return INSTANCE;
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
    
    

    
    
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("randd/motormanagement/Bundle");
    
    private static final Bundle INSTANCE = new Bundle();
    
}
