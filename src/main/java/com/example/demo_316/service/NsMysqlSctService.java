package com.example.demo_316.service;

import com.example.demo_316.model.NsMysqlSct;
import com.example.demo_316.dto.NsMysqlSctDto;
import com.example.demo_316.dto.ResponseStatusDto;
import com.example.demo_316.dto.SqlCommandDto;
import com.example.demo_316.exception.CustomException;
import com.example.demo_316.mapper.NsMysqlSctMapper;
import com.example.demo_316.repository.NsMysqlSctRepository;
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
public class NsMysqlSctService {
    DistributedTransactionManager manager;
    SqlSessionFactory sqlSessionFactory;

    @Autowired
    NsMysqlSctRepository sctRepository;

    public NsMysqlSctService(DistributedTransactionManager manager, SqlSessionFactory sqlSessionFactory) throws InstantiationException, IllegalAccessException {
        this.manager = manager;
        this.sqlSessionFactory = sqlSessionFactory;
    }

    // Execute SQL Command
    public List<NsMysqlSctDto> executeSQL(SqlCommandDto sqlCommandDto) throws CustomException {
        SqlSession sqlSession = null;

        try {
            sqlSession = sqlSessionFactory.createSqlSession();
            ExecuteSqlUtil<NsMysqlSct> executeSql = new ExecuteSqlUtil<>(NsMysqlSct.class);

            // Begin a transaction
            sqlSession.begin();

            List<NsMysqlSct> sctList = executeSql.executeSQL(sqlSession, sqlCommandDto.getSqlCommand());

            sqlSession.commit();
            return NsMysqlSctMapper.mapToNsMysqlSctDtoList(sctList);
        } catch (Exception e) {
            handleSqlSessionException(e, sqlSession);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Create Record
    public ResponseStatusDto postNsMysqlSct(NsMysqlSctDto sctDto) throws CustomException {
        return postNsMysqlSct(sctDto, false);
    }
    
    public ResponseStatusDto postNsMysqlSct(NsMysqlSctDto sctDto, boolean isOO) throws CustomException {
        DistributedTransaction transaction = null;
        try {
            NsMysqlSct sct = NsMysqlSctMapper.mapToNsMysqlSct(sctDto);
            if(isOO) {
                sct = sctRepository.postNsMysqlSctOO(manager, sct);
            }
            else {
                transaction = manager.start();
//            transaction = manager.beginReadOnly();

                sct = sctRepository.postNsMysqlSct(transaction, sct);
                transaction.commit();
            }

            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Upsert Record
    public ResponseStatusDto upsertNsMysqlSct(NsMysqlSctDto sctDto) throws CustomException {
        return upsertNsMysqlSct(sctDto, false);
    }
    
    public ResponseStatusDto upsertNsMysqlSct(NsMysqlSctDto sctDto, boolean isOO) throws CustomException {
        DistributedTransaction transaction = null;
        try {
            NsMysqlSct sct = NsMysqlSctMapper.mapToNsMysqlSct(sctDto);
            if(isOO) {
                sct = sctRepository.upsertNsMysqlSctOO(manager, sct);
            }
            else {
                transaction = manager.start();
                sct = sctRepository.upsertNsMysqlSct(transaction, sct);
                transaction.commit();
            }

            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Retrieve Record
    public NsMysqlSctDto getNsMysqlSct(NsMysqlSctDto sctDto) throws CustomException {
        return getNsMysqlSct(sctDto, false);
    }
    
    public NsMysqlSctDto getNsMysqlSct(NsMysqlSctDto sctDto, boolean isOO) throws CustomException {
        DistributedTransaction transaction = null;
        try {
            NsMysqlSct sct = NsMysqlSctMapper.mapToNsMysqlSct(sctDto);
            if(isOO) {
                sct = sctRepository.getNsMysqlSctOO(manager, sct);
            }
            else {
                transaction = manager.start();
//            transaction = manager.beginReadOnly();

                sct = sctRepository.getNsMysqlSct(transaction, sct);
                transaction.commit();
            }

            return NsMysqlSctMapper.mapToNsMysqlSctDto(sct);
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Update Record
    public ResponseStatusDto putNsMysqlSct(NsMysqlSctDto sctDto) throws CustomException {
        return putNsMysqlSct(sctDto, false);
    }
    
    public ResponseStatusDto putNsMysqlSct(NsMysqlSctDto sctDto, boolean isOO) throws CustomException {
        DistributedTransaction transaction = null;
        try {
            NsMysqlSct sct = NsMysqlSctMapper.mapToNsMysqlSct(sctDto);

            if(isOO) {
                sct = sctRepository.putNsMysqlSctOO(manager, sct);
            }
            else {
                transaction = manager.start();

                sct = sctRepository.putNsMysqlSct(transaction, sct);
                transaction.commit();
            }
            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Delete Record
    public ResponseStatusDto deleteNsMysqlSct(NsMysqlSctDto sctDto) throws CustomException {
        return deleteNsMysqlSct(sctDto, false);
    }
    
    public ResponseStatusDto deleteNsMysqlSct(NsMysqlSctDto sctDto, boolean isOO) throws CustomException {
        DistributedTransaction transaction = null;
        try {
            NsMysqlSct sct = NsMysqlSctMapper.mapToNsMysqlSct(sctDto);

            if(isOO) {
                sctRepository.deleteNsMysqlSctOO(manager, sct);
            }
            else {
                transaction = manager.start();
                sctRepository.deleteNsMysqlSct(transaction, sct);
                transaction.commit();
            }

            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Retrieve All Records
    public List<NsMysqlSctDto> getNsMysqlSctListAll() throws CustomException {
        DistributedTransaction transaction = null;
        List<NsMysqlSct> sctList = new ArrayList<>();
        try {
            transaction = manager.start();
            sctList = sctRepository.getNsMysqlSctListAll(transaction);
            return NsMysqlSctMapper.mapToNsMysqlSctDtoList(sctList);
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Retrieve All Records
    public List<NsMysqlSctDto> getNsMysqlSctListAllWithReadOnly() throws CustomException {
        DistributedTransaction transaction = null;
        List<NsMysqlSct> sctList = new ArrayList<>();
        try {
            transaction = manager.beginReadOnly();
            sctList = sctRepository.getNsMysqlSctListAll(transaction);
            return NsMysqlSctMapper.mapToNsMysqlSctDtoList(sctList);
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Retrieve Records by Partition Key
    public List<NsMysqlSctDto> getNsMysqlSctListByPk(NsMysqlSctDto sctDto) throws CustomException {
        DistributedTransaction transaction = null;
        List<NsMysqlSct> sctList = new ArrayList<>();
        try {
            NsMysqlSct sct = NsMysqlSctMapper.mapToNsMysqlSct(sctDto);
            Key partitionKey = sct.getPartitionKey();
            transaction = manager.start();
            sctList = sctRepository.getNsMysqlSctListByPk(transaction, partitionKey);
            transaction.commit();
            return NsMysqlSctMapper.mapToNsMysqlSctDtoList(sctList);
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
        if (e.getClass().getSimpleName().equals("CommitConflictException")) return 9150;
        if (e instanceof TransactionException) return 9300;
        if (e instanceof RuntimeException) return 9400;
        return 9500;
    }
}