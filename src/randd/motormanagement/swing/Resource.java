package randd.motormanagement.swing;


import java.io.*;
import java.net.URISyntaxException;


class Resource {
    

    Resource(File file) {
        this.file = file;
    }
    
    
    Resource(String path) {
        File pathFile;
        java.net.URL url = getUrl(path);
        try {
            pathFile = new File(url.toURI());
        }
        catch (URISyntaxException ex) {
            pathFile = new File(url.getFile());
        }
        file = pathFile;
    }
    
    
    String loadText() throws IOException {
        try (Reader reader = new FileReader(file)) {
            char[] chars = new char[(int) file.length()];
            reader.read(chars);
            return new String(chars);
        }
    }
    
    
    private static java.net.URL getUrl(String resourceName) {
        return loader.getResource(resourceName);
    }


    private final File file;
    
    private static final ClassLoader loader = Thread.currentThread().getContextClassLoader();
}
