package data_io;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

public class FileHandler extends DataHandler {
    public BufferedReader getBufferedDataReader(String path) throws FileNotFoundException {
        return new BufferedReader(new java.io.FileReader(path));
    }

    public void writeToFile(String path, String data) {
        try {
            FileWriter resultWriter = new FileWriter(path);
            resultWriter.write(data);
            resultWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
