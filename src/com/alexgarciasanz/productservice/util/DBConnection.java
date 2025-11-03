package com.alexgarciasanz.productservice.util;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Gestor de la conexión JDBC.
 * Utiliza la base de datos Exam.
 */
public class DBConnection {

    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/exam";
    private static final String JDBC_USER = "root"; 
    private static final String JDBC_PASSWORD = "admin"; 

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("No se pudo encontrar el driver de MySQL", e);
        }
    }

    /**
     * @return una conexión JDBC
     * @throws SQLException si hay un error al conectar
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
    }
}