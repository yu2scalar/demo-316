# Load Test Implementation Tasks

This document outlines the implementation tasks for the ScalarDB CRUD REST API load testing functionality as specified in `LOAD_TEST_REQUIREMENTS.md`.

## High Priority Tasks

### 1. Create LoadTestDto
**File**: `src/main/java/com/example/demo_316/dto/LoadTestDto.java`

Create a DTO with the following fields:
- `testDurationSeconds` (Integer) - Total test duration in seconds
- `selectRatio` (Double) - Ratio of select operations against insert operations
- `updateRatio` (Double) - Ratio of update operations against insert operations  
- `deleteRatio` (Double) - Ratio of delete operations against insert operations
- `pk` (Integer) - Primary key value for the test case
- `startCk` (Integer) - Starting cluster key value
- `threadCount` (Integer) - Number of threads for concurrent execution
- `exceptionRetryInterval` (Long) - Wait interval in milliseconds when exception occurs
- `rampUpTimeSeconds` (Integer) - Ramp-up time before statistics collection
- `operationDelayMs` (Long) - Optional delay between operations (0 for no delay)
- `cleanupAfterTest` (Boolean) - Whether to delete test records after test

**Requirements**:
- Use Lombok annotations (@Data, @Builder, @NoArgsConstructor, @AllArgsConstructor)
- Add validation annotations where appropriate

### 2. Implement Load Test Endpoint
**File**: `src/main/java/com/example/demo_316/controller/NsMysqlSctController.java`

Add new endpoint:
- **Method**: `POST /sct/loadTest`
- **Request Body**: `LoadTestDto`
- **Response**: `LoadTestResultDto` (to be created)
- **Exception Handling**: Integrate with existing exception handler

**Requirements**:
- Use existing service methods for CRUD operations
- Handle CustomException appropriately
- Return comprehensive test results

### 3. Create Multi-threaded Load Test Service
**File**: `src/main/java/com/example/demo_316/service/LoadTestService.java`

Implement service with:
- Multi-threaded execution using `ExecutorService`
- Thread-safe statistics collection
- Time-based test execution control
- Integration with existing NsMysqlSctService methods

**Key Methods**:
- `executeLoadTest(LoadTestDto loadTestDto)` - Main load test execution
- `executeWorkerThread(...)` - Individual thread execution logic
- `calculateStatistics(...)` - Statistics aggregation
- `cleanupTestData(...)` - Optional cleanup functionality

### 4. Implement Thread-safe CK Generation
**Location**: Within `LoadTestService`

Implement cluster key generation using formula:
```
CK = (threadNumber * 1000000) + operationIndex
```

**Requirements**:
- Thread-safe operation index tracking
- Unique CK generation per thread
- Support for multiple concurrent threads
- Starting from threadNumber 1, 2, 3, etc.

## Medium Priority Tasks

### 5. Add Operation Ratio Handling with Fractional Support
**Location**: Within worker thread execution logic

Implement fractional ratio handling:
- **Integer part**: Always execute this many operations
- **Fractional part**: Random probability for additional operation
- **Example**: `selectRatio = 2.3` → Always 2 selects + 30% chance for 1 more

**Implementation**:
```java
int guaranteedOps = (int) ratio;
double fractionalPart = ratio - guaranteedOps;
// Execute guaranteed operations
// Execute additional operation based on probability
```

### 6. Implement Exception Handling and Retry Mechanism
**Location**: Within worker thread execution logic

