package com.example.demo_316.service;

import com.example.demo_316.dto.SqlCommandDto;
import com.example.demo_316.exception.CustomException;
import com.example.demo_316.util.GenericSqlUtil;
import com.scalar.db.exception.transaction.*;
import com.scalar.db.sql.SqlSession;
import com.scalar.db.sql.SqlSessionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GenericSqlService {
    SqlSessionFactory sqlSessionFactory;

    public GenericSqlService(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    // Execute SQL Command (Generic - returns Map) - defaults to isOO = false for backward compatibility (explicit transactions)
    public List<Map<String, Object>> executeSQLGeneric(SqlCommandDto sqlCommandDto) throws CustomException {
        return executeSQLGeneric(sqlCommandDto, false);
    }

    // Execute SQL Command (Generic - returns Map) with isOO parameter
    public List<Map<String, Object>> executeSQLGeneric(SqlCommandDto sqlCommandDto, boolean isOO) throws CustomException {
        SqlSession sqlSession = null;

        try {
            sqlSession = sqlSessionFactory.createSqlSession();
            GenericSqlUtil genericSqlUtil = new GenericSqlUtil(sqlSession);

            List<Map<String, Object>> resultList;
            
            if (isOO) {
                // isOO = true: Execute without explicit transaction management (OO mode)
                resultList = genericSqlUtil.executeQuery(sqlCommandDto.getSqlCommand());
            } else {
                // isOO = false: Use explicit transaction management (standard mode)
                sqlSession.begin();
                resultList = genericSqlUtil.executeQuery(sqlCommandDto.getSqlCommand());
                sqlSession.commit();
            }
            
            return resultList;
        } catch (Exception e) {
            handleSqlSessionException(e, sqlSession, isOO);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    private void handleSqlSessionException(Exception e, SqlSession sqlSession) {
        handleSqlSessionException(e, sqlSession, false); // Default to standard mode (explicit transactions)
    }

    private void handleSqlSessionException(Exception e, SqlSession sqlSession, boolean isOO) {
        log.error(e.getMessage(), e);
        if (sqlSession != null && !isOO) {
            // Only attempt rollback if we're in standard mode (transaction was explicitly started)
            try {
                sqlSession.rollback();
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
            }
        }
    }

    private int determineErrorCode(Exception e) {
        if (e instanceof UnsatisfiedConditionException) return 9100;
        if (e instanceof UnknownTransactionStatusException) return 9200;
        if (e instanceof TransactionException) return 9300;
        if (e instanceof RuntimeException) return 9400;
        return 9500;
    }
}
