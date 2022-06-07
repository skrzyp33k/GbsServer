package com.gbsdevelopers.gbssocket;

import java.io.*;
import java.net.SocketAddress;
import java.util.*;

import java.sql.*;

public class GbsServerThread implements Runnable {

    private String inputHeader;

    private Vector<String> inputArguments;

    private ObjectOutputStream toClient;

    private SocketAddress socketAddress;

    private GbsMessage emptyMessage;

    public GbsServerThread(String inputHeader, Vector<String> inputArguments, ObjectOutputStream toClient,
            SocketAddress socketAddress) {
        this.inputHeader = inputHeader;
        this.inputArguments = inputArguments;
        this.toClient = toClient;
        this.socketAddress = socketAddress;

        emptyMessage = new GbsMessage("empty", new Vector<String>());
    }

    private void log(String message) {
        System.out.println("[" + inputHeader + socketAddress + "]\t " + message);
    }

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
