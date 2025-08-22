#!/bin/bash

# Multi Load Test Script
# Runs multiple load tests with specified parameters and saves results

echo "Multi Load Test Configuration"
echo "============================="

# Which load tests
echo ""
echo "Which load tests: (default value)"
read -p "/sct/loadTest: [yes]: " SCT_LOADTEST
SCT_LOADTEST=${SCT_LOADTEST:-yes}

read -p "/sct/loadTest/sql: [yes]: " SCT_LOADTEST_SQL
SCT_LOADTEST_SQL=${SCT_LOADTEST_SQL:-yes}

read -p "/jdbc/sct/loadtest: [yes]: " JDBC_LOADTEST
JDBC_LOADTEST=${JDBC_LOADTEST:-yes}

# Target host
echo ""
read -p "The target host: [localhost]: " TARGET_HOST
TARGET_HOST=${TARGET_HOST:-localhost}
PORT="8080"
BASE_URL="http://${TARGET_HOST}:${PORT}"

# Common parameters
echo ""
echo "Common parameters for each test: (default value)"
read -p "testDurationSeconds: [300]: " TEST_DURATION_SECONDS
TEST_DURATION_SECONDS=${TEST_DURATION_SECONDS:-300}

read -p "selectRatio: [1]: " SELECT_RATIO
SELECT_RATIO=${SELECT_RATIO:-1}

read -p "updateRatio: [1]: " UPDATE_RATIO
UPDATE_RATIO=${UPDATE_RATIO:-1}

read -p "deleteRatio: [1]: " DELETE_RATIO
DELETE_RATIO=${DELETE_RATIO:-1}

read -p "threadCount: [1]: " THREAD_COUNT
THREAD_COUNT=${THREAD_COUNT:-1}

read -p "rampUpTimeSeconds: [30]: " RAMP_UP_TIME_SECONDS
RAMP_UP_TIME_SECONDS=${RAMP_UP_TIME_SECONDS:-30}

# How many times
echo ""
read -p "How many times(hmt): [5]: " HOW_MANY_TIMES
HOW_MANY_TIMES=${HOW_MANY_TIMES:-5}

# Fixed parameters
INSERT_RATIO=1
INTERVAL_SECONDS=30

echo ""
echo "Configuration Summary:"
echo "====================="
echo "Load tests enabled:"
echo "  /sct/loadTest: $SCT_LOADTEST"
echo "  /sct/loadTest/sql: $SCT_LOADTEST_SQL"
echo "  /jdbc/sct/loadtest: $JDBC_LOADTEST"
echo "Target host: $TARGET_HOST"
echo "Common parameters:"
echo "  testDurationSeconds: $TEST_DURATION_SECONDS"
echo "  insertRatio: $INSERT_RATIO"
echo "  selectRatio: $SELECT_RATIO"
echo "  updateRatio: $UPDATE_RATIO"
echo "  deleteRatio: $DELETE_RATIO"
echo "  threadCount: $THREAD_COUNT"
echo "  rampUpTimeSeconds: $RAMP_UP_TIME_SECONDS"
echo "Iterations: $HOW_MANY_TIMES"
echo ""
read -p "Continue with these settings? [y/N]: " CONFIRM
if [[ ! "$CONFIRM" =~ ^[Yy]$ ]]; then
    echo "Aborted."
    exit 1
fi

# Result files
LOADTEST_TRUE_FILE="loadTest_true.json"
LOADTEST_FALSE_FILE="loadTest_false.json"
LOADTEST_SQL_TRUE_FILE="loadTest_sql_true.json"
LOADTEST_SQL_FALSE_FILE="loadTest_sql_false.json"
JDBC_LOADTEST_TRUE_FILE="jdbc_loadtest_true.json"
JDBC_LOADTEST_FALSE_FILE="jdbc_loadtest_false.json"

