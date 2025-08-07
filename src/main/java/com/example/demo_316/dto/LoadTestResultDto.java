package com.example.demo_316.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoadTestResultDto {
    
    private Integer totalOperations;
    private Integer statisticsOperations;
    private Map<String, Integer> operationCounts;
    private Map<String, Integer> successCounts;
    private Map<String, Integer> errorCounts;
    private Long totalExecutionTimeMs;
    private Long statisticsExecutionTimeMs;
    private Long rampUpTimeMs;
    private Double throughputPerMinute;
    private Map<String, Double> ratios;
    private Integer threadCount;
    private Integer exceptionsRecorded;
    private Integer testDurationSeconds;
    private Long actualTestDurationMs;
    private Boolean cleanupExecuted;
    private Integer cleanupRecordsDeleted;
}