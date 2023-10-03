package file_service;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class FileServer {

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
                    byte[] a = new byte[request.remaining()]; // create new byte array to extract request from the buffer
                    request.get(a);
                    String fileName = new String(a);
                    File file = new File("C:/Users/tiase/CS/ijprojects/TCPFileService/src/test_directory/" + fileName);
                    boolean success = false;
                    if (file.exists()) {
                        success = file.delete();
                    }

                    sendStatusCode(success, serverChannel);
                }

                case 'R' -> {
                    byte[] a = new byte[request.remaining()]; // create new byte array to extract request from the buffer
                    request.get(a);
                    String fileName = new String(a);
                    File file = new File("C:/Users/tiase/CS/ijprojects/TCPFileService/src/test_directory/" + fileName);

                    String newFileName = "renamed_file";
                    File newFile = new File("C:/Users/tiase/CS/ijprojects/TCPFileService/src/test_directory/" + newFileName);

                    boolean success = false;
                    if (file.exists()) {
                        success = file.renameTo(newFile);
                    }

                    sendStatusCode(success, serverChannel);
                }

                case 'L' -> {
                    byte[] a = new byte[request.remaining()]; // create new byte array to extract request from the buffer
                    request.get(a);
                    String directoryName = new String(a);
                    File directory = new File("C:/Users/tiase/CS/ijprojects/TCPFileService/src/" + directoryName);

                    boolean success = false;
                    if (directory.exists()) {
                        // for file in directory
                        // wrap files in byte buffer and send it back to the client

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
}




