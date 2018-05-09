import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class S3Client {

    private static String bucket_name = "mobile-analytics-dc-10124019";

    static AmazonS3 s3Clientt = null;

    static String AccessKey = "";
    static String SecretKey = "";


    static{

        getCredential();

        BasicAWSCredentials credentials = new BasicAWSCredentials(AccessKey, SecretKey);
        AmazonS3 s3Clientt = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials)).withRegion(Regions.US_WEST_1)
                .build();

    }

//    private static void getCredential(){
//        System.out.println("path: " + System.getProperty("user.dir") + "\\config\\credential.txt");
//        File file = new File(System.getProperty("user.dir") + "\\config\\credential.txt");
//        BufferedReader reader = null;
//        try {
//            reader = new BufferedReader(new FileReader(file));
//            String tempString = null;
//            if( (tempString = reader.readLine()) != null){
//                AccessKey = tempString;
//            }
//            if( (tempString = reader.readLine()) != null){
//                SecretKey = tempString;
//            }
//            reader.close();
//        } catch (Exception ex) {
//           System.out.print("exception: " + ex);
//        } finally {
//            if (reader != null) {
//                try {
//                    reader.close();
//                } catch (IOException e1) {
//                    System.out.print("cant close file: " + e1);
//                }
//            }
//        }
//    }

    private static void getCredential(){
        try {
            AccessKey = System.getenv("AWS_ACCESS_KEY_ID");
            SecretKey = System.getenv("AWS_SECRET_ACCESS_KEY");
        }
        catch (Exception ex) {
            System.out.print("exception happend when getting credential from env variable: " + ex);
        }
        finally {
           ;
        }
    }

    public static Bucket getBucket(String bucket_name) {

        AmazonS3 s3Client = getS3Client();

        Bucket named_bucket = null;
        List<Bucket> buckets = s3Client.listBuckets();
        for (Bucket b : buckets) {
            System.out.println("bucket name: " + b.getName());
            if (b.getName().equals(bucket_name)) {
                named_bucket = b;
            }
        }
        return named_bucket;
    }

    private static AmazonS3 getS3Client(){
        BasicAWSCredentials credentials = new BasicAWSCredentials(AccessKey, SecretKey);

        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials)).withRegion(Regions.US_WEST_1)
                .build();
        return s3Client;
    }

    private static void CreateBucket(String BucketName){
        Bucket b = null;

        AmazonS3 s3Client = getS3Client();


        if (s3Client.doesBucketExist(BucketName)) {
            System.out.format("Bucket %s already exists.\n", BucketName);
            b = getBucket(BucketName);
        } else {
            try {
                b = s3Client.createBucket(BucketName);
            } catch (AmazonS3Exception e) {
                System.err.println(e.getErrorMessage());
            }
        }
    }

    private static void DeleteBucket(String BucketName){

        AmazonS3 s3Client = getS3Client();

        try {

            //step1: remove objects
            System.out.println(" - removing objects from bucket");
            ObjectListing object_listing = s3Client.listObjects(BucketName);
            while (true) {
                for (Iterator<?> iterator =
                     object_listing.getObjectSummaries().iterator();
                     iterator.hasNext();) {
                    S3ObjectSummary summary = (S3ObjectSummary)iterator.next();
                    s3Client.deleteObject(BucketName, summary.getKey());
                }

                // more object_listing to retrieve?
                if (object_listing.isTruncated()) {
                    object_listing = s3Client.listNextBatchOfObjects(object_listing);
                } else {
                    break;
                }
            };

            //step2: delete bucket
            System.out.println(" OK, bucket ready to delete!");
            s3Client.deleteBucket(BucketName);

        } catch (AmazonServiceException e) {
            System.err.println(e.getErrorMessage());
            System.exit(1);
        }
    }


    private static boolean UploadSingleObject(String BucketName, String ObjectPath){

        AmazonS3 s3Client = getS3Client();

        String ObjectKeyName = Paths.get(ObjectPath).getFileName().toString();

        long FileLen = Paths.get(ObjectPath).toFile().length();
        System.out.format("Uploading %s to S3 bucket %s...\n", ObjectPath, BucketName);
        try {
            //s3Client.putObject(BucketName, ObjectKeyName, ObjectPath);
            File file = new File(ObjectPath);
            s3Client.putObject(new PutObjectRequest(
                    BucketName, ObjectKeyName, file));
        } catch (AmazonServiceException e) {
            System.err.println(e.getErrorMessage());
            return false;
        }

        return true;
    }


    private static boolean UploadSingleObjectWithSpecifiedName(String BucketName, String ObjectPath, String KeyName){

        AmazonS3 s3Client = getS3Client();

        String ObjectKeyName = KeyName;

        long FileLen = Paths.get(ObjectPath).toFile().length();
        System.out.format("Uploading %s to S3 bucket %s...\n", ObjectPath, BucketName);
        try {
            //s3Client.putObject(BucketName, ObjectKeyName, ObjectPath);
            File file = new File(ObjectPath);
            s3Client.putObject(new PutObjectRequest(
                    BucketName, ObjectKeyName, file));
        } catch (AmazonServiceException e) {
            System.err.println(e.getErrorMessage());
            return false;
        }

        return true;
    }

    public static void DownloadObjectFromBucket(String BucketName, String KeyName){

        AmazonS3 s3Client = getS3Client();
        String PathToSave = "C:\\Users\\fqyya\\Desktop\\testlog\\" + KeyName;

        try
        {
            S3Object o = s3Client.getObject(BucketName, KeyName);
            S3ObjectInputStream s3is = o.getObjectContent();
            FileOutputStream fos = new FileOutputStream(new File(PathToSave));
            byte[] read_buf = new byte[1024];
            int read_len = 0;
            while ((read_len = s3is.read(read_buf)) > 0) {
                fos.write(read_buf, 0, read_len);
            }
            s3is.close();
            fos.close();

        }
        catch (Exception ex) {
            System.err.println("exception happened when downloading bucket! exception: " + ex.toString());
        }
    }


    public static void DownloadBucket(String BucketName){

        try
        {
            BasicAWSCredentials credentials = new BasicAWSCredentials(AccessKey, SecretKey);

            AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(credentials)).withRegion(Regions.US_EAST_1)
                    .build();

            ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(BucketName).withPrefix("awsma/events/528711bbea684d52b896bb7cddeb7b72");
            ListObjectsV2Result result;

            List<String> Keys = new ArrayList<String>();

            do {
                result = s3Client.listObjectsV2(req);

                for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
                    System.out.printf(" - %s (size: %d)\n", objectSummary.getKey(), objectSummary.getSize());
                    Keys.add(objectSummary.getKey());
                    //if (objectSummary.getKey().equals("2018/03/19/21/scut_stream-1-2018-03-19-21-00-27-7c682af2-3deb-4936-a67e-3ee7bfb94d17.gz")){
//                    if(true){
//                        String KeyName = objectSummary.getKey();
//                        String[] values = KeyName.split("/");
//
//                        String PathToSave = "C:\\Users\\fqyya\\Desktop\\testlog\\data\\" + values[values.length - 1];
//
//                        S3Object o = s3Client.getObject(BucketName, KeyName);
//                        S3ObjectInputStream s3is = o.getObjectContent();
//                        FileOutputStream fos = new FileOutputStream(new File(PathToSave));
//                        byte[] read_buf = new byte[1024];
//                        int read_len = 0;
//                        while ((read_len = s3is.read(read_buf)) > 0) {
//                            fos.write(read_buf, 0, read_len);
//                        }
//                        s3is.close();
//                        fos.close();
//                    }

                }
                // If there are more than maxKeys keys in the bucket, get a continuation token
                // and list the next objects.
                String token = result.getNextContinuationToken();
                System.out.println("Next Continuation Token: " + token);
                req.setContinuationToken(token);
            } while (result.isTruncated());


            Thread.sleep(1000);
            int KeysLen = Keys.size();
            System.out.println(KeysLen + " files to download!");


            System.out.println("Begin to download! Please wait for a while!");
            long start = System.currentTimeMillis();

            int threadNum = 20;

            // 创建一个初始值为threadNum的倒数计数器
            CountDownLatch countDownLatch = new CountDownLatch(threadNum);

            // 将下载分成threadNum = 10份。
            int block = KeysLen % threadNum == 0 ? KeysLen / threadNum
                    : KeysLen / threadNum + 1;
            for (int threadId = 0; threadId < threadNum; threadId++) {
                new DownloadThread(threadId, block, Keys, s3Client, BucketName, countDownLatch).start();
            }

            // 阻塞当前线程，直到倒数计数器倒数到0
            countDownLatch.await();
            System.out.println("finished downloading");

            long end = System.currentTimeMillis();
            System.out.println("total download time：" + (end - start) / 1000 + "s");

        }
        catch (Exception ex) {
            System.err.println("exception happened when downloading bucket! exception: " + ex.toString());
        }
    }

    public static void main( String[] args ) {
        System.out.println( "begin to run!" );

//        CreateBucket("mobile-analytics-dc-10124024");
//        DeleteBucket("mobile-analytics-dc-10124024");
//        DeleteBucket("mobile-analytics-dc-10124023");
//        DeleteBucket("mobile-analytics-dc-10124022");
//        DeleteBucket("mobile-analytics-dc-10124020");
//        DeleteBucket("mobile-analytics-dc-10124019");


//        String BucketName = "mobile-analytics-dc-10124024";
//        //String ObjectPath = "C:\\dc_test\\yuanyuan.png";
//        String ObjectPath = "C:\\Users\\fqyya\\Desktop\\me.png";
//        //String ObjectPath = "C:\\dc_exercise\\qian3.jpg";
//
//
//        //if(UploadSingleObject(BucketName, ObjectPath)){
//        if(UploadSingleObjectWithSpecifiedName(BucketName, ObjectPath, "hello kitty")){
//            System.out.println("successfully upload object " + ObjectPath);
//        }
//        else{
//            System.err.println("Failed to upload object " + ObjectPath);
//        }

//        String BucketName = "mobile-analytics-dc-10124024";
//        //String BucketName = "mobile-analytics-11-06-2017-ac62389890644d2fa2549aaedf03d8ff";
//        String KeyName = "qq.jpg";
//        DownloadObjectFromBucket(BucketName, KeyName);

        String BucketName = "mobile-analytics-11-06-2017-ac62389890644d2fa2549aaedf03d8ff";
        DownloadBucket(BucketName);
    }
}


