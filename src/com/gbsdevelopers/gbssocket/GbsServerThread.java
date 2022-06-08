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

        String loggedUser = "";
        String loggedPerms = "";
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

        Vector<String> replyArgs = new Vector<String>();

        if(loggedUser.isEmpty())
        {
            reply.header = "1";
            log("Invalid login credentials");
        }
        else
        {
            reply.header = "0";
            replyArgs.add(loggedUser);
            replyArgs.add(loggedPerms);
            log("Logged user " + loggedUser + " with \"" + loggedPerms +  "\" perms");
        }

        reply.arguments = replyArgs;

        return reply;
    }

    /**
     * Handler for _configureDB header.
     * @return Reply message.
     */
    private GbsMessage _configureDB()
    {
        if(!GbsServer.isMySqlSet) {
            GbsServer.mysqlString = "jdbc:mysql://";
            GbsServer.mysqlString += inputArguments.get(0);
            GbsServer.mysqlString += ":";
            GbsServer.mysqlString += inputArguments.get(1);
            GbsServer.mysqlString += "/";
            GbsServer.mysqlString += inputArguments.get(2);

            GbsServer.mysqlUser = inputArguments.get(3);

            GbsServer.mysqlPassword = inputArguments.get(4);

            log("MySql connection string: " + GbsServer.mysqlString);
            log("MySql connection user: " + GbsServer.mysqlUser);
            log("MySql connection password " + GbsServer.mysqlPassword);

            GbsServer.isMySqlSet = true;
        }
        else
        {
            GbsServer.log("MySql credentials are previously set.");
        }
        return new GbsMessage("0", null);
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
                response = new GbsMessage("empty", new Vector<String>());;
        }
        log("finished");
        GbsServer.log("Connection closed with " + socketAddress);
        return response;
    }
}
