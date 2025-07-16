package com.example.demo_316.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoadTestDto {
    
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
}