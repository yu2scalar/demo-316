package com.example.demo_316.repository;

import com.example.demo_316.model.NsMysqlSctError;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.scalar.db.api.*;
import com.scalar.db.exception.transaction.*;
import com.scalar.db.io.Key;
import org.springframework.stereotype.Repository;

@Repository
public class NsMysqlSctErrorRepository {

    // Get Record by Partition & Clustering Key
    public NsMysqlSctError getNsMysqlSctError(DistributedTransaction transaction, NsMysqlSctError sctError) throws CrudException {
        Key partitionKey = sctError.getPartitionKey();
        Key clusteringKey = sctError.getClusteringKey();
        Get get = Get.newBuilder()
            .namespace(NsMysqlSctError.NAMESPACE)
            .table(NsMysqlSctError.TABLE)
            .partitionKey(partitionKey)
            
            .clusteringKey(clusteringKey)
            .projections(NsMysqlSctError.PK, NsMysqlSctError.CK, NsMysqlSctError.EXCEPTION, NsMysqlSctError.EXCEPTION_AT)
            .build();
        Optional<Result> result = transaction.get(get);
        if (result.isEmpty()) {
            throw new RuntimeException("No record found in NsMysqlSctError");
        }
        return buildNsMysqlSctError(result.get());
    }

    // Insert Record
    public NsMysqlSctError postNsMysqlSctError(DistributedTransaction transaction, NsMysqlSctError sctError) throws CrudException {
        Key partitionKey = sctError.getPartitionKey();
        Key clusteringKey = sctError.getClusteringKey();
        Insert insert = Insert.newBuilder()
            .namespace(NsMysqlSctError.NAMESPACE)
            .table(NsMysqlSctError.TABLE)
            .partitionKey(partitionKey)
            .clusteringKey(clusteringKey)
            .textValue(NsMysqlSctError.EXCEPTION, sctError.getException())
            .build();
        transaction.insert(insert);
        return sctError;
    }

    // Update Record
    public NsMysqlSctError putNsMysqlSctError(DistributedTransaction transaction, NsMysqlSctError sctError) throws CrudException {
        Key partitionKey = sctError.getPartitionKey();
        Key clusteringKey = sctError.getClusteringKey();
        MutationCondition condition = ConditionBuilder.updateIfExists();

        Update update = Update.newBuilder()
            .namespace(NsMysqlSctError.NAMESPACE)
            .table(NsMysqlSctError.TABLE)
            .partitionKey(partitionKey)
            .clusteringKey(clusteringKey)
            .textValue(NsMysqlSctError.EXCEPTION, sctError.getException())
            .condition(condition)
            .build();
        transaction.update(update);
        return sctError;
    }

    // Upsert Record
    public NsMysqlSctError upsertNsMysqlSctError(DistributedTransaction transaction, NsMysqlSctError sctError) throws CrudException {
        Key partitionKey = sctError.getPartitionKey();
        Key clusteringKey = sctError.getClusteringKey();
        Upsert upsert = Upsert.newBuilder()
            .namespace(NsMysqlSctError.NAMESPACE)
            .table(NsMysqlSctError.TABLE)
            .partitionKey(partitionKey)
            .clusteringKey(clusteringKey)
            .textValue(NsMysqlSctError.EXCEPTION, sctError.getException())
            .build();
        transaction.upsert(upsert);
        return sctError;
    }

    // Delete Record
    public void deleteNsMysqlSctError(DistributedTransaction transaction, NsMysqlSctError sctError) throws CrudException {
        Key partitionKey = sctError.getPartitionKey();
        Key clusteringKey = sctError.getClusteringKey();
        MutationCondition condition = ConditionBuilder.deleteIfExists();
        Delete delete = Delete.newBuilder()
            .namespace(NsMysqlSctError.NAMESPACE)
            .table(NsMysqlSctError.TABLE)
            .partitionKey(partitionKey)
            
            .clusteringKey(clusteringKey)
            .condition(condition)
            .build();
        transaction.delete(delete);
    }

    // Scan All Records
    public List<NsMysqlSctError> getNsMysqlSctErrorListAll(DistributedTransaction transaction) throws CrudException {
        Scan scan = Scan.newBuilder()
            .namespace(NsMysqlSctError.NAMESPACE)
            .table(NsMysqlSctError.TABLE)
            .all()
            .projections(NsMysqlSctError.PK, NsMysqlSctError.CK, NsMysqlSctError.EXCEPTION, NsMysqlSctError.EXCEPTION_AT)
            .limit(100)
            .build();
        List<Result> results = transaction.scan(scan);
        List<NsMysqlSctError> sctErrorList = new ArrayList<>();
        for (Result result : results) {
            sctErrorList.add(buildNsMysqlSctError(result));
        }
        return sctErrorList;
    }

    // Scan Records by Partition Key
    public List<NsMysqlSctError> getNsMysqlSctErrorListByPk(DistributedTransaction transaction, Key partitionKey) throws CrudException {
        Scan scan = Scan.newBuilder()
            .namespace(NsMysqlSctError.NAMESPACE)
            .table(NsMysqlSctError.TABLE)
            .partitionKey(partitionKey)
            .projections(NsMysqlSctError.PK, NsMysqlSctError.CK, NsMysqlSctError.EXCEPTION, NsMysqlSctError.EXCEPTION_AT)
            .limit(100)
            .build();
        List<Result> results = transaction.scan(scan);
        List<NsMysqlSctError> sctErrorList = new ArrayList<>();
        for (Result result : results) {
            sctErrorList.add(buildNsMysqlSctError(result));
        }
        return sctErrorList;
    }

    // Object Builder from ScalarDB Result
    private NsMysqlSctError buildNsMysqlSctError(Result result) {
        return NsMysqlSctError.builder()
            .pk(result.getInt(NsMysqlSctError.PK))
            .ck(result.getInt(NsMysqlSctError.CK))
            .exception(result.getText(NsMysqlSctError.EXCEPTION))
            .exceptionAt(result.getTimestamp(NsMysqlSctError.EXCEPTION_AT))
            .build();
    }
}