package data_io;

import _aux.lib;
import core.RunParameters;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Item;
import lombok.Getter;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

public class MinioHandler extends DataHandler {

    private final String SERVER_ENDPOINT;
    private final String ACCESS_KEY;
    private final String SECRET_KEY;

    @Getter private MinioClient minioClient;

    public MinioHandler(String serverEndpoint, String accessKey, String secretKey) {
        this.SERVER_ENDPOINT = serverEndpoint;
        this.ACCESS_KEY = accessKey;
        this.SECRET_KEY = secretKey;

//        Initialize the Minio client
        initializeClient();
    }

    public MinioHandler(String minioConfigPath){
        Properties config = lib.parseConfig(minioConfigPath);
        this.SERVER_ENDPOINT = config.getProperty("server_endpoint");
        this.ACCESS_KEY = config.getProperty("access_key");
        this.SECRET_KEY = config.getProperty("secret_key");

        initializeClient();
    }

//    Initialization of the Minio client through minio environment variables
    public MinioHandler(){
//        Check if necessary environment variables are set
        if (System.getenv("MINIO_ENDPOINT_URL") == null && System.getProperty("MINIO_ENDPOINT_URL") == null){
            throw new IllegalArgumentException("Environment variable MINIO_ENDPOINT_URL is not set");
        }
        if (System.getenv("MINIO_ACCESS_KEY") == null && System.getProperty("MINIO_ACCESS_KEY") == null){
            throw new IllegalArgumentException("Environment variable MINIO_ACCESS_KEY is not set");
        }
        if (System.getenv("MINIO_SECRET_KEY") == null && System.getProperty("MINIO_SECRET_KEY") == null){
            throw new IllegalArgumentException("Environment variable MINIO_SECRET_KEY is not set");
        }

//        Get the MINIO_ENDPOINT_URL, MINIO_ACCESS_KEY and MINIO_SECRET_KEY environment variables
        this.SERVER_ENDPOINT = System.getenv("MINIO_ENDPOINT_URL") == null ? System.getProperty("MINIO_ENDPOINT_URL") : System.getenv("MINIO_ENDPOINT_URL");
        this.ACCESS_KEY = System.getenv("MINIO_ACCESS_KEY") == null ? System.getProperty("MINIO_ACCESS_KEY") : System.getenv("MINIO_ACCESS_KEY");
        this.SECRET_KEY = System.getenv("MINIO_SECRET_KEY") == null ? System.getProperty("MINIO_SECRET_KEY") : System.getenv("MINIO_SECRET_KEY");

        initializeClient();
    }

    public void initializeClient(){
        try {
            this.minioClient = MinioClient.builder()
                    .endpoint(this.SERVER_ENDPOINT)
                    .credentials(this.ACCESS_KEY, this.SECRET_KEY)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    Check if a bucket exists
    public boolean bucketExists(String bucketName){
        try {
            return minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

//    Make sure the path is in the format "s3:/bucket_name/file_name"
    private String checkPath(String path){
//        if (!path.startsWith(RunParameters.S3_PREFIX)){
//            throw new IllegalArgumentException("Path must start with " + RunParameters.S3_PREFIX);
//        }

//        Make sure bucket exists
        String bucketName = path.split("/")[0];
        if (!this.bucketExists(bucketName)){
            throw new IllegalArgumentException("Bucket '" + bucketName + "' does not exist");
        }

//        Remove the S3 prefix from path
//        path = path.substring(RunParameters.S3_PREFIX.length());
        return path;
    }

    public String[] getBucketObjectFromPath(String path){
//        Get the bucket name and object path
        //        Get the bucket name and object path
        String[] pathSplit = path.split("/");
        String bucketName = pathSplit[0];
        String objectPath = String.join("/", lib.remove(pathSplit, 0));

        return new String[]{bucketName, objectPath};
    }

    public Iterable<Result<Item>> listObjects(String bucketName) {
        try {
            return minioClient.listObjects(
                    io.minio.ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .build());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

//    Upload a file to Minio
    public void uploadObject(String bucketName, String objectPath, String filePath) {
        try{
            minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectPath)
                            .filename(filePath)
                            .build());
            System.out.println(String.format("%s is uploaded to bucket %s successfully", objectPath, bucketName));
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    // Function to read an object from a Minio bucket
    public BufferedReader getObjectReader(String bucketName, String objectPath) {
        try {
            InputStream objectStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectPath)
                            .build()
            );
            return new BufferedReader(new java.io.InputStreamReader(objectStream));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

//    Get buffered reader for a file in Minio
    public BufferedReader getBufferedDataReader(String path){
//        Make sure the path is correct
        path = this.checkPath(path);

//        Get the bucket name and object path
        String[] tmp = this.getBucketObjectFromPath(path);
        String bucketName = tmp[0];
        String objectPath = tmp[1];

        return this.getObjectReader(bucketName, objectPath);
    }

//    Write string to a new file in Minio
    public void writeToFile(String path, String data){
//        Make sure the path is correct
        path = this.checkPath(path);

//        Get the bucket name and object path
        String[] tmp = this.getBucketObjectFromPath(path);
        String bucketName = tmp[0];
        String objectPath = tmp[1];

//        First write the file to a temporary local file
        String tmpPath = "/tmp/minio_tmp";
        new FileHandler().writeToFile(tmpPath, data);

//        Then upload the file to Minio
        this.uploadObject(bucketName, objectPath, tmpPath);
    }


}
