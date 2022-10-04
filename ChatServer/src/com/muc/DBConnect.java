package com.muc;
import java.sql.*;

public class DBConnect implements I_DB {
    public static void main(String[] args) {
        DBConnect database = new DBConnect();
        database.connection();
    }

    @Override
    public Connection connection() {
        Connection connection = null;
        try{
            Class.forName("org.sqlite.JDBC");
            String url = "jdbc:sqlite:message.db";
            connection = DriverManager.getConnection(url);
        }catch(ClassNotFoundException | SQLException exception){
            exception.printStackTrace();

        }

        return connection;
    }

    @Override
    public ResultSet read(PreparedStatement ps, Connection connection) {
        ResultSet resultSet = null;
        try{
            System.out.println(ps.toString());
            resultSet = ps.executeQuery();

        }catch(SQLException e){
            e.printStackTrace();

        }
        return resultSet;
    }

    @Override
    public void CloseConnection(Connection connection) {
        try{
            connection.close();
        }catch(SQLException e){
            e.printStackTrace();
        }

    }

    @Override
    public int write(PreparedStatement ps, Connection connection) {
        try{
            ps.executeUpdate();
        }catch(SQLException e ){
            e.printStackTrace();
        }
        return 1;
    }
    public DBConnect(){};
}