Exception handling requirements:
- Record exceptions to `sct_error` table using existing service
- Wait for `exceptionRetryInterval` milliseconds
- Continue with next operation (don't terminate test)
- Track exception counts for statistics

**Error Recording Fields**:
- `pk`: Same as failed operation
- `ck`: Same as failed operation  
- `exception`: "{OperationType} failed: {ExceptionMessage}\nStackTrace: {StackTrace}"
- `exceptionAt`: Current timestamp

### 7. Add Ramp-up Time and Statistics Collection
**Location**: Within `LoadTestService`

Implement two-phase execution:
- **Ramp-up Phase**: Execute operations for `rampUpTimeSeconds` without counting statistics
- **Statistics Phase**: Execute operations for `testDurationSeconds` while collecting metrics
- **Thread Coordination**: All threads must complete ramp-up before statistics collection

**Time Management**:
- Use `System.currentTimeMillis()` for timing control
- Graceful thread shutdown when time expires
- Separate counters for ramp-up vs statistics periods

### 8. Create Comprehensive Response DTO
**File**: `src/main/java/com/example/demo_316/dto/LoadTestResultDto.java`

Create response DTO with fields:
- `totalOperations` (Integer) - Total operations including ramp-up
- `statisticsOperations` (Integer) - Operations counted for statistics
- `operationCounts` (Map<String, Integer>) - Actual count per operation type
- `successCounts` (Map<String, Integer>) - Successful operations per type
- `errorCounts` (Map<String, Integer>) - Failed operations per type
- `totalExecutionTimeMs` (Long) - Total time including ramp-up
- `statisticsExecutionTimeMs` (Long) - Statistics period time only
- `rampUpTimeMs` (Long) - Actual ramp-up time
- `throughputPerMinute` (Double) - Operations per minute during statistics period
- `ratios` (Map<String, Double>) - Configured ratios for reference
- `threadCount` (Integer) - Number of threads used
- `exceptionsRecorded` (Integer) - Total exceptions recorded to sct_error
- `testDurationSeconds` (Integer) - Configured test duration
- `actualTestDurationMs` (Long) - Actual test duration
- `cleanupExecuted` (Boolean) - Whether cleanup was performed
- `cleanupRecordsDeleted` (Integer) - Records deleted during cleanup

## Low Priority Tasks

### 9. Implement Optional Test Data Cleanup
**Location**: Within `LoadTestService`

Cleanup functionality:
- Execute after statistics calculation
- Delete all records with specified `pk` from both tables
- Run on main thread after worker threads complete
- Log cleanup failures without affecting test results
- Return cleanup status in response

**Cleanup Process**:
1. Calculate all statistics
2. If `cleanupAfterTest` is true, delete records where `pk` equals test PK
3. Delete from both `sct` and `sct_error` tables
4. Return response with cleanup status

### 10. Add Data Generation Strategies
**Location**: Within worker thread execution logic

Implement data generation for test entities:

**NsMysqlSct Insert Data**:
- `pk`: Use provided PK value
- `ck`: Thread-based calculation  
- `stringValue`: "LoadTest_T{threadNumber}_{operationIndex}"
- `bintValue`: (threadNumber * 1000000) + operationIndex

**NsMysqlSct Update Data**:
- `pk`: Same as insert
- `ck`: Same as insert
- `stringValue`: "Updated_T{threadNumber}_{operationIndex}_{updateCount}"
- `bintValue`: Updated calculation based on thread and operation

## Implementation Order

1. **Phase 1**: Tasks 1-4 (Core functionality)
2. **Phase 2**: Tasks 5-8 (Advanced features)  
3. **Phase 3**: Tasks 9-10 (Enhancement features)

## Testing Strategy

### Unit Testing
- Test individual components (DTO validation, CK generation, ratio calculations)
- Mock external dependencies (ScalarDB services)
- Test exception handling scenarios

### Integration Testing
- Test with small thread counts and short durations
- Verify statistics accuracy
- Test cleanup functionality
- Validate exception recording

### Load Testing Scenarios
1. **Basic Test**: 1 thread, 10 seconds, balanced ratios
2. **Multi-thread Test**: 4 threads, 30 seconds, various ratios
3. **Exception Test**: Introduce failures, verify error handling
4. **Cleanup Test**: Verify test data removal

## Notes

- Leverage existing service methods (`postNsMysqlSct`, `getNsMysqlSct`, `putNsMysqlSct`, `deleteNsMysqlSct`)
- Follow existing code patterns and conventions
- Use existing exception handling and transaction management
- Maintain thread safety throughout implementation
- Ensure graceful shutdown and resource cleanup