package com.example.demo_316.service;

import com.example.demo_316.config.ScalarDbJdbcConfig;
import com.example.demo_316.dto.NsMysqlSctDto;
import com.example.demo_316.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NsMysqlSctJdbcService {
    
    private final ScalarDbJdbcConfig jdbcConfig;
    
    private static final String INSERT_SQL = "INSERT INTO sct (pk, ck, string_value, bint_value) VALUES (?, ?, ?, ?)";
    private static final String SELECT_SQL = "SELECT pk, ck, string_value, bint_value FROM sct WHERE pk = ? AND ck = ?";
    private static final String SELECT_BY_PK_SQL = "SELECT pk, ck, string_value, bint_value FROM sct WHERE pk = ?";
    private static final String UPDATE_SQL = "UPDATE sct SET string_value = ?, bint_value = ? WHERE pk = ? AND ck = ?";
    private static final String DELETE_SQL = "DELETE FROM sct WHERE pk = ? AND ck = ?";
    
    public NsMysqlSctDto insertNsMysqlSct(NsMysqlSctDto dto) throws CustomException {
        try (Connection connection = jdbcConfig.createConnection();
             PreparedStatement stmt = connection.prepareStatement(INSERT_SQL)) {
            
            stmt.setInt(1, dto.getPk());
            stmt.setInt(2, dto.getCk());
            stmt.setString(3, dto.getStringValue());
            stmt.setLong(4, dto.getBintValue());
            
            stmt.executeUpdate();
            connection.commit();
            
            return dto;
        } catch (SQLException e) {
            throw mapSqlException(e, "INSERT");
        }
    }
    
    public NsMysqlSctDto selectNsMysqlSct(NsMysqlSctDto dto) throws CustomException {
        try (Connection connection = jdbcConfig.createConnection();
             PreparedStatement stmt = connection.prepareStatement(SELECT_SQL)) {
            
            stmt.setInt(1, dto.getPk());
            stmt.setInt(2, dto.getCk());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return NsMysqlSctDto.builder()
                        .pk(rs.getInt("pk"))
                        .ck(rs.getInt("ck"))
                        .stringValue(rs.getString("string_value"))
                        .bintValue(rs.getLong("bint_value"))
                        .build();
                } else {
                    throw new CustomException(9404, "Record not found");
                }
            }
        } catch (SQLException e) {
            throw mapSqlException(e, "SELECT");
        }
    }
    
    public List<NsMysqlSctDto> selectNsMysqlSctListByPk(NsMysqlSctDto dto) throws CustomException {
        List<NsMysqlSctDto> results = new ArrayList<>();
        
        try (Connection connection = jdbcConfig.createConnection();
             PreparedStatement stmt = connection.prepareStatement(SELECT_BY_PK_SQL)) {
            
            stmt.setInt(1, dto.getPk());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(NsMysqlSctDto.builder()
                        .pk(rs.getInt("pk"))
                        .ck(rs.getInt("ck"))
                        .stringValue(rs.getString("string_value"))
                        .bintValue(rs.getLong("bint_value"))
                        .build());
                }
            }
        } catch (SQLException e) {
            throw mapSqlException(e, "SELECT_BY_PK");
        }
        
        return results;
    }
    
    public NsMysqlSctDto updateNsMysqlSct(NsMysqlSctDto dto) throws CustomException {
        try (Connection connection = jdbcConfig.createConnection();
             PreparedStatement stmt = connection.prepareStatement(UPDATE_SQL)) {
            
            stmt.setString(1, dto.getStringValue());
            stmt.setLong(2, dto.getBintValue());
            stmt.setInt(3, dto.getPk());
            stmt.setInt(4, dto.getCk());
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new CustomException(9404, "Record not found for update");
            }
            
            connection.commit();
            return dto;
        } catch (SQLException e) {
            throw mapSqlException(e, "UPDATE");
        }
    }
    
    public void deleteNsMysqlSct(NsMysqlSctDto dto) throws CustomException {
        try (Connection connection = jdbcConfig.createConnection();
             PreparedStatement stmt = connection.prepareStatement(DELETE_SQL)) {
            
            stmt.setInt(1, dto.getPk());
            stmt.setInt(2, dto.getCk());
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new CustomException(9404, "Record not found for delete");
            }
            
            connection.commit();
        } catch (SQLException e) {
            throw mapSqlException(e, "DELETE");
        }
    }
    
    private CustomException mapSqlException(SQLException e, String operation) {
        String message = e.getMessage();
        int errorCode;
        
        if (message != null) {
            if (message.contains("already exists") || message.contains("duplicate")) {
                errorCode = 9409;
            } else if (message.contains("not found")) {
                errorCode = 9404;
            } else if (message.contains("timeout") || message.contains("connection")) {
                errorCode = 9503;
            } else {
                errorCode = 9500;
            }
        } else {
            errorCode = 9500;
        }
        
        return new CustomException(errorCode, operation + " failed: " + (message != null ? message : "Unknown SQL error"));
    }
}