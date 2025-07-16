package com.example.demo_316.model;

import lombok.*;
import com.scalar.db.io.Key;
import java.time.*;
import java.nio.ByteBuffer;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NsMysqlSct {

    public static final String NAMESPACE = "ns_mysql";
    public static final String TABLE = "sct";
    public static final String PK = "pk";
    public static final String CK = "ck";
    public static final String STRING_VALUE = "string_value";
    public static final String BINT_VALUE = "bint_value";

    private Integer pk;
    private Integer ck;
    private String stringValue;
    private Long bintValue;

    public Key getPartitionKey() {
        return Key.newBuilder().addInt(PK, getPk()).build();
    }

    public Key getClusteringKey() {
        return Key.newBuilder().addInt(CK, getCk()).build();
    }
}
