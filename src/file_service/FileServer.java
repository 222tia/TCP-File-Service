package file_service;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class FileServer {

    public static final String BASE_FILE_PATH = "C:/Users/tiase/Documents/ijprojects/TCP-File-Service/src/";
    private static final String SERVER_FILE_PATH = "server_files/";

    private static final String CLIENT_FILE_PATH = "client_files/";

    public static ReentrantLock lock = new ReentrantLock(); // Lock to sync threads

    public static boolean isClientUploading = false; // checks if any file is being uploaded
    public static Condition isNotUploading = lock.newCondition(); // signals that a file isn't being uploaded
    public static Condition isDoneDownloading = lock.newCondition(); // checks when a download is finished

    private static int clientsUploadingNum = 0;
    private static int clientsDownloadingNum = 0;

    public static void main(String[] args) throws Exception {

        ExecutorService es = Executors.newFixedThreadPool(4);

        int port = 3000;
        ServerSocketChannel welcomeChannel = ServerSocketChannel.open(); // responsible accepting connection requests
        welcomeChannel.bind(new InetSocketAddress(port)); //inetsocket address represents port and ip address

        loop:while (true) {

            SocketChannel serverChannel = welcomeChannel.accept(); // accepts client request and creates a new socket, will establish the tcp connection with the client
            ByteBuffer request = ByteBuffer.allocate(2500); // create empty buffer to read client request

            int numBytes; // keep track of bytes that has been read from the tcp channel
            do {
                numBytes = serverChannel.read(request); // read serverChannel and save data into the byte buffer
            } while (numBytes >= 0);

            request.flip();
            char command = (char) request.get(); // convert from byte to char
            switch (command) {

                case 'D' -> {
                    String userRequest = extractRequest(request);
                    File fileToDelete = new File(BASE_FILE_PATH + SERVER_FILE_PATH + userRequest);

                    boolean success = false;
                    if (fileToDelete.exists()) {
                        success = fileToDelete.delete();
                    }

                    sendStatusCode(success, serverChannel);
                }

                case 'U' -> {
                    // submit request to the executor service to be executed
                    Runnable uploader = new CaseU(serverChannel, request);
                    es.submit(uploader);

                }

                case 'G' -> {
                    // submit request to the executor service to be executed
                    Runnable downloader = new CaseG(serverChannel, request);
                    es.submit(downloader);
                }

                case 'R' -> {
                    String userRequest = extractRequest(request);
                    String [] fileNameAndNewFileName = userRequest.split("/", -2);

                    String originalFileName = fileNameAndNewFileName[0];
                    File fileToRename = new File(BASE_FILE_PATH + SERVER_FILE_PATH + originalFileName);

                    String newFileName = fileNameAndNewFileName[1];
                    File renamedFile = new File(BASE_FILE_PATH + SERVER_FILE_PATH + newFileName);

                    boolean success = false;
                    if (fileToRename.exists()) {
                        success = fileToRename.renameTo(renamedFile);
                    }

                    sendStatusCode(success, serverChannel);
                }

                case 'L' -> {
                    String directoryName = extractRequest(request);
                    File directory = new File(BASE_FILE_PATH + directoryName);

                    boolean success = false;
                    if (directory.exists()) {
                        String[] filesInDirectory = directory.list();
                        // sends list of files to the client
                        ByteBuffer list = ByteBuffer.wrap((Arrays.toString(filesInDirectory)).getBytes());
                        serverChannel.write(list);
                        serverChannel.close();
                    } else {
                        sendStatusCode(success, serverChannel);
                    }
                }

                case 'Q' -> {
                    break loop;
                }
            }

        }

        es.shutdown();

    }

    public static void sendStatusCode(boolean success, SocketChannel serverChannel) throws Exception {
        ByteBuffer code;
        if (success) {
            code = ByteBuffer.wrap("S".getBytes()); // create bytebuffer to put code into
        } else {
            code = ByteBuffer.wrap("F".getBytes());
        }
        serverChannel.write(code); // write code into the channel to be received by the client
        serverChannel.close();
    }

    public static String extractRequest(ByteBuffer request){
        byte[] a = new byte[request.remaining()]; // create new byte array to extract request from the buffer
        request.get(a);
        return new String(a);
    }

    private static class CaseU implements Runnable {

        private final SocketChannel serverChannel;
        private final ByteBuffer request;

        public CaseU(SocketChannel serverChannel, ByteBuffer request){
            this.serverChannel = serverChannel;
            this.request = request;
        }
        public void run() {

            lock.lock();

            try {

                while(isClientUploading && clientsDownloadingNum ==0) {
                    isDoneDownloading.await();
                }
                isClientUploading = true;
                clientsUploadingNum++;

                // upload logic
                String userRequest = extractRequest(request);
                File fileToUpload = new File(BASE_FILE_PATH + CLIENT_FILE_PATH + userRequest);

                boolean success = false;
                if (fileToUpload.exists()) {
                    success = true;
                    File destination = new File(BASE_FILE_PATH + SERVER_FILE_PATH + userRequest);
                    Files.copy(fileToUpload.toPath(), destination.toPath());
                }

                sendStatusCode(success, serverChannel);
                // end of upload logic

                clientsUploadingNum--;
                if(clientsUploadingNum == 0){
                    isNotUploading.signal();
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                isClientUploading = false;
                lock.unlock();
            }

        }

    }
    private static class CaseG implements Runnable {

        private final SocketChannel serverChannel;
        private final ByteBuffer request;

        public CaseG(SocketChannel serverChannel, ByteBuffer request){
            this.serverChannel = serverChannel;
            this.request = request;
        }
        public void run() {

            lock.lock();

            try {

                while(isClientUploading) {
                    isNotUploading.await();
                }
                clientsDownloadingNum++;

                // download logic
                String userRequest = extractRequest(request);
                File fileToDownload = new File(BASE_FILE_PATH + SERVER_FILE_PATH + userRequest);

                boolean success = false;
                if (fileToDownload.exists()) {
                    success = true;
                    File destination = new File(BASE_FILE_PATH + CLIENT_FILE_PATH + userRequest);
                    Files.copy(fileToDownload.toPath(), destination.toPath());
                }

                sendStatusCode(success, serverChannel);
                // end of download logic

                clientsDownloadingNum--;
                if(clientsDownloadingNum == 0){
                    isDoneDownloading.signal();
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                lock.unlock();
            }

        }

    }
}







