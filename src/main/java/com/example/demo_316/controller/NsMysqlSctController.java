package com.example.demo_316.controller;

import com.example.demo_316.service.NsMysqlSctService;
import com.example.demo_316.service.NsMysqlSctErrorService;
import com.example.demo_316.dto.NsMysqlSctDto;
import com.example.demo_316.dto.NsMysqlSctErrorDto;
import com.example.demo_316.dto.ResponseStatusDto;
import com.example.demo_316.dto.SqlCommandDto;
import com.example.demo_316.dto.LoadTestDto;
import com.example.demo_316.exception.CustomException;
import com.scalar.db.exception.transaction.CrudException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.time.LocalDateTime;
import java.io.StringWriter;
import java.io.PrintWriter;

@RequestMapping(value = "/sct")
@RestController
public class NsMysqlSctController {
    @Autowired
    private NsMysqlSctService sctService;

    @Autowired
    private NsMysqlSctErrorService sctErrorService;

    private final Random random = new Random();

    @PostMapping
    public ResponseEntity<ResponseStatusDto> postNsMysqlSct(@RequestBody NsMysqlSctDto sctDto) throws CustomException {
        return ResponseEntity.ok(sctService.postNsMysqlSct(sctDto));
    }

    @PostMapping("/upsert")
    public ResponseEntity<ResponseStatusDto> upsertNsMysqlSct(@RequestBody NsMysqlSctDto sctDto) throws CustomException {
        return ResponseEntity.ok(sctService.upsertNsMysqlSct(sctDto));
    }

    @GetMapping("/{pk}/{ck}")
    public ResponseEntity<NsMysqlSctDto> getNsMysqlSct(@PathVariable("pk") Integer pk, @PathVariable("ck") Integer ck) throws CustomException {
        NsMysqlSctDto sctDto = NsMysqlSctDto.builder()
            .pk(pk)
                .ck(ck)
            .build();
        return ResponseEntity.ok(sctService.getNsMysqlSct(sctDto));
    }

    @PutMapping
    public ResponseEntity<ResponseStatusDto> putNsMysqlSct(@RequestBody NsMysqlSctDto sctDto) throws CustomException {
        return ResponseEntity.ok(sctService.putNsMysqlSct(sctDto));
    }

    @DeleteMapping("/{pk}/{ck}")
    public ResponseEntity<ResponseStatusDto> deleteNsMysqlSct(@PathVariable("pk") Integer pk, @PathVariable("ck") Integer ck) throws CustomException {
        NsMysqlSctDto sctDto = NsMysqlSctDto.builder()
            .pk(pk)
                .ck(ck)
            .build();
        return ResponseEntity.ok(sctService.deleteNsMysqlSct(sctDto));
    }

    @GetMapping("scanByPk/{pk}")
    public ResponseEntity<List<NsMysqlSctDto>> getNsMysqlSctByPk(@PathVariable("pk") Integer pk) throws CustomException {
        NsMysqlSctDto sctDto = NsMysqlSctDto.builder()
            .pk(pk)
            .build();
        return ResponseEntity.ok(sctService.getNsMysqlSctListByPk(sctDto));
    }

    @GetMapping("/scanAll")
    public ResponseEntity<List<NsMysqlSctDto>> getNsMysqlSctListAll() throws CustomException {
        return ResponseEntity.ok(sctService.getNsMysqlSctListAll());
    }

    @PostMapping("/executeSQL")
    public ResponseEntity<List<NsMysqlSctDto>> executeSQL(@RequestBody SqlCommandDto sqlCommandDto) throws CustomException {
        return ResponseEntity.ok(sctService.executeSQL(sqlCommandDto));
    }

    @ExceptionHandler(value = CustomException.class)
    public ResponseEntity<ResponseStatusDto> handleScalarDbException(CustomException ex) {
        // Modify the error message with more user-friendly details
        return switch (ex.getErrorCode()) {
            case 9100 -> new ResponseEntity<>(ResponseStatusDto.builder().code(ex.getErrorCode()).message(ex.getMessage()).build(), HttpStatus.BAD_REQUEST);
            case 9200, 9300 -> new ResponseEntity<>(ResponseStatusDto.builder().code(ex.getErrorCode()).message(ex.getMessage()).build(), HttpStatus.INTERNAL_SERVER_ERROR);
            case 9400 -> new ResponseEntity<>(ResponseStatusDto.builder().code(ex.getErrorCode()).message(ex.getMessage()).build(), HttpStatus.BAD_REQUEST);
            default -> new ResponseEntity<>(ResponseStatusDto.builder().code(ex.getErrorCode()).message(ex.getMessage()).build(), HttpStatus.INTERNAL_SERVER_ERROR);
        };
    }

