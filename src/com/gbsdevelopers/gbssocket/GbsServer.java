package com.gbsdevelopers.gbssocket;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;

/**
 * This class contains everything is required to handle receiving, processing and returning requests via TCP Socket
 */
public class GbsServer {
    /**
     * File name for logs
     */
    public final static String logFileName = "gbsserver-log.log";

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
     * MySQL's connection socket
     */
    public static Connection conn;

    /**
     * Simple function that puts logs in console window
     *
     * @param message Log message
     */
    public static void log(String message) {
        String logmessage = "[GbsServer]\t " + message;

        FileWriter fw = null;
        try {
            fw = new FileWriter(GbsServer.logFileName, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedWriter bw = new BufferedWriter(fw);
        try {
            bw.write(logmessage);
            bw.newLine();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(logmessage);
    }

    /**
     * Function returning random port number from range 49152 - 65535. Used when user don't have specified port number.
     *
     * @return Server port number
     */
    private static int getRandomPort() {
        int min = 49152;
        int max = 65535;
        return (int) Math.floor(Math.random() * (max - min + 1) + min);
    }

    /**
     * Function that try to parse string to int. If parse fails, returns random port number.
     *
     * @param str String to parse
     * @return Parsed integer
     */
    private static int tryParseInt(String str) {
        int number = getRandomPort();
        try {
            number = Integer.parseInt(str);
        } catch (NumberFormatException ex) {
            log("\"" + str + "\" is not a valid port number! Using random port number instead!");
        }
        return number;
    }

    /**
     * Main function. This function accepts a requests and creates a thread that handles them.
     *
     * @param args Program arguments
     */
    public static void main(String[] args) {
        int serverPort;
        isMySqlSet = false;
        try {
            serverPort = tryParseInt(args[0]);
        } catch (ArrayIndexOutOfBoundsException ex) {
            log("You have not specified port number! Using random port number instead!");
            serverPort = getRandomPort();
        }
        ServerSocket serverSocket;
        ObjectOutputStream toClient;
        ObjectInputStream fromClient;
        try {
            log("Server running on port " + serverPort);
            serverSocket = new ServerSocket(serverPort);
            while (true) {
                Socket socket = serverSocket.accept();
                log("Connection open with " + socket.getRemoteSocketAddress());
                toClient = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                fromClient = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
                GbsMessage msgRequest = (GbsMessage) fromClient.readObject();
                new Thread(new GbsServerThread(msgRequest.header, msgRequest.arguments, toClient, socket.getRemoteSocketAddress())).start();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}