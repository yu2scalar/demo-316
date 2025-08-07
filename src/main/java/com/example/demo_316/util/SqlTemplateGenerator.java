package com.example.demo_316.util;

import com.example.demo_316.model.NsMysqlSct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class SqlTemplateGenerator {
    
    /**
     * Generates SQL templates based on NsMysqlSct model structure
     * Templates use placeholders: ${pk}, ${ck}, ${threadId}, ${operationIndex}
     */
    public static class SqlTemplates {
        
        // INSERT template with parameter placeholders
        public static final String INSERT = 
            "INSERT INTO " + NsMysqlSct.NAMESPACE + "." + NsMysqlSct.TABLE + 
            " (" + NsMysqlSct.PK + ", " + NsMysqlSct.CK + ", " + NsMysqlSct.STRING_VALUE + ", " + NsMysqlSct.BINT_VALUE + ") " +
            "VALUES (${pk}, ${ck}, 'SqlLoadTest_T${threadId}_${operationIndex}', ${bintValue})";
            
        // SELECT by PK and CK
        public static final String SELECT_BY_PK_CK = 
            "SELECT " + NsMysqlSct.PK + ", " + NsMysqlSct.CK + ", " + NsMysqlSct.STRING_VALUE + ", " + NsMysqlSct.BINT_VALUE + 
            " FROM " + NsMysqlSct.NAMESPACE + "." + NsMysqlSct.TABLE + 
            " WHERE " + NsMysqlSct.PK + " = ${pk} AND " + NsMysqlSct.CK + " = ${ck}";
            
        // SELECT by PK only (scan)
        public static final String SELECT_BY_PK = 
            "SELECT " + NsMysqlSct.PK + ", " + NsMysqlSct.CK + ", " + NsMysqlSct.STRING_VALUE + ", " + NsMysqlSct.BINT_VALUE + 
            " FROM " + NsMysqlSct.NAMESPACE + "." + NsMysqlSct.TABLE + 
            " WHERE " + NsMysqlSct.PK + " = ${pk}";
            
        // UPDATE with parameter placeholders
        public static final String UPDATE = 
            "UPDATE " + NsMysqlSct.NAMESPACE + "." + NsMysqlSct.TABLE + 
            " SET " + NsMysqlSct.STRING_VALUE + " = 'SqlUpdated_T${threadId}_${operationIndex}', " +
            NsMysqlSct.BINT_VALUE + " = ${bintValue} " +
            "WHERE " + NsMysqlSct.PK + " = ${pk} AND " + NsMysqlSct.CK + " = ${ck}";
            
        // DELETE template
        public static final String DELETE = 
            "DELETE FROM " + NsMysqlSct.NAMESPACE + "." + NsMysqlSct.TABLE + 
            " WHERE " + NsMysqlSct.PK + " = ${pk} AND " + NsMysqlSct.CK + " = ${ck}";
    }
    
    /**
     * Resolves parameter placeholders in SQL templates
     * 
     * @param template SQL template with ${parameter} placeholders
     * @param pk Partition key value
     * @param ck Clustering key value  
     * @param threadId Thread identifier
     * @param operationIndex Operation index within thread
     * @return SQL with resolved parameters
     */
    public static String resolveParameters(String template, Integer pk, Integer ck, 
                                         int threadId, int operationIndex) {
        // Calculate bintValue using same formula as LoadTestService
        long bintValue = threadId * 1000000L + operationIndex;
        
        return template
            .replace("${pk}", String.valueOf(pk))
            .replace("${ck}", String.valueOf(ck))
            .replace("${threadId}", String.valueOf(threadId))
            .replace("${operationIndex}", String.valueOf(operationIndex))
            .replace("${bintValue}", String.valueOf(bintValue));
    }
    
    /**
     * Gets all available SQL templates for testing
     */
    public static List<String> getAllTemplates() {
        List<String> templates = new ArrayList<>();
        templates.add(SqlTemplates.INSERT);
        templates.add(SqlTemplates.SELECT_BY_PK_CK);
        templates.add(SqlTemplates.SELECT_BY_PK);
        templates.add(SqlTemplates.UPDATE);
        templates.add(SqlTemplates.DELETE);
        return templates;
    }
    
    /**
     * Gets template by operation name
     */
    public static String getTemplateByOperation(String operation) {
        switch (operation.toUpperCase()) {
            case "INSERT":
                return SqlTemplates.INSERT;
            case "SELECT":
            case "SELECT_BY_PK_CK":
                return SqlTemplates.SELECT_BY_PK_CK;
            case "SELECT_BY_PK":
                return SqlTemplates.SELECT_BY_PK;
            case "UPDATE":
                return SqlTemplates.UPDATE;
            case "DELETE":
                return SqlTemplates.DELETE;
            default:
                throw new IllegalArgumentException("Unknown operation: " + operation);
        }
    }
}