package file_service;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

public class FileServer {

    private static final String filePath = "C:/Users/tiase/Documents/ijprojects/CS316-Project3/src/";
    private static final String directoryName = "test_directory/";

    public static void main(String[] args) throws Exception {

        int port = 3000;
        ServerSocketChannel welcomeChannel = ServerSocketChannel.open(); // responsible accepting connection requests
        welcomeChannel.bind(new InetSocketAddress(port)); //inetsocket address represents port and ip address

        while (true) {

            SocketChannel serverChannel = welcomeChannel.accept(); // accepts client request and creates a new socket, will establish the tcp connection with the client
            ByteBuffer request = ByteBuffer.allocate(2500); // create empty buffer to read client request

            int numBytes; // keep track of bytes that has been read from the tcp channel
            do {
                numBytes = serverChannel.read(request); // read serverChannel and save data into the byte buffer
                // when shutdownoutput signal is received, .read() will return -1 and then break out of the do while loop
            } while (numBytes >= 0);

            request.flip();
            char command = (char) request.get(); // convert from byte to char
            switch (command) {

                case 'D' -> {
                    String userRequest = extractRequest(request);
                    File fileToDelete = new File(filePath + directoryName + userRequest);

                    boolean success = false;
                    if (fileToDelete.exists()) {
                        success = fileToDelete.delete();
                    }

                    sendStatusCode(success, serverChannel);
                }

                case 'U' -> {
                    // TODO: add code for upload command
                }

                case 'G' -> {
                    // TODO: add code for download command
                }

                case 'R' -> {
                    String userRequest = extractRequest(request);
                    String [] fileNameAndNewFileName = userRequest.split("/", -2);

                    String originalFileName = fileNameAndNewFileName[0];
                    File fileToRename = new File(filePath + directoryName + originalFileName);

                    String newFileName = fileNameAndNewFileName[1];
                    File renamedFile = new File(filePath + directoryName + newFileName);

                    boolean success = false;
                    if (fileToRename.exists()) {
                        success = fileToRename.renameTo(renamedFile);
                    }

                    sendStatusCode(success, serverChannel);
                }

                case 'L' -> {
                    String directoryName = extractRequest(request);
                    File directory = new File(filePath + directoryName);

                    boolean success = false;
                    if (directory.exists()) {
                        success = true;
                        String files = Arrays.toString(directory.list()); // get list of files
                        ByteBuffer filesInDirectory = ByteBuffer.wrap(files.getBytes()); // wrap in a byte buffer
                        // TODO: send filesInDirectory back to client
                    }

                    sendStatusCode(success, serverChannel);

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




