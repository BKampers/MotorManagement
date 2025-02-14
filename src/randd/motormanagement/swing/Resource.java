package randd.motormanagement.swing;


import java.io.*;


class Resource {

    Resource(File file) {
        this.file = file;
    }
    
    Resource(String path) {
        file = new File(loader.getResource(path).getFile());
    }
    
    String loadText() throws IOException {
        try (Reader reader = new FileReader(file)) {
            char[] chars = new char[(int) file.length()];
            reader.read(chars);
            return new String(chars);
        }
    }

    private final File file;
    
    private static final ClassLoader loader = Thread.currentThread().getContextClassLoader();
}
