package com.example.demo_316.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.nio.ByteBuffer;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NsMysqlSctDto {
    private Integer pk;
    private Integer ck;
    private String stringValue;
    private Long bintValue;
}