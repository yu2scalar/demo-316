package com.example.demo_316.repository;

import com.example.demo_316.model.NsMysqlSct;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.scalar.db.api.*;
import com.scalar.db.exception.transaction.*;
import com.scalar.db.io.Key;
import org.springframework.stereotype.Repository;

@Repository
public class NsMysqlSctRepository {

    // Get Record by Partition & Clustering Key
    public NsMysqlSct getNsMysqlSct(DistributedTransaction transaction, NsMysqlSct sct) throws CrudException {
        Key partitionKey = sct.getPartitionKey();
        Key clusteringKey = sct.getClusteringKey();
        Get get = Get.newBuilder()
            .namespace(NsMysqlSct.NAMESPACE)
            .table(NsMysqlSct.TABLE)
            .partitionKey(partitionKey)
            
            .clusteringKey(clusteringKey)
            .projections(NsMysqlSct.PK, NsMysqlSct.CK, NsMysqlSct.STRING_VALUE, NsMysqlSct.BINT_VALUE)
            .build();
        Optional<Result> result = transaction.get(get);
        if (result.isEmpty()) {
            throw new RuntimeException("No record found in NsMysqlSct");
        }
        return buildNsMysqlSct(result.get());
    }

    // Get Record by Partition & Clustering Key
    public NsMysqlSct getNsMysqlSctOO(DistributedTransactionManager transaction, NsMysqlSct sct) throws CrudException, UnknownTransactionStatusException {
        Key partitionKey = sct.getPartitionKey();
        Key clusteringKey = sct.getClusteringKey();
        Get get = Get.newBuilder()
                .namespace(NsMysqlSct.NAMESPACE)
                .table(NsMysqlSct.TABLE)
                .partitionKey(partitionKey)

                .clusteringKey(clusteringKey)
                .projections(NsMysqlSct.PK, NsMysqlSct.CK, NsMysqlSct.STRING_VALUE, NsMysqlSct.BINT_VALUE)
                .build();
        Optional<Result> result = transaction.get(get);
        if (result.isEmpty()) {
            throw new RuntimeException("No record found in NsMysqlSct");
        }
        return buildNsMysqlSct(result.get());
    }


    // Insert Record
    public NsMysqlSct postNsMysqlSct(DistributedTransaction transaction, NsMysqlSct sct) throws CrudException {
        Key partitionKey = sct.getPartitionKey();
        Key clusteringKey = sct.getClusteringKey();
        Insert insert = Insert.newBuilder()
            .namespace(NsMysqlSct.NAMESPACE)
            .table(NsMysqlSct.TABLE)
            .partitionKey(partitionKey)
            .clusteringKey(clusteringKey)
            .textValue(NsMysqlSct.STRING_VALUE, sct.getStringValue())
            .bigIntValue(NsMysqlSct.BINT_VALUE, sct.getBintValue())
            .build();
        transaction.insert(insert);
        return sct;
    }

    // Insert Record
    public NsMysqlSct postNsMysqlSctOO(DistributedTransactionManager transaction, NsMysqlSct sct) throws CrudException, UnknownTransactionStatusException {
        Key partitionKey = sct.getPartitionKey();
        Key clusteringKey = sct.getClusteringKey();
        Insert insert = Insert.newBuilder()
                .namespace(NsMysqlSct.NAMESPACE)
                .table(NsMysqlSct.TABLE)
                .partitionKey(partitionKey)
                .clusteringKey(clusteringKey)
                .textValue(NsMysqlSct.STRING_VALUE, sct.getStringValue())
                .bigIntValue(NsMysqlSct.BINT_VALUE, sct.getBintValue())
                .build();
        transaction.insert(insert);
        return sct;
    }


    // Update Record
    public NsMysqlSct putNsMysqlSct(DistributedTransaction transaction, NsMysqlSct sct) throws CrudException {
        Key partitionKey = sct.getPartitionKey();
        Key clusteringKey = sct.getClusteringKey();
        MutationCondition condition = ConditionBuilder.updateIfExists();

        Update update = Update.newBuilder()
            .namespace(NsMysqlSct.NAMESPACE)
            .table(NsMysqlSct.TABLE)
            .partitionKey(partitionKey)
            .clusteringKey(clusteringKey)
            .textValue(NsMysqlSct.STRING_VALUE, sct.getStringValue())
            .bigIntValue(NsMysqlSct.BINT_VALUE, sct.getBintValue())
            .condition(condition)
            .build();
        transaction.update(update);
        return sct;
    }

    // Update Record
    public NsMysqlSct putNsMysqlSctOO(DistributedTransactionManager transaction, NsMysqlSct sct) throws CrudException, UnknownTransactionStatusException {
        Key partitionKey = sct.getPartitionKey();
        Key clusteringKey = sct.getClusteringKey();
        MutationCondition condition = ConditionBuilder.updateIfExists();

        Update update = Update.newBuilder()
                .namespace(NsMysqlSct.NAMESPACE)
                .table(NsMysqlSct.TABLE)
                .partitionKey(partitionKey)
                .clusteringKey(clusteringKey)
                .textValue(NsMysqlSct.STRING_VALUE, sct.getStringValue())
                .bigIntValue(NsMysqlSct.BINT_VALUE, sct.getBintValue())
                .condition(condition)
                .build();
        transaction.update(update);
        return sct;
    }

