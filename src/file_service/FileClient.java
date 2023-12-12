package file_service;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class FileClient {

    private final static int STATUS_CODE_LENGTH = 1;
    private final static int LIST_LENGTH = 2500;

    public static ReentrantLock lock = new ReentrantLock(); // Lock to sync threads

    public static boolean isClientUploading = false; // checks if any file is being uploaded
    public static Condition isNotUploading = lock.newCondition(); // signals that a file isn't being uploaded
    public static Condition isDoneDownloading = lock.newCondition(); // checks when a download is finished

    private static int clientsUploadingNum = 0;
    private static int clientsDownloadingNum = 0;

    public static void main(String[] args) throws Exception{

        ExecutorService es = Executors.newFixedThreadPool(4);

        if (args.length !=2) {
            System.out.println("Syntax: FileClient <ServerIP> <ServerPort>");
            return;
        }

        // convert server port into an int
        int serverPort = Integer.parseInt(args[1]);

        // loop to serve multiple user requests
        // do while runs the body once and then checks the condition at the end
        String command;
        do {
            Scanner keyboard = new Scanner(System.in);
            System.out.println("Please type a command: ");
            command = keyboard.nextLine();

            switch (command) {

                case "D" -> {
                    String fileName = getUserInput(keyboard, "Please enter the name of the file you want to delete: ");
                    ByteBuffer request = ByteBuffer.wrap((command + fileName).getBytes());

                    SocketChannel channel = connectAndShutdownTCPSocket(request, serverPort, args);

                    String statusCode = getStatusCode(channel);
                    System.out.println(statusCode);
                    }

                case "U" -> {
                    String fileName = getUserInput(keyboard, "Please enter the name of the file you want to upload:");
                    ByteBuffer request = ByteBuffer.wrap((command + fileName).getBytes());

                    // submit request to the executor service to be executed
                    Runnable uploader = new CaseU(request, serverPort,args);
                    es.submit(uploader);
                }

                case "G" -> {
                    String fileName = getUserInput(keyboard, "Please enter the name of the file you want to download:");
                    ByteBuffer request = ByteBuffer.wrap((command + fileName).getBytes());

                    // submit request to the executor service to be executed
                    Runnable downloader = new CaseG(request, serverPort, args);
                    es.submit(downloader);
                }

                case "L" -> {
                    String directoryName = getUserInput(keyboard, "Please enter the directory you want to list the files of: ");
                    ByteBuffer request = ByteBuffer.wrap((command + directoryName).getBytes());

                    SocketChannel channel = connectAndShutdownTCPSocket(request, serverPort, args);

                    File directory = new File(FileServer.BASE_FILE_PATH + directoryName);
                    if (directory.exists()){
                        String list = getList(channel); // TODO: fix BufferUnderFlowException
                        System.out.println(list);
                    } else {
                        String statusCode = getStatusCode(channel);
                        System.out.println(statusCode);
                    }

                }

                case "R" -> {
                    String fileName =  getUserInput(keyboard, "Please enter the name of the file you want to rename and what you want to rename it to (file_name/new_file_name): ");
                    ByteBuffer request = ByteBuffer.wrap((command + fileName).getBytes());

                    SocketChannel channel = connectAndShutdownTCPSocket(request, serverPort, args);


                    String statusCode = getStatusCode(channel);
                    System.out.println(statusCode);
                }

                default -> {

                    if (!command.equalsIgnoreCase("Q")) {
                        System.out.println("Unknown command");
                    }

                }
            }
        } while(!command.equalsIgnoreCase("Q"));

        es.shutdown();
    }

    public static SocketChannel connectAndShutdownTCPSocket(ByteBuffer request, int serverPort, String[] args) throws Exception {
        SocketChannel channel = SocketChannel.open(); // open TCP socket
        channel.connect(new InetSocketAddress(args[0], serverPort)); // connect socket to server ip and port
        channel.write(request); // write request into byte buffer to be sent to the server
        channel.shutdownOutput(); // tells server to stop waiting to receive more and to process what has been sent
        return channel;
    }

    public static String getStatusCode(SocketChannel channel) throws Exception{
        ByteBuffer code = ByteBuffer.allocate(STATUS_CODE_LENGTH); // create buffer to receive status code from the server
        channel.read(code);
        code.flip(); // flip byte buffer
        byte[] a = new byte[STATUS_CODE_LENGTH];
        code.get(a); // get content from buffer and put it in a byte array
        return new String(a); // convert byte array "a" into a string
    }

    public static String getList(SocketChannel channel) throws Exception {
        ByteBuffer code = ByteBuffer.allocate(LIST_LENGTH);
        channel.read(code);
        code.flip();
        byte[] a = new byte[LIST_LENGTH];
        code.get(a);
        return new String(a);
    }

    public static String getUserInput(Scanner keyboard, String inputPrompt) {
        System.out.println(inputPrompt);
        return keyboard.nextLine();
    }

    private static class CaseU implements Runnable {
        
        private final int serverPort;
        private final String[] args;
        private final ByteBuffer request;

        public CaseU(ByteBuffer request, int serverPort, String[] args){
            this.request = request;
            this.serverPort = serverPort;
            this.args = args;
        }
        public void run() {

            lock.lock();

            try {

                while(isClientUploading && clientsDownloadingNum == 0) {
                    isDoneDownloading.await();
                }
                isClientUploading = true;
                clientsUploadingNum++;

                // send request
                SocketChannel channel = connectAndShutdownTCPSocket(request, serverPort, args);

                String statusCode = getStatusCode(channel);
                System.out.println(statusCode);
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
        private final int serverPort;
        private final String[] args;
        private final ByteBuffer request;

        public CaseG(ByteBuffer request, int serverPort,String[] args){
            this.request = request;
            this.serverPort = serverPort;
            this.args = args;
        }
        public void run() {

            lock.lock();

            try {

                while(isClientUploading) {
                    isNotUploading.await();
                }
                clientsDownloadingNum++;

                // send request
                SocketChannel channel = connectAndShutdownTCPSocket(request, serverPort, args);

                String statusCode = getStatusCode(channel);
                System.out.println(statusCode);
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
