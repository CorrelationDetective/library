package data_io;

import data_io.minio.Result;
import data_io.minio.messages.Item;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;

public class MinioReaderTest {
    private String minioConfigPath = "/home/jens/tue/1.SimilarityDetective/SimilarityDetective/minio_config.properties";

    private MinioHandler minioReader;


    @Before
    public void setUp(){
        this.minioReader = new MinioHandler(minioConfigPath);
    }

    @Test
    public void testListObjects(){
        String bucketName = "correlation-detective";
        Iterable<Result<Item>> objects = minioReader.listObjects(bucketName);

        try{
            int i = 0;
            for (Result<Item> object : objects) {
                String objectName = object.get().objectName();
                System.out.println(String.format("Object %d: %s", i++, objectName));
            }
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    @Test
    public void testReadObject(){
        String bucketName = "correlation-detective";
        String fileName = "example_data.csv";
        String filePath = String.format("s3://%s/%s", bucketName, fileName);

        BufferedReader reader = minioReader.getBufferedDataReader(filePath);

//        Print the data line by line
        try{
            System.out.println(reader.readLine());
            while (reader.ready()){
                System.out.println(reader.readLine());
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    @Test
    public void testWriteObject(){
        String bucketName = "correlation-detective";
        String outPath = "test.txt";
        outPath = String.format("s3://%s/%s", bucketName, outPath);

//        Upload simple text to minio
        minioReader.writeToFile(outPath, "hello world");
    }
}
