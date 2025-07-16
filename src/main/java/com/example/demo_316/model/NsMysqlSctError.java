package com.example.demo_316.model;

import lombok.*;
import com.scalar.db.io.Key;
import java.time.*;
import java.nio.ByteBuffer;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NsMysqlSctError {

    public static final String NAMESPACE = "ns_mysql";
    public static final String TABLE = "sct_error";
    public static final String PK = "pk";
    public static final String CK = "ck";
    public static final String EXCEPTION = "exception";
    public static final String EXCEPTION_AT = "exception_at";

    private Integer pk;
    private Integer ck;
    private String exception;
    private LocalDateTime exceptionAt;

    public Key getPartitionKey() {
        return Key.newBuilder().addInt(PK, getPk()).build();
    }

    public Key getClusteringKey() {
        return Key.newBuilder().addInt(CK, getCk()).build();
    }
}
