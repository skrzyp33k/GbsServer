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
     *
     * @param inputHeader    Header of received message.
     * @param inputArguments Arguments of received message.
     * @param toClient       Stream to client.
     * @param socketAddress  Server socket address
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
     *
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
     *
     * @return MySQL's connection.
     */
    private Connection getConnection() {
        Connection conn = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(GbsServer.mysqlString, GbsServer.mysqlUser, GbsServer.mysqlPassword);
            return conn;
        } catch (Exception ex) {
            log(ex.getMessage());
            return null;
        }
    }

    // Handlers

    /**
     * Handler for _loginUser header.
     *
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
        try {
            st = conn.createStatement();
            rs = st.executeQuery(query);

            while (rs.next()) {
                loggedUser = rs.getString("Login");
                loggedPerms = rs.getString("Uprawnienia");
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        Vector<String> replyArgs = new Vector<String>();

        if (loggedUser.isEmpty()) {
            reply.header = "1";
            log("Invalid login credentials");
        } else {
            reply.header = "0";
            replyArgs.add(loggedUser);
            replyArgs.add(loggedPerms);
            log("Logged user " + loggedUser + " with \"" + loggedPerms + "\" perms");
        }

        reply.arguments = replyArgs;

        return reply;
    }

    /**
     * Handler for _configureDB header.
     *
     * @return Reply message.
     */
    private GbsMessage _configureDB() {
        if (!GbsServer.isMySqlSet) {
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
        } else {
            GbsServer.log("MySql credentials are previously set.");
        }
        return new GbsMessage("0", null);
    }

    private GbsMessage _createClass() {
        GbsMessage reply = new GbsMessage();

        String className = inputArguments.get(0);

        Vector<String> queries = new Vector<String>();

        queries.add("INSERT INTO klasy VALUES(null, '" + className + "', " + inputArguments.get(1) + ");");
        queries.add("CREATE TABLE plan_" + className + " (Godzina int(11) NOT NULL, Poniedzialek int(4) DEFAULT NULL, Wtorek int(4) DEFAULT NULL, Sroda int(4) DEFAULT NULL, Czwartek int(4) DEFAULT NULL, Piatek int(4) DEFAULT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_polish_ci;");
        queries.add("ALTER TABLE plan_" + className + " ADD PRIMARY KEY (Godzina), ADD KEY Poniedzialek(Poniedzialek), ADD KEY Wtorek(Wtorek), ADD KEY Sroda(Sroda), ADD KEY Czwartek (Czwartek), ADD KEY Piatek (Piatek);");
        queries.add("ALTER TABLE plan_" + className + " ADD CONSTRAINT plan_" + className + "_ibfk_1 FOREIGN KEY (Poniedzialek) REFERENCES lekcje(ID_lekcji), ADD CONSTRAINT plan_" + className + "_ibfk_2 FOREIGN KEY(Wtorek) REFERENCES lekcje(ID_lekcji), ADD CONSTRAINT plan_" + className + "_ibfk_3 FOREIGN KEY(Sroda) REFERENCES lekcje(ID_lekcji), ADD CONSTRAINT plan_" + className + "_ibfk_4 FOREIGN KEY(Czwartek) REFERENCES lekcje(ID_lekcji), ADD CONSTRAINT plan_" + className + "_ibfk_5 FOREIGN KEY(Piatek) REFERENCES lekcje(ID_lekcji);");

        Connection conn = getConnection();
        Statement st;

        for (String query : queries) {
            try {
                log("Executing " + query);
                st = conn.createStatement();
                st.execute(query);

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return reply;
    }

    private GbsMessage _removeClass() {
        GbsMessage reply = new GbsMessage();

        String className = inputArguments.get(0);
        String classID = "";

        Connection conn = getConnection();
        String getIdQuery = "SELECT ID_klasy FROM klasy WHERE nazwa_klasy = '" + className + "';";
        Statement st;
        ResultSet rs;
        try {
            st = conn.createStatement();
            rs = st.executeQuery(getIdQuery);
            log("Executing " + getIdQuery);

            while (rs.next()) {
                classID = rs.getString("ID_klasy");
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        Vector<String> queries = new Vector<String>();

        queries.add("DELETE FROM klasy WHERE ID_klasy = " + classID + ";");
        queries.add("DELETE FROM lekcje WHERE ID_klasy = " + classID + ";");
        queries.add("DELETE FROM uczniowie WHERE ID_klasy = " + classID + ";");
        queries.add("DROP TABLE plan_" + className + ";");

        for (String query : queries) {
            try {
                log("Executing " + query);
                st = conn.createStatement();
                st.execute(query);

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return reply;
    }

    private GbsMessage _listSchedulesTables() {
        GbsMessage reply = new GbsMessage();

        Connection conn = getConnection();
        String query = "SELECT CONCAT('plan_',nazwa_klasy) AS Plany FROM klasy ORDER BY Plany ASC;";
        Statement st;
        ResultSet rs;

        int classCount = 0;

        Vector<String> classes = new Vector<String>();

        try {
            st = conn.createStatement();
            rs = st.executeQuery(query);

            while (rs.next()) {
                classCount++;
                classes.add(rs.getString("Plany"));
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        reply.header = Integer.toString(classCount);
        reply.arguments = classes;

        return reply;
    }

    private GbsMessage _listSchedule() {
        GbsMessage reply = new GbsMessage();

        String tableName = inputArguments.get(0);

        Connection conn = getConnection();
        String query = "SELECT * FROM " + tableName + ";";
        Statement st;
        ResultSet rs;

        int Count = 0;

        Vector<String> accounts = new Vector<String>();

        try {
            st = conn.createStatement();
            rs = st.executeQuery(query);

            while (rs.next()) {
                Count++;
                Vector<String> row = new Vector<String>();
                row.add(rs.getString("Godzina"));
                row.add(rs.getString("Poniedzialek"));
                row.add(rs.getString("Wtorek"));
                row.add(rs.getString("Sroda"));
                row.add(rs.getString("Czwartek"));
                row.add(rs.getString("Piatek"));

                accounts.add(GbsMessage.implode(row, ";"));
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        reply.header = Integer.toString(Count);
        reply.arguments = accounts;

        log("Sending " + tableName);

        return reply;
    }

    private GbsMessage _listAccounts() //dodaj where
    {
        GbsMessage reply = new GbsMessage();

        Connection conn = getConnection();
        String query = "SELECT * FROM konta;";
        Statement st;
        ResultSet rs;

        int Count = 0;

        Vector<String> accounts = new Vector<String>();

        try {
            st = conn.createStatement();
            rs = st.executeQuery(query);

            while (rs.next()) {
                Count++;
                Vector<String> row = new Vector<String>();
                row.add(rs.getString("ID_konta"));
                row.add(rs.getString("Login"));
                row.add(rs.getString("Haslo"));
                row.add(rs.getString("Uprawnienia"));

                accounts.add(GbsMessage.implode(row, ";"));
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        reply.header = Integer.toString(Count);
        reply.arguments = accounts;

        return reply;
    }

    private GbsMessage _listStudents() //dodaj where
    {
        GbsMessage reply = new GbsMessage();

        Connection conn = getConnection();
        String query = "SELECT u.ID_ucznia AS ID_ucznia, u.imie AS imie, u.nazwisko AS nazwisko, u.ID_rodzica AS ID_rodzica, u.ID_klasy AS ID_klasy, u.ID_konta AS ID_konta, k.nazwa_klasy FROM uczniowie AS u, klasy AS k WHERE u.ID_klasy = k.ID_klasy ORDER BY k.nazwa_klasy ASC, u.nazwisko ASC, u.imie ASC;";
        Statement st;
        ResultSet rs;

        int Count = 0;

        Vector<String> accounts = new Vector<String>();

        try {
            st = conn.createStatement();
            rs = st.executeQuery(query);

            while (rs.next()) {
                Count++;
                Vector<String> row = new Vector<String>();
                row.add(rs.getString("ID_ucznia"));
                row.add(rs.getString("imie"));
                row.add(rs.getString("nazwisko"));
                row.add(rs.getString("ID_rodzica"));
                row.add(rs.getString("ID_klasy"));
                row.add(rs.getString("ID_konta"));

                accounts.add(GbsMessage.implode(row, ";"));
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        reply.header = Integer.toString(Count);
        reply.arguments = accounts;

        return reply;
    }

    private GbsMessage _listParents() //dodaj where
    {
        GbsMessage reply = new GbsMessage();

        Connection conn = getConnection();
        String query = "SELECT * FROM rodzice ORDER BY nazwisko ASC, imie ASC;";
        Statement st;
        ResultSet rs;

        int Count = 0;

        Vector<String> accounts = new Vector<String>();

        try {
            st = conn.createStatement();
            rs = st.executeQuery(query);

            while (rs.next()) {
                Count++;
                Vector<String> row = new Vector<String>();
                row.add(rs.getString("ID_rodzica"));
                row.add(rs.getString("imie"));
                row.add(rs.getString("nazwisko"));
                row.add(rs.getString("ID_konta"));

                accounts.add(GbsMessage.implode(row, ";"));
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        reply.header = Integer.toString(Count);
        reply.arguments = accounts;

        return reply;
    }

    private GbsMessage _listTeachers() //dodaj where
    {
        GbsMessage reply = new GbsMessage();

        Connection conn = getConnection();
        String query = "SELECT * FROM nauczyciele ORDER BY nazwisko ASC, imie ASC;";
        Statement st;
        ResultSet rs;

        int Count = 0;

        Vector<String> accounts = new Vector<String>();

        try {
            st = conn.createStatement();
            rs = st.executeQuery(query);

            while (rs.next()) {
                Count++;
                Vector<String> row = new Vector<String>();
                row.add(rs.getString("ID_nauczyciela"));
                row.add(rs.getString("imie"));
                row.add(rs.getString("nazwisko"));
                row.add(rs.getString("telefon_kontaktowy"));
                row.add(rs.getString("ID_konta"));

                accounts.add(GbsMessage.implode(row, ";"));
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        reply.header = Integer.toString(Count);
        reply.arguments = accounts;

        return reply;
    }

    private GbsMessage _listClasses() //dodaj where
    {
        GbsMessage reply = new GbsMessage();

        Connection conn = getConnection();
        String query = "SELECT * FROM klasy ORDER BY nazwa_klasy ASC;";
        Statement st;
        ResultSet rs;

        int Count = 0;

        Vector<String> accounts = new Vector<String>();

        try {
            st = conn.createStatement();
            rs = st.executeQuery(query);

            while (rs.next()) {
                Count++;
                Vector<String> row = new Vector<String>();
                row.add(rs.getString("ID_klasy"));
                row.add(rs.getString("nazwa_klasy"));
                row.add(rs.getString("ID_nauczyciela"));

                accounts.add(GbsMessage.implode(row, ";"));
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        reply.header = Integer.toString(Count);
        reply.arguments = accounts;

        return reply;
    }

    private GbsMessage _listLessons() //dodaj where
    {
        GbsMessage reply = new GbsMessage();

        Connection conn = getConnection();
        String query = "SELECT * FROM lekcje ORDER BY Sala ASC;";
        Statement st;
        ResultSet rs;

        int Count = 0;

        Vector<String> accounts = new Vector<String>();

        try {
            st = conn.createStatement();
            rs = st.executeQuery(query);

            while (rs.next()) {
                Count++;
                Vector<String> row = new Vector<String>();
                row.add(rs.getString("ID_lekcji"));
                row.add(rs.getString("ID_przedmiotu"));
                row.add(rs.getString("Sala"));
                row.add(rs.getString("ID_nauczyciela"));
                row.add(rs.getString("ID_klasy"));

                accounts.add(GbsMessage.implode(row, ";"));
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        reply.header = Integer.toString(Count);
        reply.arguments = accounts;

        return reply;
    }

    private GbsMessage _listAttendances() //dodaj where
    {
        GbsMessage reply = new GbsMessage();

        Connection conn = getConnection();
        String query = "SELECT * FROM nieobecnosci ORDER BY data_nieobecnosci DESC;";
        Statement st;
        ResultSet rs;

        int Count = 0;

        Vector<String> accounts = new Vector<String>();

        try {
            st = conn.createStatement();
            rs = st.executeQuery(query);

            while (rs.next()) {
                Count++;
                Vector<String> row = new Vector<String>();
                row.add(rs.getString("ID_nieobecnosci"));
                row.add(rs.getString("ID_ucznia"));
                row.add(rs.getString("ID_lekcji"));
                row.add(rs.getString("data_nieobecnosci"));
                row.add(rs.getString("TYP"));

                accounts.add(GbsMessage.implode(row, ";"));
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        reply.header = Integer.toString(Count);
        reply.arguments = accounts;

        return reply;
    }

    private GbsMessage _listGrades() //dodaj where
    {
        GbsMessage reply = new GbsMessage();

        Connection conn = getConnection();
        String query = "SELECT * FROM oceny ORDER BY data_wystawienia DESC;";
        Statement st;
        ResultSet rs;

        int Count = 0;

        Vector<String> accounts = new Vector<String>();

        try {
            st = conn.createStatement();
            rs = st.executeQuery(query);

            while (rs.next()) {
                Count++;
                Vector<String> row = new Vector<String>();
                row.add(rs.getString("ID_oceny"));
                row.add(rs.getString("Ocena"));
                row.add(rs.getString("Waga"));
                row.add(rs.getString("Opis"));
                row.add(rs.getString("ID_ucznia"));
                row.add(rs.getString("ID_nauczyciela"));
                row.add(rs.getString("ID_przedmiotu"));
                row.add(rs.getString("data_wystawienia"));

                accounts.add(GbsMessage.implode(row, ";"));
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        reply.header = Integer.toString(Count);
        reply.arguments = accounts;

        return reply;
    }

    private GbsMessage _listCourses() //dodaj where
    {
        GbsMessage reply = new GbsMessage();

        Connection conn = getConnection();
        String query = "SELECT * FROM przedmioty ORDER BY nazwa_przedmiotu ASC;";
        Statement st;
        ResultSet rs;

        int Count = 0;

        Vector<String> accounts = new Vector<String>();

        try {
            st = conn.createStatement();
            rs = st.executeQuery(query);

            while (rs.next()) {
                Count++;
                Vector<String> row = new Vector<String>();
                row.add(rs.getString("ID_przedmiotu"));
                row.add(rs.getString("nazwa_przedmiotu"));
                row.add(rs.getString("opis"));

                accounts.add(GbsMessage.implode(row, ";"));
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        reply.header = Integer.toString(Count);
        reply.arguments = accounts;

        return reply;
    }

    private GbsMessage _listRemarks() //dodaj where
    {
        GbsMessage reply = new GbsMessage();

        Connection conn = getConnection();
        String query = "SELECT * FROM uwagi ORDER BY data_wystawienia DESC;";
        Statement st;
        ResultSet rs;

        int Count = 0;

        Vector<String> accounts = new Vector<String>();

        try {
            st = conn.createStatement();
            rs = st.executeQuery(query);

            while (rs.next()) {
                Count++;
                Vector<String> row = new Vector<String>();
                row.add(rs.getString("ID_uwagi"));
                row.add(rs.getString("Tresc"));
                row.add(rs.getString("ID_nauczyciela"));
                row.add(rs.getString("ID_ucznia"));
                row.add(rs.getString("data_wystawienia"));

                accounts.add(GbsMessage.implode(row, ";"));
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        reply.header = Integer.toString(Count);
        reply.arguments = accounts;

        return reply;
    }

    private GbsMessage _listEvents() //dodaj where
    {
        GbsMessage reply = new GbsMessage();

        Connection conn = getConnection();
        String query = "SELECT * FROM wydarzenia ORDER BY data_wydarzenia ASC;";
        Statement st;
        ResultSet rs;

        int Count = 0;

        Vector<String> accounts = new Vector<String>();

        try {
            st = conn.createStatement();
            rs = st.executeQuery(query);

            while (rs.next()) {
                Count++;
                Vector<String> row = new Vector<String>();
                row.add(rs.getString("ID_wydarzenia"));
                row.add(rs.getString("Kategoria"));
                row.add(rs.getString("Opis"));
                row.add(rs.getString("ID_lekcji"));
                row.add(rs.getString("data_wydarzenia"));

                accounts.add(GbsMessage.implode(row, ";"));
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        reply.header = Integer.toString(Count);
        reply.arguments = accounts;

        return reply;
    }

    private GbsMessage _listMessages() //dodaj where
    {
        GbsMessage reply = new GbsMessage();

        Connection conn = getConnection();
        String query = "SELECT * FROM wiadomosci ORDER BY data_wyslania DESC;";
        Statement st;
        ResultSet rs;

        int Count = 0;

        Vector<String> accounts = new Vector<String>();

        try {
            st = conn.createStatement();
            rs = st.executeQuery(query);

            while (rs.next()) {
                Count++;
                Vector<String> row = new Vector<String>();
                row.add(rs.getString("ID_wiadomosci"));
                row.add(rs.getString("Wiadomosc"));
                row.add(rs.getString("ID_nadawcy"));
                row.add(rs.getString("ID_odbiorcy"));
                row.add(rs.getString("data_wyslania"));

                accounts.add(GbsMessage.implode(row, ";"));
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        reply.header = Integer.toString(Count);
        reply.arguments = accounts;

        return reply;
    }

    /**
     * Creates server response.
     *
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
            case "_createClass":
                response = _createClass();
                break;
            case "_removeClass":
                response = _removeClass();
                break;
            case "_listSchedulesTables":
                response = _listSchedulesTables();
                break;
            case "_listSchedule":
                response = _listSchedule();
                break;
            case "_listAccounts":
                response = _listAccounts();
                break;
            case "_listStudents":
                response = _listStudents();
                break;
            case "_listParents":
                response = _listParents();
                break;
            case "_listTeachers":
                response = _listTeachers();
                break;
            case "_listClasses":
                response = _listClasses();
                break;
            case "_listLessons":
                response = _listLessons();
                break;
            case "_listAttendances":
                response = _listAttendances();
                break;
            case "_listGrades":
                response = _listGrades();
                break;
            case "_listCourses":
                response = _listCourses();
                break;
            case "_listRemarks":
                response = _listRemarks();
                break;
            case "_listEvents":
                response = _listEvents();
                break;
            case "_listMessages":
                response = _listMessages();
                break;
            default:
                response = new GbsMessage("empty", new Vector<String>());
                ;
        }
        log("finished");
        GbsServer.log("Connection closed with " + socketAddress);
        return response;
    }
}