    @PostMapping("/loadTest")
    public ResponseEntity<Map<String, Object>> loadTest(@RequestBody LoadTestDto loadTestDto) throws CustomException {
        Map<String, Object> result = new HashMap<>();

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
            executor.submit(() -> {
                AtomicInteger operationIndex = new AtomicInteger(0);

                while (System.currentTimeMillis() < testEndTime) {
                    long currentTime = System.currentTimeMillis();
                    boolean isStatisticsPeriod = currentTime >= rampUpEndTime;

                    // Generate CK using thread-based formula
                    int currentCk = (currentThreadId * 1000000) + operationIndex.getAndIncrement();

                    // Always perform INSERT
                    performInsert(loadTestDto.getPk(), currentCk, currentThreadId, operationIndex.get(),
                                isStatisticsPeriod, totalOperations, statisticsOperations,
                                statsInsertCount, statsInsertSuccess, statsInsertError, exceptionsRecorded, loadTestDto);

                    // Perform SELECT based on ratio
                    if (random.nextDouble() < loadTestDto.getSelectRatio()) {
                        performSelect(loadTestDto.getPk(), currentCk, currentThreadId, operationIndex.get(),
                                    isStatisticsPeriod, totalOperations, statisticsOperations,
                                    statsSelectCount, statsSelectSuccess, statsSelectError, exceptionsRecorded, loadTestDto);
                    }

                    // Perform UPDATE based on ratio (handle fractional)
                    double updateRatio = loadTestDto.getUpdateRatio();
                    int updateCount = (int) updateRatio;
                    double fractionalPart = updateRatio - updateCount;

                    // Guaranteed updates
                    for (int i = 0; i < updateCount; i++) {
                        performUpdate(loadTestDto.getPk(), currentCk, currentThreadId, operationIndex.get(), i,
                                    isStatisticsPeriod, totalOperations, statisticsOperations,
                                    statsUpdateCount, statsUpdateSuccess, statsUpdateError, exceptionsRecorded, loadTestDto);
                    }

                    // Fractional update
                    if (fractionalPart > 0 && random.nextDouble() < fractionalPart) {
                        performUpdate(loadTestDto.getPk(), currentCk, currentThreadId, operationIndex.get(), updateCount,
                                    isStatisticsPeriod, totalOperations, statisticsOperations,
                                    statsUpdateCount, statsUpdateSuccess, statsUpdateError, exceptionsRecorded, loadTestDto);
                    }

                    // Perform DELETE based on ratio
                    if (random.nextDouble() < loadTestDto.getDeleteRatio()) {
                        performDelete(loadTestDto.getPk(), currentCk, currentThreadId, operationIndex.get(),
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
            });
        }

        // Wait for all threads to complete
        executor.shutdown();
        try {
            executor.awaitTermination(loadTestDto.getTestDurationSeconds() + loadTestDto.getRampUpTimeSeconds() + 60, TimeUnit.SECONDS);
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

        // Build response
        result.put("totalOperations", totalOperations.get());
        result.put("statisticsOperations", statisticsOperations.get());
        result.put("operationCounts", Map.of(
            "insert", statsInsertCount.get(),
            "select", statsSelectCount.get(),
            "update", statsUpdateCount.get(),
            "delete", statsDeleteCount.get()
        ));
        result.put("successCounts", Map.of(
            "insert", statsInsertSuccess.get(),
            "select", statsSelectSuccess.get(),
            "update", statsUpdateSuccess.get(),
            "delete", statsDeleteSuccess.get()
        ));
        result.put("errorCounts", Map.of(
            "insert", statsInsertError.get(),
            "select", statsSelectError.get(),
            "update", statsUpdateError.get(),
            "delete", statsDeleteError.get()
        ));
        result.put("totalExecutionTimeMs", totalExecutionTime);
        result.put("statisticsExecutionTimeMs", statisticsTime);
        result.put("rampUpTimeMs", rampUpTime);
        result.put("throughputPerMinute", throughputPerMinute);
        result.put("ratios", Map.of(
            "select", loadTestDto.getSelectRatio(),
            "update", loadTestDto.getUpdateRatio(),
            "delete", loadTestDto.getDeleteRatio()
        ));
        result.put("threadCount", loadTestDto.getThreadCount());
        result.put("exceptionsRecorded", exceptionsRecorded.get());
        result.put("testDurationSeconds", loadTestDto.getTestDurationSeconds());
        result.put("actualTestDurationMs", statisticsTime);
        result.put("cleanupExecuted", cleanupExecuted);
        result.put("cleanupRecordsDeleted", cleanupRecordsDeleted);

        return ResponseEntity.ok(result);
    }

    private void performInsert(Integer pk, Integer ck, int threadId, int operationIndex,
                              boolean isStatisticsPeriod, AtomicLong totalOps, AtomicLong statsOps,
                              AtomicInteger count, AtomicInteger success, AtomicInteger error, AtomicLong exceptionsRecorded,
                              LoadTestDto loadTestDto) {
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
                              AtomicInteger count, AtomicInteger success, AtomicInteger error, AtomicLong exceptionsRecorded,
                              LoadTestDto loadTestDto) {
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

    private void performUpdate(Integer pk, Integer ck, int threadId, int operationIndex, int updateCount,
                              boolean isStatisticsPeriod, AtomicLong totalOps, AtomicLong statsOps,
                              AtomicInteger count, AtomicInteger success, AtomicInteger error, AtomicLong exceptionsRecorded,
                              LoadTestDto loadTestDto) {
        try {
            NsMysqlSctDto updateDto = NsMysqlSctDto.builder()
                .pk(pk)
                .ck(ck)
                .stringValue("Updated_T" + threadId + "_" + operationIndex + "_" + updateCount)
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
                              AtomicInteger count, AtomicInteger success, AtomicInteger error, AtomicLong exceptionsRecorded,
                              LoadTestDto loadTestDto) {
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
            // If we can't record the exception, just log it
            System.err.println("Failed to record exception: " + ex.getMessage());
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
                    System.err.println("Failed to delete sct record: " + e.getMessage());
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
                    System.err.println("Failed to delete error record: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Cleanup failed: " + e.getMessage());
        }
        return deletedRecords;
    }
