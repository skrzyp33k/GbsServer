package com.gbsdevelopers.gbssocket;

import java.io.*;
import java.net.SocketAddress;
import java.util.*;

import java.sql.*;

/**
 * This class contains every server action.
 */
public class GbsServerThread implements Runnable {

    /**
     * Header of received message.
     */
    private String inputHeader;

    /**
     * Arguments of received message.
     */
    private Vector<String> inputArguments;

    /**
     * Stream to client.
     */
    private ObjectOutputStream toClient;

    /**
     * Server socket address
     */
    private SocketAddress socketAddress;

    /**
     * Empty return message
     */
    private GbsMessage emptyMessage;

    /**
     * Constructor for server thread.
     * @param inputHeader Header of received message.
     * @param inputArguments Arguments of received message.
     * @param toClient Stream to client.
     * @param socketAddress Server socket address
     */
    public GbsServerThread(String inputHeader, Vector<String> inputArguments, ObjectOutputStream toClient,
            SocketAddress socketAddress) {
        this.inputHeader = inputHeader;
        this.inputArguments = inputArguments;
        this.toClient = toClient;
        this.socketAddress = socketAddress;

        emptyMessage = new GbsMessage("empty", new Vector<String>());
    }

    /**
     * Simple function that puts logs in console window
     * @param message Log message
     */
    private void log(String message) {
        System.out.println("[" + inputHeader + socketAddress + "]\t " + message);
    }

    /**
     * run() method. Necessary for threads.
     */
    public void run() {
        log("started");
        try {
            toClient.writeObject(getResponse());
            toClient.flush();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Connects to database.
     * @return MySQL's connection.
     */
    private Connection getConnection(){
        Connection conn = null;
        try{
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(GbsServer.mysqlString, GbsServer.mysqlUser, GbsServer.mysqlPassword);
            return conn;
        }catch (Exception ex){
            log(ex.getMessage());
            return null;
        }
    }

    // Handlers

    /**
     * Handler for _loginUser header.
     * @return Reply message.
     */
    private GbsMessage _loginUser() {
        GbsMessage reply = new GbsMessage();

        String login = inputArguments.get(0);
        String password = inputArguments.get(1);

        Connection conn = getConnection();
        String query = "SELECT * FROM konta WHERE Login = '" + login + "' AND Haslo = '" + password + "';";
        Statement st;
        ResultSet rs;

        String loggedUser = null;
        String loggedPerms = null;
        try{
            st = conn.createStatement();
            rs = st.executeQuery(query);

            while(rs.next())
            {
                loggedUser = rs.getString("Login");
                loggedPerms = rs.getString("Uprawnienia");
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        reply.header = "0";

        Vector<String> replyArgs = new Vector<String>();

        replyArgs.add(loggedUser);
        replyArgs.add(loggedPerms);

        reply.arguments = replyArgs;

        return reply;
    }

    /**
     * Handler for _configureDB header.
     * @return Reply message.
     */
    private GbsMessage _configureDB()
    {
        GbsServer.mysqlString += inputArguments.get(0);
        GbsServer.mysqlString += ":";
        GbsServer.mysqlString += inputArguments.get(1);
        GbsServer.mysqlString += "/";
        GbsServer.mysqlString += inputArguments.get(2);

        GbsServer.mysqlUser = inputArguments.get(3);

        GbsServer.mysqlPassword = inputArguments.get(4);

        log("mysql connection:\t" + GbsServer.mysqlString + "\t" + GbsServer.mysqlUser + "\t" + GbsServer.mysqlPassword);

        return emptyMessage;
    }

    /**
     * Creates server response.
     * @return Server response message.
     */
    private GbsMessage getResponse() {
        GbsMessage response;
        // obsluga serwera
        switch (inputHeader) {
            case "_configureDB":
                response = _configureDB();
                break;
            case "_loginUser":
                response = _loginUser();
                break;
            default:
                response = emptyMessage;
        }
        log("finished");
        return response;
    }
}
