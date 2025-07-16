package com.example.demo_316.service;

import com.example.demo_316.dto.LoadTestDto;
import com.example.demo_316.dto.LoadTestResultDto;
import com.example.demo_316.dto.NsMysqlSctDto;
import com.example.demo_316.dto.NsMysqlSctErrorDto;
import com.example.demo_316.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.List;
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
public class LoadTestService {
    
    private final NsMysqlSctService sctService;
    private final NsMysqlSctErrorService sctErrorService;
    private final Random random = new Random();
    
    public LoadTestResultDto executeLoadTest(LoadTestDto loadTestDto) throws CustomException {
        // Statistics tracking
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
        long rampUpEndTime = testStartTime + (loadTestDto.getRampUpTimeSeconds() * 1000L);
        long testEndTime = rampUpEndTime + (loadTestDto.getTestDurationSeconds() * 1000L);

        ExecutorService executor = Executors.newFixedThreadPool(loadTestDto.getThreadCount());

        // Create and start threads
        for (int threadId = 1; threadId <= loadTestDto.getThreadCount(); threadId++) {
            final int currentThreadId = threadId;
            executor.submit(() -> executeWorkerThread(
                currentThreadId, rampUpEndTime, testEndTime, loadTestDto,
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
                loadTestDto.getTestDurationSeconds() + loadTestDto.getRampUpTimeSeconds() + 60, 
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

        // Cleanup if requested
        boolean cleanupExecuted = false;
        int cleanupRecordsDeleted = 0;
        if (loadTestDto.getCleanupAfterTest() != null && loadTestDto.getCleanupAfterTest()) {
            cleanupRecordsDeleted = performCleanup(loadTestDto.getPk());
            cleanupExecuted = true;
        }

        // Build and return result
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
                "select", loadTestDto.getSelectRatio(),
                "update", loadTestDto.getUpdateRatio(),
                "delete", loadTestDto.getDeleteRatio()
            ))
            .threadCount(loadTestDto.getThreadCount())
            .exceptionsRecorded(exceptionsRecorded.intValue())
            .testDurationSeconds(loadTestDto.getTestDurationSeconds())
            .actualTestDurationMs(statisticsTime)
            .cleanupExecuted(cleanupExecuted)
            .cleanupRecordsDeleted(cleanupRecordsDeleted)
            .build();
    }
    
    private void executeWorkerThread(
        int threadId, long rampUpEndTime, long testEndTime, LoadTestDto loadTestDto,
        AtomicLong totalOperations, AtomicLong statisticsOperations, AtomicLong exceptionsRecorded,
        AtomicInteger statsInsertCount, AtomicInteger statsSelectCount, 
        AtomicInteger statsUpdateCount, AtomicInteger statsDeleteCount,
        AtomicInteger statsInsertSuccess, AtomicInteger statsSelectSuccess,
        AtomicInteger statsUpdateSuccess, AtomicInteger statsDeleteSuccess,
        AtomicInteger statsInsertError, AtomicInteger statsSelectError,
        AtomicInteger statsUpdateError, AtomicInteger statsDeleteError) {
        
        AtomicInteger operationIndex = new AtomicInteger(0);

        while (System.currentTimeMillis() < testEndTime) {
            long currentTime = System.currentTimeMillis();
            boolean isStatisticsPeriod = currentTime >= rampUpEndTime;

            // Generate CK using thread-based formula: (threadNumber * 1000000) + operationIndex
            int currentCk = (threadId * 1000000) + operationIndex.getAndIncrement();

            // Always perform INSERT
            performInsert(loadTestDto.getPk(), currentCk, threadId, operationIndex.get(),
                        isStatisticsPeriod, totalOperations, statisticsOperations,
                        statsInsertCount, statsInsertSuccess, statsInsertError, exceptionsRecorded, loadTestDto);

            // Perform SELECT based on ratio
            double selectRatio = loadTestDto.getSelectRatio();
            int selectLoops = (int) selectRatio;
            double selectFractional = selectRatio - selectLoops;
            
            // Execute guaranteed select loops
            for (int i = 0; i < selectLoops; i++) {
                performSelect(loadTestDto.getPk(), currentCk, threadId, operationIndex.get(),
                            isStatisticsPeriod, totalOperations, statisticsOperations,
                            statsSelectCount, statsSelectSuccess, statsSelectError, exceptionsRecorded, loadTestDto);
            }
            
            // Additional select loop based on fractional probability
            if (selectFractional > 0 && random.nextDouble() < selectFractional) {
                performSelect(loadTestDto.getPk(), currentCk, threadId, operationIndex.get(),
                            isStatisticsPeriod, totalOperations, statisticsOperations,
                            statsSelectCount, statsSelectSuccess, statsSelectError, exceptionsRecorded, loadTestDto);
            }

            // Perform UPDATE based on ratio (handle fractional)
            double updateRatio = loadTestDto.getUpdateRatio();
            int updateLoops = (int) updateRatio;
            double updateFractional = updateRatio - updateLoops;

            // Execute guaranteed update loops
            for (int i = 0; i < updateLoops; i++) {
                performUpdate(loadTestDto.getPk(), currentCk, threadId, operationIndex.get(), i, false,
                            isStatisticsPeriod, totalOperations, statisticsOperations,
                            statsUpdateCount, statsUpdateSuccess, statsUpdateError, exceptionsRecorded, loadTestDto);
            }

            // Additional update loop based on fractional probability
            if (updateFractional > 0 && random.nextDouble() < updateFractional) {
                performUpdate(loadTestDto.getPk(), currentCk, threadId, operationIndex.get(), updateLoops, true,
                            isStatisticsPeriod, totalOperations, statisticsOperations,
                            statsUpdateCount, statsUpdateSuccess, statsUpdateError, exceptionsRecorded, loadTestDto);
            }

            // Perform DELETE based on ratio
            double deleteRatio = loadTestDto.getDeleteRatio();
            int deleteLoops = (int) deleteRatio;
            double deleteFractional = deleteRatio - deleteLoops;
            
            // Execute guaranteed delete loops
            for (int i = 0; i < deleteLoops; i++) {
                performDelete(loadTestDto.getPk(), currentCk, threadId, operationIndex.get(),
                            isStatisticsPeriod, totalOperations, statisticsOperations,
                            statsDeleteCount, statsDeleteSuccess, statsDeleteError, exceptionsRecorded, loadTestDto);
            }
            
            // Additional delete loop based on fractional probability
            if (deleteFractional > 0 && random.nextDouble() < deleteFractional) {
                performDelete(loadTestDto.getPk(), currentCk, threadId, operationIndex.get(),
                            isStatisticsPeriod, totalOperations, statisticsOperations,
                            statsDeleteCount, statsDeleteSuccess, statsDeleteError, exceptionsRecorded, loadTestDto);
            }

            // Optional delay between operations
            if (loadTestDto.getOperationDelayMs() != null && loadTestDto.getOperationDelayMs() > 0) {
                try {
                    Thread.sleep(loadTestDto.getOperationDelayMs());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void performInsert(Integer pk, Integer ck, int threadId, int operationIndex,
                              boolean isStatisticsPeriod, AtomicLong totalOps, AtomicLong statsOps,
                              AtomicInteger count, AtomicInteger success, AtomicInteger error, 
                              AtomicLong exceptionsRecorded, LoadTestDto loadTestDto) {
        try {
            NsMysqlSctDto insertDto = NsMysqlSctDto.builder()
                .pk(pk)
                .ck(ck)
                .stringValue("LoadTest_T" + threadId + "_" + operationIndex)
                .bintValue((long) (threadId * 1000000 + operationIndex))
                .build();

            sctService.postNsMysqlSct(insertDto);

            totalOps.incrementAndGet();
            if (isStatisticsPeriod) {
                statsOps.incrementAndGet();
                count.incrementAndGet();
                success.incrementAndGet();
            }
        } catch (Exception e) {
            recordException(pk, ck, "INSERT", e, exceptionsRecorded);
            waitForRetryInterval(loadTestDto.getExceptionRetryInterval());
            totalOps.incrementAndGet();
            if (isStatisticsPeriod) {
                statsOps.incrementAndGet();
                count.incrementAndGet();
                error.incrementAndGet();
            }
        }
    }

    private void performSelect(Integer pk, Integer ck, int threadId, int operationIndex,
                              boolean isStatisticsPeriod, AtomicLong totalOps, AtomicLong statsOps,
                              AtomicInteger count, AtomicInteger success, AtomicInteger error, 
                              AtomicLong exceptionsRecorded, LoadTestDto loadTestDto) {
        try {
            NsMysqlSctDto selectDto = NsMysqlSctDto.builder()
                .pk(pk)
                .ck(ck)
                .build();

            sctService.getNsMysqlSct(selectDto);

            totalOps.incrementAndGet();
            if (isStatisticsPeriod) {
                statsOps.incrementAndGet();
                count.incrementAndGet();
                success.incrementAndGet();
            }
        } catch (Exception e) {
            recordException(pk, ck, "SELECT", e, exceptionsRecorded);
            waitForRetryInterval(loadTestDto.getExceptionRetryInterval());
            totalOps.incrementAndGet();
            if (isStatisticsPeriod) {
                statsOps.incrementAndGet();
                count.incrementAndGet();
                error.incrementAndGet();
            }
        }
    }

    private void performUpdate(Integer pk, Integer ck, int threadId, int operationIndex, int updateCount, boolean isFractional,
                              boolean isStatisticsPeriod, AtomicLong totalOps, AtomicLong statsOps,
                              AtomicInteger count, AtomicInteger success, AtomicInteger error, 
                              AtomicLong exceptionsRecorded, LoadTestDto loadTestDto) {
        try {
            String stringValue = isFractional ? 
                "Updated_fractional_T" + threadId + "_" + operationIndex :
                "Updated_T" + threadId + "_" + operationIndex + "_" + updateCount;
            
            NsMysqlSctDto updateDto = NsMysqlSctDto.builder()
                .pk(pk)
                .ck(ck)
                .stringValue(stringValue)
                .bintValue((long) (threadId * 1000000 + operationIndex + updateCount * 100))
                .build();

            sctService.putNsMysqlSct(updateDto);

            totalOps.incrementAndGet();
            if (isStatisticsPeriod) {
                statsOps.incrementAndGet();
                count.incrementAndGet();
                success.incrementAndGet();
            }
        } catch (Exception e) {
            recordException(pk, ck, "UPDATE", e, exceptionsRecorded);
            waitForRetryInterval(loadTestDto.getExceptionRetryInterval());
            totalOps.incrementAndGet();
            if (isStatisticsPeriod) {
                statsOps.incrementAndGet();
                count.incrementAndGet();
                error.incrementAndGet();
            }
        }
    }

    private void performDelete(Integer pk, Integer ck, int threadId, int operationIndex,
                              boolean isStatisticsPeriod, AtomicLong totalOps, AtomicLong statsOps,
                              AtomicInteger count, AtomicInteger success, AtomicInteger error, 
                              AtomicLong exceptionsRecorded, LoadTestDto loadTestDto) {
        try {
            NsMysqlSctDto deleteDto = NsMysqlSctDto.builder()
                .pk(pk)
                .ck(ck)
                .build();

            sctService.deleteNsMysqlSct(deleteDto);

            totalOps.incrementAndGet();
            if (isStatisticsPeriod) {
                statsOps.incrementAndGet();
                count.incrementAndGet();
                success.incrementAndGet();
            }
        } catch (Exception e) {
            recordException(pk, ck, "DELETE", e, exceptionsRecorded);
            waitForRetryInterval(loadTestDto.getExceptionRetryInterval());
            totalOps.incrementAndGet();
            if (isStatisticsPeriod) {
                statsOps.incrementAndGet();
                count.incrementAndGet();
                error.incrementAndGet();
            }
        }
    }

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

            NsMysqlSctErrorDto errorDto = NsMysqlSctErrorDto.builder()
                .pk(pk)
                .ck(ck)
                .exception(operationType + " failed: " + e.getMessage() + "\nStackTrace: " + sw.toString())
                .exceptionAt(LocalDateTime.now())
                .build();

            sctErrorService.postNsMysqlSctError(errorDto);
            exceptionsRecorded.incrementAndGet();
        } catch (Exception ex) {
            log.error("Failed to record exception: {}", ex.getMessage());
        }
    }

    private int performCleanup(Integer pk) {
        int deletedRecords = 0;
        try {
            // Get all records with the specified PK from sct table
            NsMysqlSctDto searchDto = NsMysqlSctDto.builder().pk(pk).build();
            List<NsMysqlSctDto> sctRecords = sctService.getNsMysqlSctListByPk(searchDto);

            // Delete all sct records
            for (NsMysqlSctDto record : sctRecords) {
                try {
                    sctService.deleteNsMysqlSct(record);
                    deletedRecords++;
                } catch (Exception e) {
                    log.error("Failed to delete sct record: {}", e.getMessage());
                }
            }

            // Get all error records with the specified PK from sct_error table
            NsMysqlSctErrorDto searchErrorDto = NsMysqlSctErrorDto.builder().pk(pk).build();
            List<NsMysqlSctErrorDto> errorRecords = sctErrorService.getNsMysqlSctErrorListByPk(searchErrorDto);

            // Delete all error records
            for (NsMysqlSctErrorDto record : errorRecords) {
                try {
                    sctErrorService.deleteNsMysqlSctError(record);
                    deletedRecords++;
                } catch (Exception e) {
                    log.error("Failed to delete error record: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Cleanup failed: {}", e.getMessage());
        }
        return deletedRecords;
    }
}