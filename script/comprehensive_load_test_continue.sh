#!/bin/bash

# Comprehensive Load Test - CONTINUATION
# Continue from SQL_OO_FALSE iteration 9 through JDBC completion
BASE_URL="http://localhost:8080/sct"
OUTPUT_CSV="comprehensive_load_test_results.csv"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

log() {
    echo -e "${GREEN}[$(date '+%Y-%m-%d %H:%M:%S')]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

# Check server
check_server() {
    # Check if server is responding
    curl -s --max-time 5 "$BASE_URL/scanAll" >/dev/null 2>&1
    return $?
}

# Execute load test
execute_load_test() {
    local endpoint="$1"
    local json_payload="$2"
    local scenario_name="$3"
    local iteration="$4"
    local progress_current="$5"
    local progress_total="$6"
    
    log "Running $scenario_name - Iteration $iteration"
    
    local response=$(curl -s -X POST \
        -H "Content-Type: application/json" \
        -d "$json_payload" \
        "$BASE_URL$endpoint" 2>/dev/null)
    
    if [ $? -ne 0 ]; then
        error "Failed to execute load test for $scenario_name iteration $iteration"
        return 1
    fi
    
    # Extract metrics
    local total_ops=$(echo "$response" | jq -r '.totalOperations // "ERROR"')
    local stats_ops=$(echo "$response" | jq -r '.statisticsOperations // "ERROR"')
    local total_time=$(echo "$response" | jq -r '.totalExecutionTimeMs // "ERROR"')
    local stats_time=$(echo "$response" | jq -r '.statisticsExecutionTimeMs // "ERROR"')
    local ramp_time=$(echo "$response" | jq -r '.rampUpTimeMs // "ERROR"')
    local throughput=$(echo "$response" | jq -r '.throughputPerMinute // "ERROR"')
    
    # Sum success counts
    local insert_success=$(echo "$response" | jq -r '.successCounts.insert // 0')
    local select_success=$(echo "$response" | jq -r '.successCounts.select // 0')
    local update_success=$(echo "$response" | jq -r '.successCounts.update // 0')
    local delete_success=$(echo "$response" | jq -r '.successCounts.delete // 0')
    local total_success=$((insert_success + select_success + update_success + delete_success))
    
    # Sum error counts
    local insert_error=$(echo "$response" | jq -r '.errorCounts.insert // 0')
    local select_error=$(echo "$response" | jq -r '.errorCounts.select // 0')
    local update_error=$(echo "$response" | jq -r '.errorCounts.update // 0')
    local delete_error=$(echo "$response" | jq -r '.errorCounts.delete // 0')
    local total_error=$((insert_error + select_error + update_error + delete_error))
    
    if [ "$total_ops" = "ERROR" ]; then
        error "Failed to parse response for $scenario_name iteration $iteration"
        echo "Response: $response" | head -500
        return 1
    fi
    
    # Append to existing CSV (don't overwrite header)
    echo "$scenario_name,$iteration,$total_ops,$stats_ops,$total_time,$stats_time,$ramp_time,$throughput,$total_success,$total_error" >> "$OUTPUT_CSV"
    
    log "$scenario_name Iteration $iteration: $stats_ops ops, ${throughput} ops/min, Success: $total_success, Errors: $total_error"
    log "Progress: $progress_current/$progress_total tests completed"
    
    sleep 3
    return 0
}

main() {
    log "Starting Comprehensive Load Test - CONTINUATION"
    info "Continuing from SQL_OO_FALSE iteration 9 through JDBC completion"
    
    # Check dependencies
    if ! command -v jq &> /dev/null; then
        error "jq is required. Install: sudo apt-get install jq"
        exit 1
    fi
    
    # Check server
    if ! check_server; then
        error "Server not running. Start with: ./gradlew bootRun"
        exit 1
    fi
    
    # Check if CSV exists
    if [ ! -f "$OUTPUT_CSV" ]; then
        error "CSV file $OUTPUT_CSV not found. Run the main test first."
        exit 1
    fi
    
    info "Continuing test - appending to existing $OUTPUT_CSV"
    
    # Count current progress from existing CSV
    local existing_count=$(tail -n +2 "$OUTPUT_CSV" | wc -l)
    info "Found $existing_count existing test results"
    
    local start_time=$(date +%s)
    local current_test=$existing_count
    local total_tests=50
    
    # Remaining tests to complete
    local remaining_tests=(
        "/loadTest/sql|SQL_OO_FALSE|{\"testDurationSeconds\":60,\"selectRatio\":1,\"updateRatio\":1,\"deleteRatio\":1,\"pk\":1004,\"startCk\":0,\"threadCount\":10,\"exceptionRetryInterval\":0,\"rampUpTimeSeconds\":30,\"operationDelayMs\":0,\"cleanupAfterTest\":true,\"isOO\":false}|9"
        "/loadTest/sql|SQL_OO_FALSE|{\"testDurationSeconds\":60,\"selectRatio\":1,\"updateRatio\":1,\"deleteRatio\":1,\"pk\":1004,\"startCk\":0,\"threadCount\":10,\"exceptionRetryInterval\":0,\"rampUpTimeSeconds\":30,\"operationDelayMs\":0,\"cleanupAfterTest\":true,\"isOO\":false}|10"
        "/loadTest/jdbc|JDBC_LOAD_TEST|{\"testDurationSeconds\":60,\"selectRatio\":1,\"updateRatio\":1,\"deleteRatio\":1,\"pk\":1005,\"startCk\":0,\"threadCount\":10,\"exceptionRetryInterval\":0,\"rampUpTimeSeconds\":30,\"operationDelayMs\":0,\"cleanupAfterTest\":true,\"isOO\":false}|1"
        "/loadTest/jdbc|JDBC_LOAD_TEST|{\"testDurationSeconds\":60,\"selectRatio\":1,\"updateRatio\":1,\"deleteRatio\":1,\"pk\":1005,\"startCk\":0,\"threadCount\":10,\"exceptionRetryInterval\":0,\"rampUpTimeSeconds\":30,\"operationDelayMs\":0,\"cleanupAfterTest\":true,\"isOO\":false}|2"
        "/loadTest/jdbc|JDBC_LOAD_TEST|{\"testDurationSeconds\":60,\"selectRatio\":1,\"updateRatio\":1,\"deleteRatio\":1,\"pk\":1005,\"startCk\":0,\"threadCount\":10,\"exceptionRetryInterval\":0,\"rampUpTimeSeconds\":30,\"operationDelayMs\":0,\"cleanupAfterTest\":true,\"isOO\":false}|3"
        "/loadTest/jdbc|JDBC_LOAD_TEST|{\"testDurationSeconds\":60,\"selectRatio\":1,\"updateRatio\":1,\"deleteRatio\":1,\"pk\":1005,\"startCk\":0,\"threadCount\":10,\"exceptionRetryInterval\":0,\"rampUpTimeSeconds\":30,\"operationDelayMs\":0,\"cleanupAfterTest\":true,\"isOO\":false}|4"
        "/loadTest/jdbc|JDBC_LOAD_TEST|{\"testDurationSeconds\":60,\"selectRatio\":1,\"updateRatio\":1,\"deleteRatio\":1,\"pk\":1005,\"startCk\":0,\"threadCount\":10,\"exceptionRetryInterval\":0,\"rampUpTimeSeconds\":30,\"operationDelayMs\":0,\"cleanupAfterTest\":true,\"isOO\":false}|5"
        "/loadTest/jdbc|JDBC_LOAD_TEST|{\"testDurationSeconds\":60,\"selectRatio\":1,\"updateRatio\":1,\"deleteRatio\":1,\"pk\":1005,\"startCk\":0,\"threadCount\":10,\"exceptionRetryInterval\":0,\"rampUpTimeSeconds\":30,\"operationDelayMs\":0,\"cleanupAfterTest\":true,\"isOO\":false}|6"
        "/loadTest/jdbc|JDBC_LOAD_TEST|{\"testDurationSeconds\":60,\"selectRatio\":1,\"updateRatio\":1,\"deleteRatio\":1,\"pk\":1005,\"startCk\":0,\"threadCount\":10,\"exceptionRetryInterval\":0,\"rampUpTimeSeconds\":30,\"operationDelayMs\":0,\"cleanupAfterTest\":true,\"isOO\":false}|7"
        "/loadTest/jdbc|JDBC_LOAD_TEST|{\"testDurationSeconds\":60,\"selectRatio\":1,\"updateRatio\":1,\"deleteRatio\":1,\"pk\":1005,\"startCk\":0,\"threadCount\":10,\"exceptionRetryInterval\":0,\"rampUpTimeSeconds\":30,\"operationDelayMs\":0,\"cleanupAfterTest\":true,\"isOO\":false}|8"
        "/loadTest/jdbc|JDBC_LOAD_TEST|{\"testDurationSeconds\":60,\"selectRatio\":1,\"updateRatio\":1,\"deleteRatio\":1,\"pk\":1005,\"startCk\":0,\"threadCount\":10,\"exceptionRetryInterval\":0,\"rampUpTimeSeconds\":30,\"operationDelayMs\":0,\"cleanupAfterTest\":true,\"isOO\":false}|9"
        "/loadTest/jdbc|JDBC_LOAD_TEST|{\"testDurationSeconds\":60,\"selectRatio\":1,\"updateRatio\":1,\"deleteRatio\":1,\"pk\":1005,\"startCk\":0,\"threadCount\":10,\"exceptionRetryInterval\":0,\"rampUpTimeSeconds\":30,\"operationDelayMs\":0,\"cleanupAfterTest\":true,\"isOO\":false}|10"
    )
    
    info "=== Continuing SQL_OO_FALSE - Iterations 9-10 ==="
    
    # Execute remaining tests
    for test_def in "${remaining_tests[@]}"; do
        IFS='|' read -r endpoint name payload iteration <<< "$test_def"
        
        ((current_test++))
        
        if ! execute_load_test "$endpoint" "$payload" "$name" "$iteration" "$current_test" "$total_tests"; then
            error "Failed $name iteration $iteration"
        fi
        
        # Log milestone when starting JDBC
        if [ "$name" = "JDBC_LOAD_TEST" ] && [ "$iteration" = "1" ]; then
            info "=== Starting JDBC_LOAD_TEST - 10 iterations ==="
        fi
    done
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    log "Continuation completed! Duration: ${duration}s"
    log "Final results saved to: $OUTPUT_CSV"
    
    # Generate final comprehensive summary
    echo ""
    echo "=== FINAL COMPREHENSIVE LOAD TEST RESULTS ==="
    awk -F',' '
    NR>1 {
        scenario_ops[$1] += $4
        scenario_throughput[$1] += $8  
        scenario_success[$1] += $9
        scenario_error[$1] += $10
        scenario_count[$1]++
    }
    END {
        printf "%-16s %8s %12s %15s %12s %10s\n", "Scenario", "Tests", "Avg Ops", "Avg Throughput", "Avg Success", "Avg Errors"
        printf "%-16s %8s %12s %15s %12s %10s\n", "--------", "-----", "-------", "--------------", "-----------", "----------"
        for (s in scenario_count) {
            printf "%-16s %8d %12.0f %15.2f %12.0f %10.0f\n", 
                s, 
                scenario_count[s],
                scenario_ops[s]/scenario_count[s], 
                scenario_throughput[s]/scenario_count[s],
                scenario_success[s]/scenario_count[s],
                scenario_error[s]/scenario_count[s]
            
            avg_ops[s] = scenario_ops[s]/scenario_count[s]
            avg_throughput[s] = scenario_throughput[s]/scenario_count[s]
        }
        
        print ""
        print "=== FINAL PERFORMANCE RANKING ==="
        
        # Sort scenarios by throughput
        n = 0
        for (s in avg_throughput) {
            scenarios[n] = s
            throughputs[n] = avg_throughput[s]
            n++
        }
        
        # Simple bubble sort by throughput (descending)
        for (i = 0; i < n-1; i++) {
            for (j = 0; j < n-i-1; j++) {
                if (throughputs[j] < throughputs[j+1]) {
                    # Swap throughputs
                    temp_t = throughputs[j]
                    throughputs[j] = throughputs[j+1]
                    throughputs[j+1] = temp_t
                    # Swap scenarios
                    temp_s = scenarios[j]
                    scenarios[j] = scenarios[j+1]
                    scenarios[j+1] = temp_s
                }
            }
        }
        
        for (i = 0; i < n; i++) {
            printf "%d. %-16s: %8.1f ops/min\n", i+1, scenarios[i], throughputs[i]
        }
        
        print ""
        print "=== KEY INSIGHTS ==="
        
        # Calculate percentage differences
        if ("API_OO_TRUE" in avg_throughput && "API_OO_FALSE" in avg_throughput) {
            api_diff = ((avg_throughput["API_OO_TRUE"] - avg_throughput["API_OO_FALSE"]) / avg_throughput["API_OO_FALSE"]) * 100
            printf "API: isOO=true is %.1f%% faster than isOO=false\n", api_diff
        }
        
        if ("SQL_OO_TRUE" in avg_throughput && "SQL_OO_FALSE" in avg_throughput) {
            sql_diff = ((avg_throughput["SQL_OO_FALSE"] - avg_throughput["SQL_OO_TRUE"]) / avg_throughput["SQL_OO_TRUE"]) * 100
            printf "SQL: isOO=false is %.1f%% faster than isOO=true\n", sql_diff
        }
        
        if ("JDBC_LOAD_TEST" in avg_throughput && "SQL_OO_FALSE" in avg_throughput) {
            jdbc_vs_sql = ((avg_throughput["JDBC_LOAD_TEST"] - avg_throughput["SQL_OO_FALSE"]) / avg_throughput["SQL_OO_FALSE"]) * 100
            printf "JDBC vs SQL_OO_FALSE: %.1f%% difference\n", jdbc_vs_sql
        }
    }' "$OUTPUT_CSV"
    
    log "Complete comprehensive load test finished successfully!"
}

main "$@"