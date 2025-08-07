package com.example.demo_316.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlLoadTestDto {
    
    @NotNull
    @Min(1)
    private Integer testDurationSeconds;
    
    @NotNull
    @Min(0)
    private Double selectRatio;
    
    @NotNull
    @Min(0)
    private Double updateRatio;
    
    @NotNull
    @Min(0)
    private Double deleteRatio;
    
    @NotNull
    private Integer pk;
    
    @NotNull
    private Integer startCk;
    
    @NotNull
    @Min(1)
    private Integer threadCount;
    
    @NotNull
    @Min(0)
    private Long exceptionRetryInterval;
    
    @NotNull
    @Min(0)
    private Integer rampUpTimeSeconds;
    
    @NotNull
    @Min(0)
    private Long operationDelayMs;
    
    @NotNull
    private Boolean cleanupAfterTest;
    
    @Builder.Default
    private Boolean isOO = false; // isOO = false uses explicit transaction management (begin/commit), isOO = true executes without explicit transactions (OO mode)
    
    // Optional custom SQL templates to override default templates
    // Key: operation type (INSERT, SELECT, UPDATE, DELETE)
    // Value: custom SQL template with ${pk}, ${ck}, ${threadId}, ${operationIndex} placeholders
    private Map<String, String> customSqlTemplates;
}