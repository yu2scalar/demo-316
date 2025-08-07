package com.example.demo_316.service;

import com.example.demo_316.dto.LoadTestResultDto;
import com.example.demo_316.dto.NsMysqlSctErrorDto;
import com.example.demo_316.dto.SqlLoadTestDto;
import com.example.demo_316.exception.CustomException;
import com.example.demo_316.util.SqlTemplateGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class SqlLoadTestService {
    
    private final GenericSqlService genericSqlService;
    private final NsMysqlSctErrorService sctErrorService;
    private final NsMysqlSctService sctService; // For cleanup
    private final Random random = new Random();
    
    public LoadTestResultDto executeSqlLoadTest(SqlLoadTestDto sqlLoadTestDto) throws CustomException {
        // Statistics tracking (same as LoadTestService)
        AtomicLong totalOperations = new AtomicLong(0);
        AtomicLong statisticsOperations = new AtomicLong(0);
        AtomicLong exceptionsRecorded = new AtomicLong(0);

        // Operation counts during statistics period
        AtomicInteger statsInsertCount = new AtomicInteger(0);
        AtomicInteger statsSelectCount = new AtomicInteger(0);
        AtomicInteger statsUpdateCount = new AtomicInteger(0);
        AtomicInteger statsDeleteCount = new AtomicInteger(0);

        // Success counts during statistics period
        AtomicInteger statsInsertSuccess = new AtomicInteger(0);
        AtomicInteger statsSelectSuccess = new AtomicInteger(0);
        AtomicInteger statsUpdateSuccess = new AtomicInteger(0);
        AtomicInteger statsDeleteSuccess = new AtomicInteger(0);

        // Error counts during statistics period
        AtomicInteger statsInsertError = new AtomicInteger(0);
        AtomicInteger statsSelectError = new AtomicInteger(0);
        AtomicInteger statsUpdateError = new AtomicInteger(0);
        AtomicInteger statsDeleteError = new AtomicInteger(0);

        long testStartTime = System.currentTimeMillis();
        long rampUpEndTime = testStartTime + (sqlLoadTestDto.getRampUpTimeSeconds() * 1000L);
        long testEndTime = rampUpEndTime + (sqlLoadTestDto.getTestDurationSeconds() * 1000L);

        ExecutorService executor = Executors.newFixedThreadPool(sqlLoadTestDto.getThreadCount());

        // Create and start threads with gradual ramp-up
        long threadStartDelay = (sqlLoadTestDto.getRampUpTimeSeconds() * 1000L) / sqlLoadTestDto.getThreadCount();
        for (int threadId = 1; threadId <= sqlLoadTestDto.getThreadCount(); threadId++) {
            final int currentThreadId = threadId;
            final long threadStartTime = testStartTime + (threadStartDelay * (threadId - 1));
            
            executor.submit(() -> executeSqlWorkerThread(
                currentThreadId, threadStartTime, rampUpEndTime, testEndTime, sqlLoadTestDto,
                totalOperations, statisticsOperations, exceptionsRecorded,
                statsInsertCount, statsSelectCount, statsUpdateCount, statsDeleteCount,
                statsInsertSuccess, statsSelectSuccess, statsUpdateSuccess, statsDeleteSuccess,
                statsInsertError, statsSelectError, statsUpdateError, statsDeleteError
            ));
        }

        // Wait for all threads to complete
        executor.shutdown();
        try {
            executor.awaitTermination(
                sqlLoadTestDto.getTestDurationSeconds() + sqlLoadTestDto.getRampUpTimeSeconds() + 60, 
                TimeUnit.SECONDS
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long testCompleteTime = System.currentTimeMillis();
        long totalExecutionTime = testCompleteTime - testStartTime;
        long rampUpTime = rampUpEndTime - testStartTime;
        long statisticsTime = testCompleteTime - rampUpEndTime;

        // Calculate throughput (operations per minute)
        double throughputPerMinute = 0.0;
        if (statisticsTime > 0) {
            throughputPerMinute = (statisticsOperations.get() * 60000.0) / statisticsTime;
        }

        // Cleanup if requested (reuse existing cleanup logic from LoadTestService)
        boolean cleanupExecuted = false;
        int cleanupRecordsDeleted = 0;
        
        log.info("Cleanup check: cleanupAfterTest = {}", sqlLoadTestDto.getCleanupAfterTest());
        
        if (sqlLoadTestDto.getCleanupAfterTest() != null && sqlLoadTestDto.getCleanupAfterTest()) {
            log.info("Executing cleanup for PK: {}", sqlLoadTestDto.getPk());
            cleanupRecordsDeleted = performCleanup(sqlLoadTestDto.getPk());
            cleanupExecuted = true;
        } else {
            log.info("Cleanup skipped - cleanupAfterTest is false or null");
        }

        // Build and return result (same structure as LoadTestService)
        return LoadTestResultDto.builder()
            .totalOperations(totalOperations.intValue())
            .statisticsOperations(statisticsOperations.intValue())
            .operationCounts(Map.of(
                "insert", statsInsertCount.get(),
                "select", statsSelectCount.get(),
                "update", statsUpdateCount.get(),
                "delete", statsDeleteCount.get()
            ))
            .successCounts(Map.of(
                "insert", statsInsertSuccess.get(),
                "select", statsSelectSuccess.get(),
                "update", statsUpdateSuccess.get(),
                "delete", statsDeleteSuccess.get()
            ))
            .errorCounts(Map.of(
                "insert", statsInsertError.get(),
                "select", statsSelectError.get(),
                "update", statsUpdateError.get(),
                "delete", statsDeleteError.get()
            ))
            .totalExecutionTimeMs(totalExecutionTime)
            .statisticsExecutionTimeMs(statisticsTime)
            .rampUpTimeMs(rampUpTime)
            .throughputPerMinute(throughputPerMinute)
            .ratios(Map.of(
                "select", sqlLoadTestDto.getSelectRatio(),
                "update", sqlLoadTestDto.getUpdateRatio(),
                "delete", sqlLoadTestDto.getDeleteRatio()
            ))
            .threadCount(sqlLoadTestDto.getThreadCount())
            .exceptionsRecorded(exceptionsRecorded.intValue())
            .testDurationSeconds(sqlLoadTestDto.getTestDurationSeconds())
            .actualTestDurationMs(statisticsTime)
            .cleanupExecuted(cleanupExecuted)
            .cleanupRecordsDeleted(cleanupRecordsDeleted)
            .build();
    }
    
    private void executeSqlWorkerThread(
        int threadId, long threadStartTime, long rampUpEndTime, long testEndTime, SqlLoadTestDto sqlLoadTestDto,
        AtomicLong totalOperations, AtomicLong statisticsOperations, AtomicLong exceptionsRecorded,
        AtomicInteger statsInsertCount, AtomicInteger statsSelectCount, 
        AtomicInteger statsUpdateCount, AtomicInteger statsDeleteCount,
        AtomicInteger statsInsertSuccess, AtomicInteger statsSelectSuccess,
        AtomicInteger statsUpdateSuccess, AtomicInteger statsDeleteSuccess,
        AtomicInteger statsInsertError, AtomicInteger statsSelectError,
        AtomicInteger statsUpdateError, AtomicInteger statsDeleteError) {
        
        // Start each thread with a different initial operationIndex to avoid conflicts
        AtomicInteger operationIndex = new AtomicInteger(threadId * 1000);
        
        // Wait until this thread's scheduled start time (gradual ramp-up)
        long startTime = System.currentTimeMillis();
        if (startTime < threadStartTime) {
            try {
                Thread.sleep(threadStartTime - startTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        while (System.currentTimeMillis() < testEndTime) {
            long currentTime = System.currentTimeMillis();
            boolean isStatisticsPeriod = currentTime >= rampUpEndTime;

            // Generate CK using thread-based formula: (threadNumber * 1000000) + operationIndex
            int currentOperationIndex = operationIndex.getAndIncrement();
            int currentCk = (threadId * 1000000) + currentOperationIndex;
            
            // Debug logging for key generation
            log.debug("Thread {} generating CK: {} (operationIndex: {})", threadId, currentCk, currentOperationIndex);

            // Always perform INSERT
            performSqlInsert(sqlLoadTestDto.getPk(), currentCk, threadId, currentOperationIndex,
                        isStatisticsPeriod, totalOperations, statisticsOperations,
                        statsInsertCount, statsInsertSuccess, statsInsertError, exceptionsRecorded, sqlLoadTestDto);

            // Perform SELECT based on ratio (same logic as LoadTestService)
            double selectRatio = sqlLoadTestDto.getSelectRatio();
            int selectLoops = (int) selectRatio;
            double selectFractional = selectRatio - selectLoops;
            
            // Execute guaranteed select loops
            for (int i = 0; i < selectLoops; i++) {
                performSqlSelect(sqlLoadTestDto.getPk(), currentCk, threadId, currentOperationIndex,
                            isStatisticsPeriod, totalOperations, statisticsOperations,
                            statsSelectCount, statsSelectSuccess, statsSelectError, exceptionsRecorded, sqlLoadTestDto);
            }
            
            // Additional select loop based on fractional probability
            if (selectFractional > 0 && random.nextDouble() < selectFractional) {
                performSqlSelect(sqlLoadTestDto.getPk(), currentCk, threadId, currentOperationIndex,
                            isStatisticsPeriod, totalOperations, statisticsOperations,
                            statsSelectCount, statsSelectSuccess, statsSelectError, exceptionsRecorded, sqlLoadTestDto);
            }

            // Perform UPDATE based on ratio (handle fractional)
            double updateRatio = sqlLoadTestDto.getUpdateRatio();
            int updateLoops = (int) updateRatio;
            double updateFractional = updateRatio - updateLoops;

            // Execute guaranteed update loops
            for (int i = 0; i < updateLoops; i++) {
                performSqlUpdate(sqlLoadTestDto.getPk(), currentCk, threadId, currentOperationIndex, i, false,
                            isStatisticsPeriod, totalOperations, statisticsOperations,
                            statsUpdateCount, statsUpdateSuccess, statsUpdateError, exceptionsRecorded, sqlLoadTestDto);
            }

            // Additional update loop based on fractional probability
            if (updateFractional > 0 && random.nextDouble() < updateFractional) {
                performSqlUpdate(sqlLoadTestDto.getPk(), currentCk, threadId, currentOperationIndex, updateLoops, true,
                            isStatisticsPeriod, totalOperations, statisticsOperations,
                            statsUpdateCount, statsUpdateSuccess, statsUpdateError, exceptionsRecorded, sqlLoadTestDto);
            }

            // Perform DELETE based on ratio
            double deleteRatio = sqlLoadTestDto.getDeleteRatio();
            int deleteLoops = (int) deleteRatio;
            double deleteFractional = deleteRatio - deleteLoops;
            
            // Execute guaranteed delete loops
            for (int i = 0; i < deleteLoops; i++) {
                performSqlDelete(sqlLoadTestDto.getPk(), currentCk, threadId, currentOperationIndex,
                            isStatisticsPeriod, totalOperations, statisticsOperations,
                            statsDeleteCount, statsDeleteSuccess, statsDeleteError, exceptionsRecorded, sqlLoadTestDto);
            }
            
            // Additional delete loop based on fractional probability
            if (deleteFractional > 0 && random.nextDouble() < deleteFractional) {
                performSqlDelete(sqlLoadTestDto.getPk(), currentCk, threadId, currentOperationIndex,
                            isStatisticsPeriod, totalOperations, statisticsOperations,
                            statsDeleteCount, statsDeleteSuccess, statsDeleteError, exceptionsRecorded, sqlLoadTestDto);
            }

            // Optional delay between operations
            if (sqlLoadTestDto.getOperationDelayMs() != null && sqlLoadTestDto.getOperationDelayMs() > 0) {
                try {
                    Thread.sleep(sqlLoadTestDto.getOperationDelayMs());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void performSqlInsert(Integer pk, Integer ck, int threadId, int operationIndex,
                                 boolean isStatisticsPeriod, AtomicLong totalOps, AtomicLong statsOps,
                                 AtomicInteger count, AtomicInteger success, AtomicInteger error, 
                                 AtomicLong exceptionsRecorded, SqlLoadTestDto sqlLoadTestDto) {
        try {
            String sqlTemplate = getSqlTemplate("INSERT", sqlLoadTestDto.getCustomSqlTemplates());
            String resolvedSql = SqlTemplateGenerator.resolveParameters(sqlTemplate, pk, ck, threadId, operationIndex);
            
            genericSqlService.executeSQLGeneric(new com.example.demo_316.dto.SqlCommandDto(resolvedSql));

            totalOps.incrementAndGet();
            if (isStatisticsPeriod) {
                statsOps.incrementAndGet();
                count.incrementAndGet();
                success.incrementAndGet();
            }
        } catch (Exception e) {
            recordException(pk, ck, "SQL_INSERT", e, exceptionsRecorded);
            waitForRetryInterval(sqlLoadTestDto.getExceptionRetryInterval());
            totalOps.incrementAndGet();
            if (isStatisticsPeriod) {
                statsOps.incrementAndGet();
                count.incrementAndGet();
                error.incrementAndGet();
            }
        }
    }

    private void performSqlSelect(Integer pk, Integer ck, int threadId, int operationIndex,
                                 boolean isStatisticsPeriod, AtomicLong totalOps, AtomicLong statsOps,
                                 AtomicInteger count, AtomicInteger success, AtomicInteger error, 
                                 AtomicLong exceptionsRecorded, SqlLoadTestDto sqlLoadTestDto) {
        try {
            String sqlTemplate = getSqlTemplate("SELECT", sqlLoadTestDto.getCustomSqlTemplates());
            String resolvedSql = SqlTemplateGenerator.resolveParameters(sqlTemplate, pk, ck, threadId, operationIndex);
            
            genericSqlService.executeSQLGeneric(new com.example.demo_316.dto.SqlCommandDto(resolvedSql));

            totalOps.incrementAndGet();
            if (isStatisticsPeriod) {
                statsOps.incrementAndGet();
                count.incrementAndGet();
                success.incrementAndGet();
            }
        } catch (Exception e) {
            recordException(pk, ck, "SQL_SELECT", e, exceptionsRecorded);
            waitForRetryInterval(sqlLoadTestDto.getExceptionRetryInterval());
            totalOps.incrementAndGet();
            if (isStatisticsPeriod) {
                statsOps.incrementAndGet();
                count.incrementAndGet();
                error.incrementAndGet();
            }
        }
    }

    private void performSqlUpdate(Integer pk, Integer ck, int threadId, int operationIndex, int updateCount, boolean isFractional,
                                 boolean isStatisticsPeriod, AtomicLong totalOps, AtomicLong statsOps,
                                 AtomicInteger count, AtomicInteger success, AtomicInteger error, 
                                 AtomicLong exceptionsRecorded, SqlLoadTestDto sqlLoadTestDto) {
        try {
            String sqlTemplate = getSqlTemplate("UPDATE", sqlLoadTestDto.getCustomSqlTemplates());
            String resolvedSql = SqlTemplateGenerator.resolveParameters(sqlTemplate, pk, ck, threadId, operationIndex);
            
            genericSqlService.executeSQLGeneric(new com.example.demo_316.dto.SqlCommandDto(resolvedSql));

            totalOps.incrementAndGet();
            if (isStatisticsPeriod) {
                statsOps.incrementAndGet();
                count.incrementAndGet();
                success.incrementAndGet();
            }
        } catch (Exception e) {
            recordException(pk, ck, "SQL_UPDATE", e, exceptionsRecorded);
            waitForRetryInterval(sqlLoadTestDto.getExceptionRetryInterval());
            totalOps.incrementAndGet();
            if (isStatisticsPeriod) {
                statsOps.incrementAndGet();
                count.incrementAndGet();
                error.incrementAndGet();
            }
        }
    }

    private void performSqlDelete(Integer pk, Integer ck, int threadId, int operationIndex,
                                 boolean isStatisticsPeriod, AtomicLong totalOps, AtomicLong statsOps,
                                 AtomicInteger count, AtomicInteger success, AtomicInteger error, 
                                 AtomicLong exceptionsRecorded, SqlLoadTestDto sqlLoadTestDto) {
        try {
            String sqlTemplate = getSqlTemplate("DELETE", sqlLoadTestDto.getCustomSqlTemplates());
            String resolvedSql = SqlTemplateGenerator.resolveParameters(sqlTemplate, pk, ck, threadId, operationIndex);
            
            genericSqlService.executeSQLGeneric(new com.example.demo_316.dto.SqlCommandDto(resolvedSql));

            totalOps.incrementAndGet();
            if (isStatisticsPeriod) {
                statsOps.incrementAndGet();
                count.incrementAndGet();
                success.incrementAndGet();
            }
        } catch (Exception e) {
            recordException(pk, ck, "SQL_DELETE", e, exceptionsRecorded);
            waitForRetryInterval(sqlLoadTestDto.getExceptionRetryInterval());
            totalOps.incrementAndGet();
            if (isStatisticsPeriod) {
                statsOps.incrementAndGet();
                count.incrementAndGet();
                error.incrementAndGet();
            }
        }
    }
    
    private String getSqlTemplate(String operationType, Map<String, String> customTemplates) {
        // Check if custom template is provided
        if (customTemplates != null && customTemplates.containsKey(operationType)) {
            return customTemplates.get(operationType);
        }
        
        // Fall back to default templates from SqlTemplateGenerator
        return SqlTemplateGenerator.getTemplateByOperation(operationType);
    }
    
    // Reuse exception handling logic from LoadTestService
    private void waitForRetryInterval(Long retryInterval) {
        if (retryInterval != null && retryInterval > 0) {
            try {
                Thread.sleep(retryInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void recordException(Integer pk, Integer ck, String operationType, Exception e, AtomicLong exceptionsRecorded) {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);

            // Truncate to millisecond precision for ScalarDB compatibility
            LocalDateTime now = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
            
            // Truncate exception message to prevent gRPC header size issues (max 8KB to be safe)
            String fullExceptionMessage = operationType + " failed: " + e.getMessage() + "\nStackTrace: " + sw.toString();
            String truncatedMessage = truncateExceptionMessage(fullExceptionMessage, 8192);
            
            // Try to record the exception with retry logic for timestamp conflicts
            int retryCount = 0;
            while (retryCount < 3) {
                try {
                    NsMysqlSctErrorDto errorDto = NsMysqlSctErrorDto.builder()
                        .pk(pk)
                        .ck(ck)
                        .exception(truncatedMessage)
                        .exceptionAt(now.plusNanos(retryCount * 1000000L).truncatedTo(java.time.temporal.ChronoUnit.MILLIS)) // Add milliseconds for uniqueness
                        .build();

                    sctErrorService.postNsMysqlSctError(errorDto);
                    exceptionsRecorded.incrementAndGet();
                    break; // Success, exit retry loop
                } catch (Exception ex) {
                    retryCount++;
                    if (retryCount >= 3) {
                        log.error("Failed to record exception after {} retries: {}", retryCount, ex.getMessage());
                    } else {
                        log.warn("Exception recording failed, retrying... (attempt {}/3): {}", retryCount, ex.getMessage());
                        // Add a small delay between retries
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Failed to record exception: {}", ex.getMessage());
        }
    }
    
    private String truncateExceptionMessage(String message, int maxLength) {
        if (message == null) {
            return null;
        }
        
        if (message.length() <= maxLength) {
            return message;
        }
        
        // Truncate and add indication that it was truncated
        String truncated = message.substring(0, maxLength - 50); // Leave room for truncation message
        return truncated + "\n... [TRUNCATED - Original length: " + message.length() + " chars]";
    }

    // Reuse cleanup logic from LoadTestService
    private int performCleanup(Integer pk) {
        int deletedRecords = 0;
        try {
            // Get all records with the specified PK from sct table
            com.example.demo_316.dto.NsMysqlSctDto searchDto = com.example.demo_316.dto.NsMysqlSctDto.builder().pk(pk).build();
            java.util.List<com.example.demo_316.dto.NsMysqlSctDto> sctRecords = sctService.getNsMysqlSctListByPk(searchDto);

            // Delete all sct records only (preserve sct_error records for analysis)
            for (com.example.demo_316.dto.NsMysqlSctDto record : sctRecords) {
                try {
                    sctService.deleteNsMysqlSct(record, false);
                    deletedRecords++;
                } catch (Exception e) {
                    log.error("Failed to delete sct record: {}", e.getMessage());
                }
            }

            log.info("SQL Load Test cleanup completed: {} sct records deleted for PK {}. Error records preserved for analysis.", deletedRecords, pk);
        } catch (Exception e) {
            log.error("SQL Load Test cleanup failed: {}", e.getMessage());
        }
        return deletedRecords;
    }
}