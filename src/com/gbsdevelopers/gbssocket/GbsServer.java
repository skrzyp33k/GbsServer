package com.gbsdevelopers.gbssocket;

import java.io.*;
import java.net.*;
import java.sql.Connection;

/**
 * This class contains everything is required to handle receiving, processing and returning requests via TCP Socket
 */
public class GbsServer {
    /**
     * MySQL's connection string
     */
    public static String mysqlString;

    /**
     * MySQL's credentials flag
     */
    public static boolean isMySqlSet;

    /**
     * MySQL's user
     */
    public static String mysqlUser;

    /**
     * MySQL's password
     */
    public static String mysqlPassword;

    /**
     * Simple function that puts logs in console window
     * @param message Log message
     */

    public static Connection conn;

    public static void log(String message) {
        System.out.println("[GbsServer]\t " + message);
    }

    /**
     * Function returning random port number from range 49152 - 65535. Used when user don't have specified port number.
     * @return Server port number
     */
    private static int getRandomPort()
    {
        int min = 49152;
        int max = 65535;
        return (int)Math.floor(Math.random()*(max-min+1)+min);
    }

    /**
     * Function that try to parse string to int. If parse fails, returns random port number.
     * @param str String to parse
     * @return Parsed integer
     */
    private static int tryParseInt(String str)
    {
        int number = getRandomPort();
        try{
            number = Integer.parseInt(str);
        }
        catch (NumberFormatException ex){
            log("\""+str+"\" is not a valid port number! Using random port number instead!");
        }
        return number;
    }

    /**
     * Main function. This function accepts a requests and creates a thread that handles them.
     * @param args Program arguments
     */
    public static void main(String[] args) {
        int serverPort = 0;
        isMySqlSet = false;
        try
        {
            serverPort = tryParseInt(args[0]);
        }
        catch(ArrayIndexOutOfBoundsException ex)
        {
            log("You have not specified port number! Using random port number instead!");
            serverPort = getRandomPort();
        }
        ServerSocket serverSocket = null;
        ObjectOutputStream toClient = null;
        ObjectInputStream fromClient = null;
        try {
            log("Server running on port " + serverPort);
            serverSocket = new ServerSocket(serverPort);
            while (true) {
                Socket socket = serverSocket.accept();
                log("Connection open with " + socket.getRemoteSocketAddress());
                toClient = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                fromClient = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
                GbsMessage msgRequest = (GbsMessage) fromClient.readObject();
                new Thread(new GbsServerThread(msgRequest.header, msgRequest.arguments, toClient,
                        socket.getRemoteSocketAddress())).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}