# Initialize result files with empty arrays
echo "[]" > "$LOADTEST_TRUE_FILE"
echo "[]" > "$LOADTEST_FALSE_FILE"
echo "[]" > "$LOADTEST_SQL_TRUE_FILE"
echo "[]" > "$LOADTEST_SQL_FALSE_FILE"
echo "[]" > "$JDBC_LOADTEST_TRUE_FILE"
echo "[]" > "$JDBC_LOADTEST_FALSE_FILE"

# Function to append result to JSON array file
append_result() {
    local file="$1"
    local result="$2"
    
    # Read current content, remove closing bracket, add new result, close array
    local current_content=$(cat "$file" | sed '$ s/]//')
    if [[ "$current_content" == "[" ]]; then
        # First entry
        echo "[$result]" > "$file"
    else
        # Subsequent entries
        echo "${current_content}, $result]" > "$file"
    fi
}

# Function to run load test and save result
run_load_test() {
    local endpoint="$1"
    local is_oo="$2"
    local result_file="$3"
    local test_name="$4"
    
    echo "Running $test_name (isOO: $is_oo)..."
    
    # Prepare JSON payload based on endpoint
    if [[ "$endpoint" == *"jdbc"* ]]; then
        # JDBC endpoint doesn't use isOO parameter
        local payload="{
            \"testDurationSeconds\": $TEST_DURATION_SECONDS,
            \"selectRatio\": $SELECT_RATIO,
            \"updateRatio\": $UPDATE_RATIO,
            \"deleteRatio\": $DELETE_RATIO,
            \"insertRatio\": $INSERT_RATIO,
            \"threadCount\": $THREAD_COUNT,
            \"rampUpTimeSeconds\": $RAMP_UP_TIME_SECONDS,
            \"pk\": 1,
            \"startCk\": 1,
            \"exceptionRetryInterval\": 1000,
            \"operationDelayMs\": 0,
            \"cleanupAfterTest\": true
        }"
    else
        local payload="{
            \"testDurationSeconds\": $TEST_DURATION_SECONDS,
            \"selectRatio\": $SELECT_RATIO,
            \"updateRatio\": $UPDATE_RATIO,
            \"deleteRatio\": $DELETE_RATIO,
            \"insertRatio\": $INSERT_RATIO,
            \"threadCount\": $THREAD_COUNT,
            \"rampUpTimeSeconds\": $RAMP_UP_TIME_SECONDS,
            \"pk\": 1,
            \"startCk\": 1,
            \"exceptionRetryInterval\": 1000,
            \"operationDelayMs\": 0,
            \"cleanupAfterTest\": true,
            \"isOO\": $is_oo
        }"
    fi
    
    # Execute the load test
    local response=$(curl -s -X POST \
        -H "Content-Type: application/json" \
        -d "$payload" \
        "$BASE_URL$endpoint")
    
    if [[ $? -eq 0 ]] && [[ -n "$response" ]]; then
        echo "Test completed successfully"
        append_result "$result_file" "$response"
    else
        echo "Test failed or returned empty response"
        echo "Response: $response"
    fi
}

# Function to wait with countdown
wait_with_countdown() {
    local seconds="$1"
    echo "Waiting $seconds seconds before next test..."
    for ((i=seconds; i>0; i--)); do
        echo -ne "\rTime remaining: $i seconds "
        sleep 1
    done
    echo -e "\rStarting next test...                "
}

# Calculate total tests
TESTS_PER_ITERATION=0
[[ "$SCT_LOADTEST" =~ ^[Yy] ]] && TESTS_PER_ITERATION=$((TESTS_PER_ITERATION + 2))
[[ "$SCT_LOADTEST_SQL" =~ ^[Yy] ]] && TESTS_PER_ITERATION=$((TESTS_PER_ITERATION + 2))
[[ "$JDBC_LOADTEST" =~ ^[Yy] ]] && TESTS_PER_ITERATION=$((TESTS_PER_ITERATION + 2))

