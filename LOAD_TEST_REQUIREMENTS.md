# Load Testing Requirements for ScalarDB CRUD REST API

## Overview
This document describes the requirements for implementing load testing functionality for the existing ScalarDB-based CRUD REST API application.

## Current System Analysis
The application contains two main controllers with standard CRUD operations:
- **NsMysqlSctController** (`/sct`) - handles NsMysqlSct entities
- **NsMysqlSctErrorController** (`/sctError`) - handles NsMysqlSctError entities

### Existing Data Models
#### NsMysqlSct
- `pk` (Integer) - Primary Key
- `ck` (Integer) - Cluster Key
- `stringValue` (String) - String data field
- `bintValue` (Long) - Long integer data field

#### NsMysqlSctError
- `pk` (Integer) - Primary Key
- `ck` (Integer) - Cluster Key
- `exception` (String) - Exception message
- `exceptionAt` (LocalDateTime) - Exception timestamp

## Requirements

### 1. Load Testing Method Implementation
Add a new `/loadTest` endpoint to the NsMysqlSctController that performs continuous CRUD operations using existing service methods.

#### 1.1 Endpoint Specifications
- **NsMysqlSctController**: `POST /sct/loadTest`
- **Focus**: Stress test operates exclusively on the `sct` table
- **Method Usage**: Uses existing service methods (`postNsMysqlSct`, `getNsMysqlSct`, `putNsMysqlSct`, `deleteNsMysqlSct`)

#### 1.2 Request Parameters
Create a `LoadTestDto` with the following fields:
- `testDurationSeconds` (Integer) - Total test duration in seconds (excluding ramp-up time)
- `selectRatio` (Double) - Ratio of select operations against insert operations
- `updateRatio` (Double) - Ratio of update operations against insert operations
- `deleteRatio` (Double) - Ratio of delete operations against insert operations
- `pk` (Integer) - Primary key value which is defined for each test case
- `startCk` (Integer) - Starting cluster key value
- `threadCount` (Integer) - Number of threads to use for concurrent execution
- `exceptionRetryInterval` (Long) - Wait interval in milliseconds when exception occurs
- `rampUpTimeSeconds` (Integer) - Ramp-up time in seconds before statistics collection begins
- `operationDelayMs` (Long) - Optional delay between operations in milliseconds (0 for no delay)
- `cleanupAfterTest` (Boolean) - Whether to delete test records related to pk after calculating statistics

### 2. Multi-Threading Support

#### 2.1 Thread-based CK Generation
The cluster key (CK) value should be generated using the following formula:
```
CK = (threadNumber * 1000000) + operationIndex
```

Where:
- `threadNumber`: Thread identifier (1, 2, 3, 4, etc.)
- `operationIndex`: Sequential counter for each operation within the thread (starts from 0)

#### 2.2 Thread CK Range Examples
- **4 threads, 60 seconds test duration**: 
  - Thread 1: CK 1000000+ (1000000, 1000001, 1000002, ...)
  - Thread 2: CK 2000000+ (2000000, 2000001, 2000002, ...)
  - Thread 3: CK 3000000+ (3000000, 3000001, 3000002, ...)
  - Thread 4: CK 4000000+ (4000000, 4000001, 4000002, ...)

- **Each thread continues generating operations until test duration expires**

#### 2.3 Time-based Test Execution
- **Test Flow**: Ramp-up period → Statistics collection period → Cleanup
- **Duration Control**: Each thread runs operations continuously until `testDurationSeconds` expires
- **Operation Timing**: Optional delay between operations using `operationDelayMs`
- **Graceful Shutdown**: Threads check time boundaries and stop gracefully

#### 2.4 Exception Handling with sct_error Integration
When any operation throws an exception:
1. Record the exception details to `sct_error` table with:
   - `pk`: Same as the failed operation
   - `ck`: Same as the failed operation
   - `exception`: Exception message and stack trace
   - `exceptionAt`: Current timestamp
2. Wait for `exceptionRetryInterval` milliseconds
3. Continue with the next operation
4. Do not terminate the entire load test

### 3. Operation Execution Logic