    // Upsert Record
    public NsMysqlSct upsertNsMysqlSct(DistributedTransaction transaction, NsMysqlSct sct) throws CrudException {
        Key partitionKey = sct.getPartitionKey();
        Key clusteringKey = sct.getClusteringKey();
        Upsert upsert = Upsert.newBuilder()
                .namespace(NsMysqlSct.NAMESPACE)
                .table(NsMysqlSct.TABLE)
                .partitionKey(partitionKey)
                .clusteringKey(clusteringKey)
                .textValue(NsMysqlSct.STRING_VALUE, sct.getStringValue())
                .bigIntValue(NsMysqlSct.BINT_VALUE, sct.getBintValue())
                .build();
        transaction.upsert(upsert);
        return sct;
    }

    // Upsert Record
    public NsMysqlSct upsertNsMysqlSctOO(DistributedTransactionManager transaction, NsMysqlSct sct) throws CrudException, UnknownTransactionStatusException {
        Key partitionKey = sct.getPartitionKey();
        Key clusteringKey = sct.getClusteringKey();
        Upsert upsert = Upsert.newBuilder()
                .namespace(NsMysqlSct.NAMESPACE)
                .table(NsMysqlSct.TABLE)
                .partitionKey(partitionKey)
                .clusteringKey(clusteringKey)
                .textValue(NsMysqlSct.STRING_VALUE, sct.getStringValue())
                .bigIntValue(NsMysqlSct.BINT_VALUE, sct.getBintValue())
                .build();
        transaction.upsert(upsert);
        return sct;
    }

    // Delete Record
    public void deleteNsMysqlSct(DistributedTransaction transaction, NsMysqlSct sct) throws CrudException {
        Key partitionKey = sct.getPartitionKey();
        Key clusteringKey = sct.getClusteringKey();
        MutationCondition condition = ConditionBuilder.deleteIfExists();
        Delete delete = Delete.newBuilder()
            .namespace(NsMysqlSct.NAMESPACE)
            .table(NsMysqlSct.TABLE)
            .partitionKey(partitionKey)
            
            .clusteringKey(clusteringKey)
            .condition(condition)
            .build();
        transaction.delete(delete);
    }

    // Delete Record
    public void deleteNsMysqlSctOO(DistributedTransactionManager transaction, NsMysqlSct sct) throws CrudException, UnknownTransactionStatusException {
        Key partitionKey = sct.getPartitionKey();
        Key clusteringKey = sct.getClusteringKey();
        MutationCondition condition = ConditionBuilder.deleteIfExists();
        Delete delete = Delete.newBuilder()
                .namespace(NsMysqlSct.NAMESPACE)
                .table(NsMysqlSct.TABLE)
                .partitionKey(partitionKey)

                .clusteringKey(clusteringKey)
                .condition(condition)
                .build();
        transaction.delete(delete);
    }

    // Scan All Records
    public List<NsMysqlSct> getNsMysqlSctListAll(DistributedTransaction transaction) throws CrudException {
        Scan scan = Scan.newBuilder()
            .namespace(NsMysqlSct.NAMESPACE)
            .table(NsMysqlSct.TABLE)
            .all()
            .projections(NsMysqlSct.PK, NsMysqlSct.CK, NsMysqlSct.STRING_VALUE, NsMysqlSct.BINT_VALUE)
            .limit(100)
            .build();
        List<Result> results = transaction.scan(scan);
        List<NsMysqlSct> sctList = new ArrayList<>();
        for (Result result : results) {
            sctList.add(buildNsMysqlSct(result));
        }
        return sctList;
    }

    // Scan Records by Partition Key
    public List<NsMysqlSct> getNsMysqlSctListByPk(DistributedTransaction transaction, Key partitionKey) throws CrudException {
        Scan scan = Scan.newBuilder()
            .namespace(NsMysqlSct.NAMESPACE)
            .table(NsMysqlSct.TABLE)
            .partitionKey(partitionKey)
            .projections(NsMysqlSct.PK, NsMysqlSct.CK, NsMysqlSct.STRING_VALUE, NsMysqlSct.BINT_VALUE)
            .limit(100)
            .build();
        List<Result> results = transaction.scan(scan);
        List<NsMysqlSct> sctList = new ArrayList<>();
        for (Result result : results) {
            sctList.add(buildNsMysqlSct(result));
        }
        return sctList;
    }

    // Object Builder from ScalarDB Result
    private NsMysqlSct buildNsMysqlSct(Result result) {
        return NsMysqlSct.builder()
            .pk(result.getInt(NsMysqlSct.PK))
            .ck(result.getInt(NsMysqlSct.CK))
            .stringValue(result.getText(NsMysqlSct.STRING_VALUE))
            .bintValue(result.getBigInt(NsMysqlSct.BINT_VALUE))
            .build();
    }
}