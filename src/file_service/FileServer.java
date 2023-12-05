package file_service;

import java.io.File;
import java.io.IOException;
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

    public static ReentrantLock lock = new ReentrantLock(); // Lock to sync threads

    public static boolean isWriterWriting = false; //Checks if writers are active
    public static Condition isNoWriter = lock.newCondition(); // Checks if there are any writers
    public static Condition isDoneReading = lock.newCondition(); // Checks when readers are done reading

    private static int writerNum = 0;
    private static int readerNum = 0;
    private static int resource = 0;

    public static final String BASE_FILE_PATH = "C:/Users/tiase/Documents/ijprojects/TCP-File-Service/src/";
    private static final String SERVER_FILE_PATH = "server_files/";

    private static final String CLIENT_FILE_PATH = "client_files/";

    public static void main(String[] args) {
        ExecutorService es = Executors.newFixedThreadPool(4);
        for(int i=0; i<6; i++){
            es.submit(new FileServer.Handler());
        }
        es.shutdown();
    }

    private static class Handler implements Runnable {

        public void run() {


            int port = 3000;
            ServerSocketChannel welcomeChannel = null; // responsible accepting connection requests
            try {
                welcomeChannel = ServerSocketChannel.open();
                welcomeChannel.bind(new InetSocketAddress(port)); //inetsocket address represents port and ip address
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            while (true) {

                SocketChannel serverChannel = null; // accepts client request and creates a new socket, will establish the tcp connection with the client
                try {
                    serverChannel = welcomeChannel.accept();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                ByteBuffer request = ByteBuffer.allocate(2500); // create empty buffer to read client request

                int numBytes; // keep track of bytes that has been read from the tcp channel
                do {
                    try {
                        numBytes = serverChannel.read(request); // read serverChannel and save data into the byte buffer
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } while (numBytes >= 0);

                request.flip();
                char command = (char) request.get(); // convert from byte to char
                switch (command) {

                    case 'D' -> {
                        lock.lock();
                        try {
                            while (isWriterWriting) {
                                isNoWriter.await();
                            }
                            readerNum++;

                            // TODO: file read logic
                            String userRequest = extractRequest(request);
                            File fileToDelete = new File(BASE_FILE_PATH + SERVER_FILE_PATH + userRequest);

                            boolean success = false;
                            if (fileToDelete.exists()) {
                                success = fileToDelete.delete();
                            }

                            sendStatusCode(success, serverChannel);
                            // release reader

                            System.out.println("Reader #" + readerNum + " has read resource as: " + resource);
                            readerNum--;
                            if (readerNum == 0) {
                                isDoneReading.signal();
                            }

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        } finally {
                            lock.unlock();
                        }


                    }

                    case 'U' -> {
                        lock.lock();
                        try {
                            while (isWriterWriting && readerNum == 0) {
                                isDoneReading.await();
                            }
                            isWriterWriting = true;
                            resource++;
                            writerNum++;

                            // TODO: file write logic
                            String userRequest = extractRequest(request);
                            File fileToUpload = new File(BASE_FILE_PATH + CLIENT_FILE_PATH + userRequest);

                            boolean success = false;
                            if (fileToUpload.exists()) {
                                success = true;
                                File destination = new File(BASE_FILE_PATH + SERVER_FILE_PATH + userRequest);
                                Files.copy(fileToUpload.toPath(), destination.toPath());
                            }

                            sendStatusCode(success, serverChannel);
                            // release writer

                            System.out.println("Writer # " + writerNum + " wrote the resource to: " + resource);
                            writerNum--;
                            if (writerNum == 0) {
                                isNoWriter.signal();
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        } finally {
                            isWriterWriting = false;
                            lock.unlock();
                        }


                    }

                    case 'G' -> {
                        String userRequest = extractRequest(request);
                        File fileToDownload = new File(BASE_FILE_PATH + SERVER_FILE_PATH + userRequest);

                        boolean success = false;
                        if (fileToDownload.exists()) {
                            success = true;
                            File destination = new File(BASE_FILE_PATH + CLIENT_FILE_PATH + userRequest);
                            try {
                                Files.copy(fileToDownload.toPath(), destination.toPath());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        try {
                            sendStatusCode(success, serverChannel);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                    case 'R' -> {
                        String userRequest = extractRequest(request);
                        String[] fileNameAndNewFileName = userRequest.split("/", -2);

                        String originalFileName = fileNameAndNewFileName[0];
                        File fileToRename = new File(BASE_FILE_PATH + SERVER_FILE_PATH + originalFileName);

                        String newFileName = fileNameAndNewFileName[1];
                        File renamedFile = new File(BASE_FILE_PATH + SERVER_FILE_PATH + newFileName);

                        boolean success = false;
                        if (fileToRename.exists()) {
                            success = fileToRename.renameTo(renamedFile);
                        }

                        try {
                            sendStatusCode(success, serverChannel);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                    case 'L' -> {
                        String directoryName = extractRequest(request);
                        File directory = new File(BASE_FILE_PATH + directoryName);

                        boolean success = false;
                        if (directory.exists()) {
                            String[] filesInDirectory = directory.list();
                            // sends list of files to the client
                            ByteBuffer list = ByteBuffer.wrap((Arrays.toString(filesInDirectory)).getBytes());
                            try {
                                serverChannel.write(list);
                                serverChannel.close();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }

                        } else {
                            try {
                                sendStatusCode(success, serverChannel);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }

                }

            }
        }
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
}




