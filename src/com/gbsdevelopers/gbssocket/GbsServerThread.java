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
    public GbsServerThread(String inputHeader, Vector<String> inputArguments, ObjectOutputStream toClient, SocketAddress socketAddress) {
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

        String query = "SELECT * FROM konta WHERE Login = '" + login + "' AND Haslo = '" + password + "';";
        Statement st;
        ResultSet rs;

        String loggedUser = "";
        String loggedPerms = "";
        String loggedID = "";
        try {
            st = GbsServer.conn.createStatement();
            rs = st.executeQuery(query);

            while (rs.next()) {
                loggedUser = rs.getString("Login");
                loggedPerms = rs.getString("Uprawnienia");
                loggedID = rs.getString("ID_Konta");
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
            replyArgs.add(loggedID);
            log("Logged user " + loggedUser + " with \"" + loggedPerms + "\" perms and ID == " + loggedID);
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

            GbsServer.conn = this.getConnection();

            GbsServer.isMySqlSet = true;
        } else {
            GbsServer.log("MySql credentials are previously set.");
        }
        return new GbsMessage("0", null);
    }

    /**
     * Handler for _manualQuery header.
     *
     * @return Reply message.
     */
    private GbsMessage _manualQuery() {
        GbsMessage reply = new GbsMessage();

        String query = inputArguments.get(0);

        Statement st;
        try {
            log("Executing " + query);
            st = GbsServer.conn.createStatement();
            st.execute(query);
        } catch (SQLException ex) {
            reply.arguments.add(ex.getMessage());
            ex.printStackTrace();
        }

        return reply;
    }

    /**
     * Handler for _createClass header.
     *
     * @return Reply message.
     */
    private GbsMessage _createClass() {
        GbsMessage reply = new GbsMessage();

        String className = inputArguments.get(0);

        Vector<String> queries = new Vector<String>();

        queries.add("INSERT INTO klasy VALUES(null, '" + className + "', " + inputArguments.get(1) + ");");
        queries.add("CREATE TABLE plan_" + className + " (Godzina int(11) NOT NULL, Poniedzialek int(4) DEFAULT NULL, Wtorek int(4) DEFAULT NULL, Sroda int(4) DEFAULT NULL, Czwartek int(4) DEFAULT NULL, Piatek int(4) DEFAULT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_polish_ci;");
        queries.add("ALTER TABLE plan_" + className + " ADD PRIMARY KEY (Godzina), ADD KEY Poniedzialek(Poniedzialek), ADD KEY Wtorek(Wtorek), ADD KEY Sroda(Sroda), ADD KEY Czwartek (Czwartek), ADD KEY Piatek (Piatek);");
        queries.add("ALTER TABLE plan_" + className + " ADD CONSTRAINT plan_" + className + "_ibfk_1 FOREIGN KEY (Poniedzialek) REFERENCES lekcje(ID_lekcji), ADD CONSTRAINT plan_" + className + "_ibfk_2 FOREIGN KEY(Wtorek) REFERENCES lekcje(ID_lekcji), ADD CONSTRAINT plan_" + className + "_ibfk_3 FOREIGN KEY(Sroda) REFERENCES lekcje(ID_lekcji), ADD CONSTRAINT plan_" + className + "_ibfk_4 FOREIGN KEY(Czwartek) REFERENCES lekcje(ID_lekcji), ADD CONSTRAINT plan_" + className + "_ibfk_5 FOREIGN KEY(Piatek) REFERENCES lekcje(ID_lekcji);");

        Statement st;

        for (String query : queries) {
            try {
                log("Executing " + query);
                st = GbsServer.conn.createStatement();
                st.execute(query);

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return reply;
    }

    /**
     * Handler for _removeClass header.
     *
     * @return Reply message.
     */
    private GbsMessage _removeClass() {
        GbsMessage reply = new GbsMessage();

        String className = inputArguments.get(0);
        String classID = "";

        String getIdQuery = "SELECT ID_klasy FROM klasy WHERE nazwa_klasy = '" + className + "';";
        Statement st;
        ResultSet rs;
        try {
            st = GbsServer.conn.createStatement();
            rs = st.executeQuery(getIdQuery);
            log("Executing " + getIdQuery);

            while (rs.next()) {
                classID = rs.getString("ID_klasy");
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        Vector<String> queries = new Vector<String>();

        queries.add("DELETE FROM uczniowie WHERE ID_klasy = " + classID + ";");
        queries.add("DELETE FROM lekcje WHERE ID_klasy = " + classID + ";");
        queries.add("DELETE FROM klasy WHERE ID_klasy = " + classID + ";");
        queries.add("DROP TABLE plan_" + className + ";");

        for (String query : queries) {
            try {
                log("Executing " + query);
                st = GbsServer.conn.createStatement();
                st.execute(query);

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return reply;
    }

    /**
     * Handler for _listSchedulesTables header.
     *
     * @return Reply message.
     */
    private GbsMessage _listSchedulesTables() {
        GbsMessage reply = new GbsMessage();

        String query = "SELECT CONCAT('plan_',nazwa_klasy) AS Plany FROM klasy ORDER BY Plany ASC;";
        Statement st;
        ResultSet rs;

        int classCount = 0;

        Vector<String> classes = new Vector<String>();

        try {
            st = GbsServer.conn.createStatement();
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

    /**
     * Handler for _listSchedule header.
     *
     * @return Reply message.
     */
    private GbsMessage _listSchedule() {
        GbsMessage reply = new GbsMessage();

        String tableName = inputArguments.get(0);

        String query = "SELECT * FROM " + tableName + ";";
        Statement st;
        ResultSet rs;

        int Count = 0;

        Vector<String> accounts = new Vector<String>();

        try {
            st = GbsServer.conn.createStatement();
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

    /**
     * Handler for _addStudent header.
     *
     * @return Reply message.
     */
    private GbsMessage _addStudent() {
        GbsMessage reply = new GbsMessage();

        String name = inputArguments.get(0);
        String surname = inputArguments.get(1);
        String classID = inputArguments.get(2);
        String studentPass = inputArguments.get(3);
        String parentPass = inputArguments.get(4);

        //zbuduj nickname (1 litera imienia + 3 litery nazwiska + id + r jak rodzic)

        StringBuilder sb = new StringBuilder();

        sb.append(name.substring(0, 1));
        sb.append(surname.substring(0, 3));

        String login = sb.toString().toLowerCase(Locale.ROOT);

        String studentID = "";
        String parentID = "";

        Statement st;
        ResultSet rs;

        //dodaj konto ucznia (losowy login)

        try {
            String query = "INSERT INTO konta VALUES (null, '"+login+"','"+studentPass+"','u')";
            log("Executing " + query);
            st = GbsServer.conn.createStatement();
            st.execute(query);
        } catch (SQLException ex) {
            reply.arguments.add(ex.getMessage());
            ex.printStackTrace();
        }

        //zapisz id konta ucznia

        try {
            String query = "SELECT ID_konta FROM konta WHERE Login = '"+login+"';";
            log("Executing " + query);
            st = GbsServer.conn.createStatement();
            rs = st.executeQuery(query);

            while (rs.next()) {
                studentID = rs.getString("ID_konta");
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        //zaaktualizuj login ucznia

        try {
            String query = "UPDATE konta SET Login = '"+login+studentID+"u' WHERE ID_konta = " + studentID + ";";
            log("Executing " + query);
            st = GbsServer.conn.createStatement();
            st.execute(query);
        } catch (SQLException ex) {
            reply.arguments.add(ex.getMessage());
            ex.printStackTrace();
        }

        //dodaj konto rodzica (losowy login)

        try {
            String query = "INSERT INTO konta VALUES (null, '"+login+"','"+parentPass+"','r')";
            log("Executing " + query);
            st = GbsServer.conn.createStatement();
            st.execute(query);
        } catch (SQLException ex) {
            reply.arguments.add(ex.getMessage());
            ex.printStackTrace();
        }

        //zapisz id konta rodzica

        try {
            String query = "SELECT ID_konta FROM konta WHERE Login = '"+login+"';";
            log("Executing " + query);
            st = GbsServer.conn.createStatement();
            rs = st.executeQuery(query);

            while (rs.next()) {
                parentID = rs.getString("ID_konta");
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        //zaaktualizuj login rodzica

        try {
            String query = "UPDATE konta SET Login = '"+login+parentID+"r' WHERE ID_konta = " + parentID + ";";
            log("Executing " + query);
            st = GbsServer.conn.createStatement();
            st.execute(query);
        } catch (SQLException ex) {
            reply.arguments.add(ex.getMessage());
            ex.printStackTrace();
        }

        //dodaj rodzica

        try {
            String query = "INSERT INTO rodzice VALUES(null,'"+name+"','"+surname+"',"+parentID+");";
            log("Executing " + query);
            st = GbsServer.conn.createStatement();
            st.execute(query);
        } catch (SQLException ex) {
            reply.arguments.add(ex.getMessage());
            ex.printStackTrace();
        }

        //zapisz id rodzica z tabeli rodzice

        try {
            String query = "SELECT ID_rodzica FROM rodzice WHERE ID_konta = "+parentID+";";
            log("Executing " + query);
            st = GbsServer.conn.createStatement();
            rs = st.executeQuery(query);

            while (rs.next()) {
                parentID = rs.getString("ID_rodzica");
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        //dodaj ucznia

        try {
            String query = "INSERT INTO uczniowie VALUES(null,'"+name+"','"+surname+"',"+parentID+","+classID+","+studentID+");";
            log("Executing " + query);
            st = GbsServer.conn.createStatement();
            st.execute(query);
        } catch (SQLException ex) {
            reply.arguments.add(ex.getMessage());
            ex.printStackTrace();
        }

        return reply;
    }

    /**
     * Handler for _removeStudent header.
     *
     * @return Reply message.
     */
    private GbsMessage _removeStudent() {
        GbsMessage reply = new GbsMessage();

        String studentID = inputArguments.get(0);

        Statement st;
        ResultSet rs;

        String studentAccountID = "";
        String parentAccountID = "";

        try {
            String query = "SELECT ID_konta FROM uczniowie WHERE ID_ucznia = "+studentID+";";
            log("Executing " + query);
            st = GbsServer.conn.createStatement();
            rs = st.executeQuery(query);

            while (rs.next()) {
                studentAccountID = rs.getString("ID_konta");
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        try {
            String query = "SELECT r.ID_konta AS ID_konta FROM uczniowie u, rodzice r WHERE u.ID_rodzica = r.ID_rodzica AND ID_ucznia = "+studentID+";";
            log("Executing " + query);
            st = GbsServer.conn.createStatement();
            rs = st.executeQuery(query);

            while (rs.next()) {
                parentAccountID = rs.getString("ID_konta");
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        try {
            String query = "DELETE FROM konta WHERE ID_konta IN ("+studentAccountID+","+parentAccountID+")";
            log("Executing " + query);
            st = GbsServer.conn.createStatement();
            st.execute(query);
        } catch (SQLException ex) {
            reply.arguments.add(ex.getMessage());
            ex.printStackTrace();
        }


        return reply;
    }

    /**
     * Handler for _addTeacher header.
     *
     * @return Reply message.
     */
    private GbsMessage _addTeacher() {
        GbsMessage reply = new GbsMessage();

        String name = inputArguments.get(0);
        String surname = inputArguments.get(1);
        String pass = inputArguments.get(2);
        String phone = inputArguments.get(3);

        StringBuilder sb = new StringBuilder();

        sb.append(name.substring(0,1));
        sb.append(surname.substring(0,3));

        String login = sb.toString().toLowerCase(Locale.ROOT);
        String teacherID = "";

        Statement st;
        ResultSet rs;

        try {
            String query = "INSERT INTO konta VALUES (null, '"+login+"','"+pass+"','n')";
            log("Executing " + query);
            st = GbsServer.conn.createStatement();
            st.execute(query);
        } catch (SQLException ex) {
            reply.arguments.add(ex.getMessage());
            ex.printStackTrace();
        }

        try {
            String query = "SELECT ID_konta FROM konta WHERE Login = '"+login+"';";
            log("Executing " + query);
            st = GbsServer.conn.createStatement();
            rs = st.executeQuery(query);

            while (rs.next()) {
                teacherID = rs.getString("ID_konta");
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        try {
            String query = "UPDATE konta SET Login = '"+login+teacherID+"n' WHERE ID_konta = " + teacherID + ";";
            log("Executing " + query);
            st = GbsServer.conn.createStatement();
            st.execute(query);
        } catch (SQLException ex) {
            reply.arguments.add(ex.getMessage());
            ex.printStackTrace();
        }

        try {
            String query = "INSERT INTO nauczyciele VALUES (null,'"+name+"','"+surname+"','"+phone+"',"+teacherID+");";
            log("Executing " + query);
            st = GbsServer.conn.createStatement();
            st.execute(query);
        } catch (SQLException ex) {
            reply.arguments.add(ex.getMessage());
            ex.printStackTrace();
        }


        return reply;
    }

    /**
     * Handler for _removeTeacher header.
     *
     * @return Reply message.
     */
    private GbsMessage _removeTeacher() {
        GbsMessage reply = new GbsMessage();

        String teacherID = inputArguments.get(0);

        Statement st;
        ResultSet rs;

        String accountID = "";

        try {
            String query = "SELECT ID_konta FROM nauczyciele WHERE ID_nauczyciela = "+teacherID+";";
            log("Executing " + query);
            st = GbsServer.conn.createStatement();
            rs = st.executeQuery(query);

            while (rs.next()) {
                accountID = rs.getString("ID_konta");
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        try {
            String query = "DELETE FROM konta WHERE ID_konta = "+accountID+";";
            log("Executing " + query);
            st = GbsServer.conn.createStatement();
            st.execute(query);
        } catch (SQLException ex) {
            reply.arguments.add(ex.getMessage());
            ex.printStackTrace();
        }

        return reply;
    }

    /**
     * Handler for _changeAttendance header.
     *
     * @return Reply message.
     */
    private GbsMessage _changeAttendance() {
        GbsMessage reply = new GbsMessage();

        String attendanceID = inputArguments.get(0);

        String newType = inputArguments.get(1);

        Statement st;

        try {
            String query = "UPDATE nieobecnosci SET TYP = '"+newType+"' WHERE ID_nieobecnosci = "+attendanceID+";";
            log("Executing " + query);
            st = GbsServer.conn.createStatement();
            st.execute(query);
        } catch (SQLException ex) {
            reply.arguments.add(ex.getMessage());
            ex.printStackTrace();
        }

        return reply;
    }

    /**
     * Handler for _listAccounts header.
     *
     * @return Reply message.
     */
    private GbsMessage _listAccounts() //dodaj where
    {
        GbsMessage reply = new GbsMessage();

        String query = "SELECT * FROM konta;";
        Statement st;
        ResultSet rs;

        int Count = 0;

        Vector<String> accounts = new Vector<String>();

        try {
            st = GbsServer.conn.createStatement();
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

    /**
     * Handler for _listStudents header.
     *
     * @return Reply message.
     */
    private GbsMessage _listStudents() //dodaj where
    {
        GbsMessage reply = new GbsMessage();

        String query = "SELECT u.ID_ucznia AS ID_ucznia, u.imie AS imie, u.nazwisko AS nazwisko, u.ID_rodzica AS ID_rodzica, u.ID_klasy AS ID_klasy, u.ID_konta AS ID_konta, k.nazwa_klasy FROM uczniowie AS u, klasy AS k WHERE u.ID_klasy = k.ID_klasy ORDER BY k.nazwa_klasy ASC, u.nazwisko ASC, u.imie ASC;";
        Statement st;
        ResultSet rs;

        int Count = 0;

        Vector<String> accounts = new Vector<String>();

        try {
            st = GbsServer.conn.createStatement();
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

    /**
     * Handler for _listParents header.
     *
     * @return Reply message.
     */
    private GbsMessage _listParents() //dodaj where
    {
        GbsMessage reply = new GbsMessage();

        String query = "SELECT * FROM rodzice ORDER BY nazwisko ASC, imie ASC;";
        Statement st;
        ResultSet rs;

        int Count = 0;

        Vector<String> accounts = new Vector<String>();

        try {
            st = GbsServer.conn.createStatement();
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

    /**
     * Handler for _listTeachers header.
     *
     * @return Reply message.
     */
    private GbsMessage _listTeachers() //dodaj where
    {
        GbsMessage reply = new GbsMessage();

        String query = "SELECT * FROM nauczyciele ORDER BY nazwisko ASC, imie ASC;";
        Statement st;
        ResultSet rs;

        int Count = 0;

        Vector<String> accounts = new Vector<String>();

        try {
            st = GbsServer.conn.createStatement();
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

    /**
     * Handler for _listClasses header.
     *
     * @return Reply message.
     */
    private GbsMessage _listClasses() //dodaj where
    {
        GbsMessage reply = new GbsMessage();

        String query = "SELECT * FROM klasy ORDER BY nazwa_klasy ASC;";
        Statement st;
        ResultSet rs;

        int Count = 0;

        Vector<String> accounts = new Vector<String>();

        try {
            st = GbsServer.conn.createStatement();
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

    /**
     * Handler for _listLessons header.
     *
     * @return Reply message.
     */
    private GbsMessage _listLessons() //dodaj where
    {
        GbsMessage reply = new GbsMessage();

        String query = "SELECT * FROM lekcje ORDER BY Sala ASC;";
        Statement st;
        ResultSet rs;

        int Count = 0;

        Vector<String> accounts = new Vector<String>();

        try {
            st = GbsServer.conn.createStatement();
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

    /**
     * Handler for _listAttendances header.
     *
     * @return Reply message.
     */
    private GbsMessage _listAttendances() //dodaj where
    {
        GbsMessage reply = new GbsMessage();

        String query = "SELECT * FROM nieobecnosci ORDER BY data_nieobecnosci DESC;";
        Statement st;
        ResultSet rs;

        int Count = 0;

        Vector<String> accounts = new Vector<String>();

        try {
            st = GbsServer.conn.createStatement();
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

    /**
     * Handler for _listGrades header.
     *
     * @return Reply message.
     */
    private GbsMessage _listGrades() //dodaj where
    {
        GbsMessage reply = new GbsMessage();

        String query = "SELECT * FROM oceny ORDER BY data_wystawienia DESC;";
        Statement st;
        ResultSet rs;

        int Count = 0;

        Vector<String> accounts = new Vector<String>();

        try {
            st = GbsServer.conn.createStatement();
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

    /**
     * Handler for _listCourses header.
     *
     * @return Reply message.
     */
    private GbsMessage _listCourses() //dodaj where
    {
        GbsMessage reply = new GbsMessage();

        String query = "SELECT * FROM przedmioty ORDER BY nazwa_przedmiotu ASC;";
        Statement st;
        ResultSet rs;

        int Count = 0;

        Vector<String> accounts = new Vector<String>();

        try {
            st = GbsServer.conn.createStatement();
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

    /**
     * Handler for _listRemarks header.
     *
     * @return Reply message.
     */
    private GbsMessage _listRemarks() //dodaj where
    {
        GbsMessage reply = new GbsMessage();

        String query = "SELECT * FROM uwagi ORDER BY data_wystawienia DESC;";
        Statement st;
        ResultSet rs;

        int Count = 0;

        Vector<String> accounts = new Vector<String>();

        try {
            st = GbsServer.conn.createStatement();
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

    /**
     * Handler for _listEvents header.
     *
     * @return Reply message.
     */
    private GbsMessage _listEvents() //dodaj where
    {
        GbsMessage reply = new GbsMessage();

        String query = "SELECT * FROM wydarzenia ORDER BY data_wydarzenia ASC;";
        Statement st;
        ResultSet rs;

        int Count = 0;

        Vector<String> accounts = new Vector<String>();

        try {
            st = GbsServer.conn.createStatement();
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

    /**
     * Handler for _listMessages header.
     *
     * @return Reply message.
     */
    private GbsMessage _listMessages() //dodaj where
    {
        GbsMessage reply = new GbsMessage();

        String query = "SELECT * FROM wiadomosci ORDER BY data_wyslania DESC;";
        Statement st;
        ResultSet rs;

        int Count = 0;

        Vector<String> accounts = new Vector<String>();

        try {
            st = GbsServer.conn.createStatement();
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
            case "_manualQuery":
                response = _manualQuery();
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
            case "_addStudent":
                response = _addStudent();
                break;
            case "_addTeacher":
                response = _addTeacher();
                break;
            case "_removeStudent":
                response = _removeStudent();
                break;
            case "_removeTeacher":
                response = _removeTeacher();
                break;
            case "_changeAttendance":
                response = _changeAttendance();
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