#### 3.1 Service Method Integration
The stress test uses existing service methods directly in a continuous loop:
```java
while (System.currentTimeMillis() < testEndTime) {
    // Always perform INSERT using existing service method
    sctService.postNsMysqlSct(insertDto);
    
    // Perform SELECT based on ratio using existing service method
    double selectRatio = loadTestDto.getSelectRatio();
    int selectLoops = (int) selectRatio; // Always execute this many times
    double selectFractional = selectRatio - selectLoops; // Fractional part for additional chance
    
    // Execute guaranteed select loops
    for (int i = 0; i < selectLoops; i++) {
        sctService.getNsMysqlSct(selectDto);
    }
    
    // Additional select loop based on fractional probability
    if (selectFractional > 0 && random.nextDouble() < selectFractional) {
        sctService.getNsMysqlSct(selectDto);
    }
    
    // Perform UPDATE based on ratio using existing service method
    double updateRatio = loadTestDto.getUpdateRatio();
    int updateLoops = (int) updateRatio; // Always execute this many times
    double updateFractional = updateRatio - updateLoops; // Fractional part for additional chance
    
    // Execute guaranteed update loops
    for (int i = 0; i < updateLoops; i++) {
        sctService.putNsMysqlSct(updateDto);
    }
    
    // Additional update loop based on fractional probability
    if (updateFractional > 0 && random.nextDouble() < updateFractional) {
        sctService.putNsMysqlSct(updateDto);
    }
    
    // Perform DELETE based on ratio using existing service method
    double deleteRatio = loadTestDto.getDeleteRatio();
    int deleteLoops = (int) deleteRatio; // Always execute this many times
    double deleteFractional = deleteRatio - deleteLoops; // Fractional part for additional chance
    
    // Execute guaranteed delete loops
    for (int i = 0; i < deleteLoops; i++) {
        sctService.deleteNsMysqlSct(deleteDto);
    }
    
    // Additional delete loop based on fractional probability
    if (deleteFractional > 0 && random.nextDouble() < deleteFractional) {
        sctService.deleteNsMysqlSct(deleteDto);
    }
    
    // Handle exception retry intervals and operation delays
}
```

**Simplified Logic Explanation:**
- **Integer part of ratio**: Always execute this many loops
- **Fractional part of ratio**: Random chance for one additional loop
- **Examples**:
  - `selectRatio = 2.3`: Always 2 selects + 30% chance for 1 more = average 2.3 selects per insert
  - `updateRatio = 0.7`: Always 0 updates + 70% chance for 1 more = average 0.7 updates per insert
  - `deleteRatio = 3.0`: Always 3 deletes + 0% chance for additional = exactly 3 deletes per insert

#### 3.2 Time-based Operation Flow
Each thread executes the following loop until test duration expires:
1. **Insert Operation**: Always execute (100% rate) - `sctService.postNsMysqlSct()`
2. **Select Operation**: Execute based on `selectRatio` probability - `sctService.getNsMysqlSct()`
3. **Update Operation**: Execute based on `updateRatio` (including fractional handling) - `sctService.putNsMysqlSct()`
4. **Delete Operation**: Execute based on `deleteRatio` probability - `sctService.deleteNsMysqlSct()`
5. **Exception Handling**: Record to sct_error and wait for retry interval
6. **Delay**: Wait for `operationDelayMs` if configured
7. **Time Check**: Continue if within test duration, otherwise exit gracefully

#### 3.3 Insert Operations
- Execute **every operation cycle** (100% execution rate)
- Call `sctService.postNsMysqlSct(insertDto)` with generated data
- Generate unique data for each operation using operation index
- Use thread-based CK calculation for unique key generation

#### 3.4 Select Operations
- Execute based on probability defined by `selectRatio`
- Call `sctService.getNsMysqlSct(selectDto)` with PK and CK
- Example: `selectRatio: 0.5` means 50% chance of execution per operation cycle

#### 3.5 Update Operations
- Handle fractional ratios (e.g., 1.5 means 1 guaranteed execution + 50% chance for additional execution)
- Call `sctService.putNsMysqlSct(updateDto)` with updated data
- Integer part: guaranteed number of executions per cycle
- Fractional part: probability of additional execution
- Example: `updateRatio: 1.5` means 1 update per cycle + 50% chance for second update

#### 3.6 Delete Operations
- Execute based on probability defined by `deleteRatio`
- Call `sctService.deleteNsMysqlSct(deleteDto)` with PK and CK
- Example: `deleteRatio: 0.2` means 20% chance of execution per operation cycle