class DownloadThread extends Thread {
    private int start, end,threadId;
    private AmazonS3 s3Client;
    private String BucketName;
    private List<String> Keys;

    private CountDownLatch countDownLatch;

    public DownloadThread(int threadId,int block, List<String> Keys, AmazonS3 s3Client, String BucketName, CountDownLatch countDownLatch) {
        this.threadId = threadId;

        start = block * threadId;
        end = block * (threadId + 1) - 1;
        if(end >= Keys.size())
            end = Keys.size() - 1;

        this.Keys = Keys;
        this.s3Client = s3Client;
        this.BucketName = BucketName;

        this.countDownLatch = countDownLatch;
    }

    public void run() {
        try {
//            System.out.println("thread id: " +  threadId + " start: " + start + " , end: " + end);
            for(int i = start; i <= end; ++i){
                String KeyName = Keys.get(i);
                String[] values = KeyName.split("/");
                System.out.println("downloading " + values[values.length - 1]);
                String PathToSave = "C:\\Users\\fqyya\\Desktop\\testlog\\data\\" +  values[values.length - 1];

                S3Object o = s3Client.getObject(BucketName, KeyName);
                S3ObjectInputStream s3is = o.getObjectContent();
                FileOutputStream fos = new FileOutputStream(new File(PathToSave));
                byte[] read_buf = new byte[1024];
                int read_len = 0;
                while ((read_len = s3is.read(read_buf)) > 0) {
                      fos.write(read_buf, 0, read_len);
                }
                s3is.close();
                fos.close();
            }

            System.out.println("downloader thread " + this.getName() + " is over!");
            // 倒数器减1
            countDownLatch.countDown();

        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }
}