# Main execution loop
echo "Starting multi load test execution"
echo "Total iterations: $HOW_MANY_TIMES"
echo "Tests per iteration: $TESTS_PER_ITERATION"
echo "Total tests: $((HOW_MANY_TIMES * TESTS_PER_ITERATION))"
echo "=========================================="

for ((iteration=1; iteration<=HOW_MANY_TIMES; iteration++)); do
    echo ""
    echo "=== Iteration $iteration/$HOW_MANY_TIMES ==="
    
    test_count=0
    total_tests_this_iteration=$TESTS_PER_ITERATION
    
    # Test 1: /sct/loadTest (isOO: true)
    if [[ "$SCT_LOADTEST" =~ ^[Yy] ]]; then
        test_count=$((test_count + 1))
        run_load_test "/sct/loadTest" "true" "$LOADTEST_TRUE_FILE" "SCT LoadTest"
        if [[ $test_count -lt $total_tests_this_iteration || $iteration -lt $HOW_MANY_TIMES ]]; then
            wait_with_countdown $INTERVAL_SECONDS
        fi
        
        # Test 2: /sct/loadTest (isOO: false)
        test_count=$((test_count + 1))
        run_load_test "/sct/loadTest" "false" "$LOADTEST_FALSE_FILE" "SCT LoadTest"
        if [[ $test_count -lt $total_tests_this_iteration || $iteration -lt $HOW_MANY_TIMES ]]; then
            wait_with_countdown $INTERVAL_SECONDS
        fi
    fi
    
    # Test 3: /sct/loadTest/sql (isOO: true)
    if [[ "$SCT_LOADTEST_SQL" =~ ^[Yy] ]]; then
        test_count=$((test_count + 1))
        run_load_test "/sct/loadTest/sql" "true" "$LOADTEST_SQL_TRUE_FILE" "SCT SQL LoadTest"
        if [[ $test_count -lt $total_tests_this_iteration || $iteration -lt $HOW_MANY_TIMES ]]; then
            wait_with_countdown $INTERVAL_SECONDS
        fi
        
        # Test 4: /sct/loadTest/sql (isOO: false)
        test_count=$((test_count + 1))
        run_load_test "/sct/loadTest/sql" "false" "$LOADTEST_SQL_FALSE_FILE" "SCT SQL LoadTest"
        if [[ $test_count -lt $total_tests_this_iteration || $iteration -lt $HOW_MANY_TIMES ]]; then
            wait_with_countdown $INTERVAL_SECONDS
        fi
    fi
    
    # Test 5: /jdbc/sct/loadtest (isOO: true - note: JDBC doesn't use isOO)
    if [[ "$JDBC_LOADTEST" =~ ^[Yy] ]]; then
        test_count=$((test_count + 1))
        run_load_test "/jdbc/sct/loadtest" "true" "$JDBC_LOADTEST_TRUE_FILE" "JDBC LoadTest"
        if [[ $test_count -lt $total_tests_this_iteration || $iteration -lt $HOW_MANY_TIMES ]]; then
            wait_with_countdown $INTERVAL_SECONDS
        fi
        
        # Test 6: /jdbc/sct/loadtest (isOO: false - note: JDBC doesn't use isOO)
        test_count=$((test_count + 1))
        run_load_test "/jdbc/sct/loadtest" "false" "$JDBC_LOADTEST_FALSE_FILE" "JDBC LoadTest"
        if [[ $test_count -lt $total_tests_this_iteration || $iteration -lt $HOW_MANY_TIMES ]]; then
            wait_with_countdown $INTERVAL_SECONDS
        fi
    fi
done

echo ""
echo "=========================================="
echo "All load tests completed!"
echo "Results saved to:"
echo "  - $LOADTEST_TRUE_FILE"
echo "  - $LOADTEST_FALSE_FILE"
echo "  - $LOADTEST_SQL_TRUE_FILE"
echo "  - $LOADTEST_SQL_FALSE_FILE"
echo "  - $JDBC_LOADTEST_TRUE_FILE"
echo "  - $JDBC_LOADTEST_FALSE_FILE"