### 4. Data Generation Strategy

#### 4.1 For NsMysqlSct
- **Insert**: 
  - `pk`: Use provided PK value
  - `ck`: Thread-based calculation (threadNumber * 1000000 + operationIndex)
  - `stringValue`: "LoadTest_T{threadNumber}_{operationIndex}"
  - `bintValue`: (threadNumber * 1000000) + operationIndex
- **Update**:
  - `pk`: Same as insert
  - `ck`: Same as insert
  - `stringValue`: "Updated_T{threadNumber}_{operationIndex}_{updateCount}" or "Updated_fractional_T{threadNumber}_{operationIndex}"
  - `bintValue`: calculated based on thread, operation index and update count

#### 4.2 For NsMysqlSctError
- **Insert**:
  - `pk`: Use provided PK value
  - `ck`: Thread-based calculation (threadNumber * 1000000 + operationIndex)
  - `exception`: "LoadTest_Exception_T{threadNumber}_{operationIndex}"
  - `exceptionAt`: current timestamp
- **Update**:
  - `pk`: Same as insert
  - `ck`: Same as insert
  - `exception`: "Updated_Exception_T{threadNumber}_{operationIndex}_{updateCount}" or "Updated_fractional_Exception_T{threadNumber}_{operationIndex}"
  - `exceptionAt`: current timestamp

#### 4.3 For Exception Recording
When recording exceptions to sct_error:
- `pk`: PK of the failed operation
- `ck`: CK of the failed operation
- `exception`: "{OperationType} failed: {ExceptionMessage}\nStackTrace: {StackTrace}"
- `exceptionAt`: Current timestamp

### 5. Ramp-up Time and Statistics Collection
- **Ramp-up Period**: Execute operations for `rampUpTimeSeconds` before starting statistics collection
- **Statistics Window**: Only count operations and measure performance after ramp-up period
- **Warm-up Benefits**: Allow system to reach steady state before measurement
- **Thread Coordination**: All threads must complete ramp-up before statistics collection begins
- **Time-based Control**: Both ramp-up and statistics periods are controlled by time duration, not loop count

### 6. Test Data Cleanup
- **Optional Cleanup**: When `cleanupAfterTest` is set to `true`, delete all test records after statistics calculation
- **Cleanup Scope**: Delete all records with the specified `pk` value from both main table and sct_error table
- **Timing**: Cleanup occurs after all statistics have been calculated and response is prepared
- **Multi-threading**: Cleanup runs on main thread after all worker threads complete
- **Error Handling**: Cleanup failures are logged but do not affect test results
- **Cleanup Process**:
  1. Calculate and prepare all statistics
  2. If `cleanupAfterTest` is true, delete all records where `pk` equals the test PK
  3. For NsMysqlSct tests: delete from both `sct` and `sct_error` tables
  4. For NsMysqlSctError tests: delete from `sct_error` table only
  5. Return response with cleanup status

### 7. Response Format
Return a comprehensive result object containing:
- `totalOperations` (Integer) - Total number of operations executed (including ramp-up)
- `statisticsOperations` (Integer) - Number of operations counted for statistics (after ramp-up)
- `operationCounts` (Map) - Actual count of each operation type executed (statistics period only)
  - `insert` (Integer)
  - `select` (Integer)
  - `update` (Integer)
  - `delete` (Integer)
- `successCounts` (Map) - Number of successful operations (statistics period only)
  - `insert` (Integer)
  - `select` (Integer)
  - `update` (Integer)
  - `delete` (Integer)
- `errorCounts` (Map) - Number of failed operations (statistics period only)
  - `insert` (Integer)
  - `select` (Integer)
  - `update` (Integer)
  - `delete` (Integer)
- `totalExecutionTimeMs` (Long) - Total execution time including ramp-up
- `statisticsExecutionTimeMs` (Long) - Execution time for statistics period only
- `rampUpTimeMs` (Long) - Actual ramp-up time in milliseconds
- `throughputPerMinute` (Double) - Average throughput (total operations per minute) during statistics period
- `ratios` (Map) - Configured ratios for reference
  - `select` (Double)
  - `update` (Double)
  - `delete` (Double)
