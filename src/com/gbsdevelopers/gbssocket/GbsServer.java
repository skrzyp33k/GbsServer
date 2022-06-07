package com.gbsdevelopers.gbssocket;

import java.io.*;
import java.net.*;

public class GbsServer {

    public static String mysqlString;

    public static String mysqlUser;

    public static String mysqlPassword;

    private static void log(String message) {
        System.out.println("[GbsServer]\t " + message);
    }

    private static int getRandomPort()
    {
        int min = 49152;
        int max = 65535;
        return (int)Math.floor(Math.random()*(max-min+1)+min);
    }

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

    public static void main(String[] args) {
        mysqlString = "jdbc:mysql://";
        int serverPort = 0;
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
                log("Connection closed with " + socket.getRemoteSocketAddress());
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