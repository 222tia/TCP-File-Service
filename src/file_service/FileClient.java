package file_service;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class FileClient {

    private final static int STATUS_CODE_LENGTH = 1;

    public static void main(String[] args) throws Exception{

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
                   // TODO: add code for upload command
                }

                case "G" -> {
                    // TODO: add code for download command
                }

                case "L" -> {
                    String directoryName = getUserInput(keyboard, "Please enter the directory you want to list the files of: ");
                    ByteBuffer request = ByteBuffer.wrap((command + directoryName).getBytes());

                    SocketChannel channel = connectAndShutdownTCPSocket(request, serverPort, args);

                    String statusCode = getStatusCode(channel);
                    System.out.println(statusCode);

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

    public static String getUserInput(Scanner keyboard, String inputPrompt) {
        System.out.println(inputPrompt);
        return keyboard.nextLine();
    }


}