- `threadCount` (Integer) - Number of threads used
- `exceptionsRecorded` (Integer) - Total number of exceptions recorded to sct_error table
- `testDurationSeconds` (Integer) - Configured test duration
- `actualTestDurationMs` (Long) - Actual test duration in milliseconds
- `cleanupExecuted` (Boolean) - Whether cleanup was performed
- `cleanupRecordsDeleted` (Integer) - Number of records deleted during cleanup (if executed)

### 8. Example Usage

#### 8.1 Request Example
```json
{
  "testDurationSeconds": 300,
  "selectRatio": 0.5,
  "updateRatio": 1.5,
  "deleteRatio": 0.2,
  "pk": 1000,
  "startCk": 1000,
  "threadCount": 4,
  "exceptionRetryInterval": 1000,
  "rampUpTimeSeconds": 30,
  "operationDelayMs": 100
}
```

#### 8.2 Expected Behavior
With the above configuration:
- **Ramp-up**: 30 seconds of operations before statistics collection
- **Test Duration**: 300 seconds (5 minutes) of statistics collection
- **Operation Delay**: 100ms delay between each operation cycle
- **Multi-threading**: 4 threads running concurrently
- **Insert**: Every operation cycle (100% rate)
- **Select**: 50% probability per cycle
- **Update**: 1 guaranteed + 50% chance for additional per cycle
- **Delete**: 20% probability per cycle

#### 8.3 Response Example
```json
{
  "totalOperations": 8800,
  "statisticsOperations": 7200,
  "operationCounts": {
    "insert": 7200,
    "select": 3600,
    "update": 10800,
    "delete": 1440
  },
  "successCounts": {
    "insert": 7180,
    "select": 3600,
    "update": 10750,
    "delete": 1430
  },
  "errorCounts": {
    "insert": 20,
    "select": 0,
    "update": 50,
    "delete": 10
  },
  "totalExecutionTimeMs": 330000,
  "statisticsExecutionTimeMs": 300000,
  "rampUpTimeMs": 30000,
  "throughputPerMinute": 4608.0,
  "ratios": {
    "select": 0.5,
    "update": 1.5,
    "delete": 0.2
  },
  "threadCount": 4,
  "exceptionsRecorded": 80,
  "testDurationSeconds": 300,
  "actualTestDurationMs": 300045,
  "cleanupExecuted": true,
  "cleanupRecordsDeleted": 100
}
```

## Implementation Details

### 9. Technical Considerations

#### 9.1 Key Generation
- Use `startPk + loopIndex` and `startCk + loopIndex` for unique key generation
- Ensure no key conflicts during execution

#### 9.2 Random Number Generation
- Use `java.util.Random` for probability-based operation execution
- Initialize once per controller instance

#### 9.3 Performance Measurement
- Measure execution time using `System.currentTimeMillis()`
- Start timing before loop execution, end after completion

#### 9.4 Transaction Handling
- Leverage existing service layer transaction management
- Each operation should be independent (no cross-operation transactions)

### 10. Testing Scenarios

#### 10.1 Basic Load Test
- Small loop count (10-50) for functional verification
- Balanced ratios (all around 1.0)

#### 10.2 Read-Heavy Load Test
- High select ratio (2.0-5.0)
- Low update/delete ratios (0.1-0.5)

#### 10.3 Write-Heavy Load Test
- High update ratio (2.0-3.0)
- Low select/delete ratios (0.1-0.5)

#### 10.4 Mixed Workload Test
- Fractional ratios to test probability logic
- Example: select: 0.7, update: 1.3, delete: 0.4

## Acceptance Criteria

1. ✅ Load testing endpoints are available on both controllers
2. ✅ All CRUD operations execute according to specified ratios
3. ✅ Fractional ratios are handled correctly (integer + probability)
4. ✅ Error handling prevents single failures from stopping the test
5. ✅ Response includes comprehensive execution statistics
6. ✅ Key generation ensures uniqueness across loop iterations
7. ✅ Performance metrics are accurately measured and reported

## Future Enhancements (Optional)

### 11.1 Advanced Features
- Concurrent execution support
- Custom data generation strategies
- Detailed error reporting
- Performance metrics (operations per second)
- Configurable delay between operations

### 11.2 Monitoring Integration
- Integration with application monitoring tools
- Real-time progress reporting
- Resource usage tracking during load tests
