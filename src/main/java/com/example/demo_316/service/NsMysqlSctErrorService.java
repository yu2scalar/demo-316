package com.example.demo_316.service;

import com.example.demo_316.model.NsMysqlSctError;
import com.example.demo_316.dto.NsMysqlSctErrorDto;
import com.example.demo_316.dto.ResponseStatusDto;
import com.example.demo_316.dto.SqlCommandDto;
import com.example.demo_316.exception.CustomException;
import com.example.demo_316.mapper.NsMysqlSctErrorMapper;
import com.example.demo_316.repository.NsMysqlSctErrorRepository;
import com.example.demo_316.util.ExecuteSqlUtil;
import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.DistributedTransactionManager;
import com.scalar.db.io.Key;
import com.scalar.db.exception.transaction.*;
import com.scalar.db.sql.SqlSession;
import com.scalar.db.sql.SqlSessionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class NsMysqlSctErrorService {
    DistributedTransactionManager manager;
    SqlSessionFactory sqlSessionFactory;

    @Autowired
    NsMysqlSctErrorRepository sctErrorRepository;

    public NsMysqlSctErrorService(DistributedTransactionManager manager, SqlSessionFactory sqlSessionFactory) throws InstantiationException, IllegalAccessException {
        this.manager = manager;
        this.sqlSessionFactory = sqlSessionFactory;
    }

    // Execute SQL Command
    public List<NsMysqlSctErrorDto> executeSQL(SqlCommandDto sqlCommandDto) throws CustomException {
        SqlSession sqlSession = null;

        try {
            sqlSession = sqlSessionFactory.createSqlSession();
            ExecuteSqlUtil<NsMysqlSctError> executeSql = new ExecuteSqlUtil<>(NsMysqlSctError.class);

            // Begin a transaction
            sqlSession.begin();

            List<NsMysqlSctError> sctErrorList = executeSql.executeSQL(sqlSession, sqlCommandDto.getSqlCommand());

            sqlSession.commit();
            return NsMysqlSctErrorMapper.mapToNsMysqlSctErrorDtoList(sctErrorList);
        } catch (Exception e) {
            handleSqlSessionException(e, sqlSession);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Create Record
    public ResponseStatusDto postNsMysqlSctError(NsMysqlSctErrorDto sctErrorDto) throws CustomException {
        DistributedTransaction transaction = null;
        try {
            NsMysqlSctError sctError = NsMysqlSctErrorMapper.mapToNsMysqlSctError(sctErrorDto);
            transaction = manager.start();
            sctError = sctErrorRepository.postNsMysqlSctError(transaction, sctError);
            transaction.commit();
            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Upsert Record
    public ResponseStatusDto upsertNsMysqlSctError(NsMysqlSctErrorDto sctErrorDto) throws CustomException {
        DistributedTransaction transaction = null;
        try {
            NsMysqlSctError sctError = NsMysqlSctErrorMapper.mapToNsMysqlSctError(sctErrorDto);
            transaction = manager.start();
            sctError = sctErrorRepository.upsertNsMysqlSctError(transaction, sctError);
            transaction.commit();
            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Retrieve Record
    public NsMysqlSctErrorDto getNsMysqlSctError(NsMysqlSctErrorDto sctErrorDto) throws CustomException {
        DistributedTransaction transaction = null;
        try {
            NsMysqlSctError sctError = NsMysqlSctErrorMapper.mapToNsMysqlSctError(sctErrorDto);
            transaction = manager.start();
            sctError = sctErrorRepository.getNsMysqlSctError(transaction, sctError);
            transaction.commit();
            return NsMysqlSctErrorMapper.mapToNsMysqlSctErrorDto(sctError);
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Update Record
    public ResponseStatusDto putNsMysqlSctError(NsMysqlSctErrorDto sctErrorDto) throws CustomException {
        DistributedTransaction transaction = null;
        try {
            NsMysqlSctError sctError = NsMysqlSctErrorMapper.mapToNsMysqlSctError(sctErrorDto);
            transaction = manager.start();
            sctError = sctErrorRepository.putNsMysqlSctError(transaction, sctError);
            transaction.commit();
            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Delete Record
    public ResponseStatusDto deleteNsMysqlSctError(NsMysqlSctErrorDto sctErrorDto) throws CustomException {
        DistributedTransaction transaction = null;
        try {
            NsMysqlSctError sctError = NsMysqlSctErrorMapper.mapToNsMysqlSctError(sctErrorDto);
//            Key partitionKey = sctError.getPartitionKey();
//            Key clusteringKey = sctError.getClusteringKey();
            transaction = manager.start();
            sctErrorRepository.deleteNsMysqlSctError(transaction, sctError);
            transaction.commit();
            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Retrieve All Records
    public List<NsMysqlSctErrorDto> getNsMysqlSctErrorListAll() throws CustomException {
        DistributedTransaction transaction = null;
        List<NsMysqlSctError> sctErrorList = new ArrayList<>();
        try {
            transaction = manager.start();
            sctErrorList = sctErrorRepository.getNsMysqlSctErrorListAll(transaction);
            return NsMysqlSctErrorMapper.mapToNsMysqlSctErrorDtoList(sctErrorList);
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Retrieve Records by Partition Key
    public List<NsMysqlSctErrorDto> getNsMysqlSctErrorListByPk(NsMysqlSctErrorDto sctErrorDto) throws CustomException {
        DistributedTransaction transaction = null;
        List<NsMysqlSctError> sctErrorList = new ArrayList<>();
        try {
            NsMysqlSctError sctError = NsMysqlSctErrorMapper.mapToNsMysqlSctError(sctErrorDto);
            Key partitionKey = sctError.getPartitionKey();
            transaction = manager.start();
            sctErrorList = sctErrorRepository.getNsMysqlSctErrorListByPk(transaction, partitionKey);
            transaction.commit();
            return NsMysqlSctErrorMapper.mapToNsMysqlSctErrorDtoList(sctErrorList);
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    private void handleTransactionException(Exception e, DistributedTransaction transaction) {
        log.error(e.getMessage(), e);
        if (transaction != null) {
            try {
                transaction.rollback();
            } catch (RollbackException ex) {
                log.error(ex.getMessage(), ex);
            }
        }
    }

    private void handleSqlSessionException(Exception e, SqlSession sqlSession) {
        log.error(e.getMessage(), e);
        if (sqlSession != null) {
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