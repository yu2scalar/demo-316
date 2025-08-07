package com.example.demo_316.config;

import org.springframework.context.annotation.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Configuration
public class ScalarDbJdbcConfig {

    public Connection createConnection() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:scalardb:scalardb_sql.properties");
        connection.setAutoCommit(false);
        return connection;
    }
}