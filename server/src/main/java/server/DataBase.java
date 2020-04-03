package server;

import java.sql.*;

public class DataBase {

    private static Connection connection;
    private static Statement statement;
    private static PreparedStatement createUserStatement;
    private static PreparedStatement getUserNicknameStatement;
    private static PreparedStatement changeUserNicknameStatement;
    private static PreparedStatement deleteUserStatement;

    public static boolean connect() throws ClassNotFoundException, SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            System.out.println("Драйвер подключен");
            String url = "jdbc:sqlite:chat.db";
            connection = DriverManager.getConnection(url);
            System.out.println("Соединение c Базой Данных установлено");
            statement = connection.createStatement();
            createUserTable();
            prepareAllStatement();


       } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        } finally {
            return false;
        }

    }

    static void createUserTable()  {
        try{
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS users (\n" +
                    "    id       INTEGER      PRIMARY KEY AUTOINCREMENT" +
                    "                          NOT NULL" +
                    "                          UNIQUE," +
                    "    login    TEXT    UNIQUE\n" +
                    "                     NOT NULL,\n" +
                    "    parol    TEXT    NOT NULL,\n" +
                    "    nickName TEXT\n" +
                    ");"
            );
            System.out.println("Таблица users создана");
        }catch (Exception e){
            e.printStackTrace();
            System.err.println("ERROR");
        }
    }

    public static void disconnection() {
        try {
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Невозможно подключиться к базе");
        }
    }

    public static void prepareAllStatement() throws SQLException {
        createUserStatement = connection.prepareStatement("INSERT INTO users (login, parol, nickname) VALUES (?, ?, ?);");
        getUserNicknameStatement = connection.prepareStatement("SELECT nickname FROM users WHERE login = ? AND parol = ?;");
        changeUserNicknameStatement = connection.prepareStatement("UPDATE users SET nickname = ? WHERE nickname = ?;");
        deleteUserStatement = connection.prepareStatement("DELETE FROM users WHERE login = ?;");
    }

    public static boolean createUser(String login, String password, String nickname) {
        try {
            createUserStatement.setString(1, login);
            createUserStatement.setString(2, password);
            createUserStatement.setString(3, nickname);
            createUserStatement.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public static String getUserNickName(String login, String password){
        String nickName = null;
        try {
            getUserNicknameStatement.setString(1, login);
            getUserNicknameStatement.setString(2, password);
            ResultSet rs = getUserNicknameStatement.executeQuery();
            if (rs.next()){
                nickName = rs.getString(1);
            }
            rs.close();
        }catch (SQLException e){
            e.printStackTrace();
        }
        return nickName;
    }

    public static boolean changeUserNickname(String currentNickname, String newNickname) {
        try {
            changeUserNicknameStatement.setString(1, newNickname);
            changeUserNicknameStatement.setString(2, currentNickname);
            changeUserNicknameStatement.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public static boolean deleteUser(String login) {
        try {
            deleteUserStatement.setString(1, login);
            deleteUserStatement.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}