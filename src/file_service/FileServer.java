package file_service;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

public class FileServer {

    private static final String defaultPath = "C:/Users/tiase/Documents/ijprojects/CS316-Project3/src/";

    public static void main(String[] args) throws Exception {

        int port = 3000;
        ServerSocketChannel welcomeChannel = ServerSocketChannel.open(); // responsible accepting connection requests
        welcomeChannel.bind(new InetSocketAddress(port)); //inetsocket address represents port and ip address

        while (true) {

            SocketChannel serverChannel = welcomeChannel.accept(); // accepts client request and creates a new socket, will establish the tcp connection with the client
            // read client request
            ByteBuffer request = ByteBuffer.allocate(2500); // create empty buffer

            // keep track of bytes that has been read from the tcp channel
            int numBytes;
            do {
                numBytes = serverChannel.read(request); // read serverChannel and save data into the byte buffer
                // when shutdownoutput signal is received, .read() will return -1 and then break out of the do while loop
            } while (numBytes >= 0);

            request.flip();
            char command = (char) request.get(); // convert from byte to char
            switch (command) {

                case 'D' -> {
                    File file = extractRequest(request);

                    boolean success = false;
                    if (file.exists()) {
                        success = file.delete();
                    }

                    sendStatusCode(success, serverChannel);
                }

                case 'R' -> {
                    File file = extractRequest(request);

                    String newFileName = defaultPath + "test_directory/renamed_file";
                    File newFile = new File(newFileName);

                    boolean success = false;
                    if (file.exists()) {
                        success = file.renameTo(newFile);
                    }

                    sendStatusCode(success, serverChannel);
                }

                case 'L' -> {
                    File directory = extractRequest(request);

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

    public static File extractRequest(ByteBuffer request){
        byte[] a = new byte[request.remaining()]; // create new byte array to extract request from the buffer
        request.get(a);
        String pathName = new String(a);
        return new File(defaultPath + pathName);
    }
}




