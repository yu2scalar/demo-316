#!/bin/bash

# Comprehensive Load Test - All 5 Scenarios
# API isOO=true, API isOO=false, SQL isOO=true, SQL isOO=false, JDBC
BASE_URL="http://localhost:8080/sct"
OUTPUT_CSV="comprehensive_load_test_results.csv"
ITERATIONS=10

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
    
    # Write to CSV
    echo "$scenario_name,$iteration,$total_ops,$stats_ops,$total_time,$stats_time,$ramp_time,$throughput,$total_success,$total_error" >> "$OUTPUT_CSV"
    
    log "$scenario_name Iteration $iteration: $stats_ops ops, ${throughput} ops/min, Success: $total_success, Errors: $total_error"
    
    sleep 3
    return 0
}

main() {
    log "Starting Comprehensive Load Test - All 5 Scenarios"
    info "Test Parameters: 60s test duration, 30s ramp-up, 10 threads, $ITERATIONS iterations each"
    
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
    
    # Create new CSV
    echo "scenario,iteration,totalOperations,statisticsOperations,totalExecutionTimeMs,statisticsExecutionTimeMs,rampUpTimeMs,throughputPerMinute,totalSuccessCount,totalErrorCount" > "$OUTPUT_CSV"
    
    # Test scenarios - All 5 patterns
    local scenarios=(
        "/loadTest|API_OO_TRUE|{\"testDurationSeconds\":60,\"selectRatio\":1,\"updateRatio\":1,\"deleteRatio\":1,\"pk\":1001,\"startCk\":0,\"threadCount\":10,\"exceptionRetryInterval\":0,\"rampUpTimeSeconds\":30,\"operationDelayMs\":0,\"cleanupAfterTest\":true,\"isOO\":true}"
        "/loadTest|API_OO_FALSE|{\"testDurationSeconds\":60,\"selectRatio\":1,\"updateRatio\":1,\"deleteRatio\":1,\"pk\":1002,\"startCk\":0,\"threadCount\":10,\"exceptionRetryInterval\":0,\"rampUpTimeSeconds\":30,\"operationDelayMs\":0,\"cleanupAfterTest\":true,\"isOO\":false}"
        "/loadTest/sql|SQL_OO_TRUE|{\"testDurationSeconds\":60,\"selectRatio\":1,\"updateRatio\":1,\"deleteRatio\":1,\"pk\":1003,\"startCk\":0,\"threadCount\":10,\"exceptionRetryInterval\":0,\"rampUpTimeSeconds\":30,\"operationDelayMs\":0,\"cleanupAfterTest\":true,\"isOO\":true}"
        "/loadTest/sql|SQL_OO_FALSE|{\"testDurationSeconds\":60,\"selectRatio\":1,\"updateRatio\":1,\"deleteRatio\":1,\"pk\":1004,\"startCk\":0,\"threadCount\":10,\"exceptionRetryInterval\":0,\"rampUpTimeSeconds\":30,\"operationDelayMs\":0,\"cleanupAfterTest\":true,\"isOO\":false}"
        "/loadTest/jdbc|JDBC_LOAD_TEST|{\"testDurationSeconds\":60,\"selectRatio\":1,\"updateRatio\":1,\"deleteRatio\":1,\"pk\":1005,\"startCk\":0,\"threadCount\":10,\"exceptionRetryInterval\":0,\"rampUpTimeSeconds\":30,\"operationDelayMs\":0,\"cleanupAfterTest\":true,\"isOO\":false}"
    )
    
    local total_tests=$((ITERATIONS * 5))
    local current_test=0
    local start_time=$(date +%s)
    
    # Run each scenario
    for scenario_def in "${scenarios[@]}"; do
        IFS='|' read -r endpoint name payload <<< "$scenario_def"
        
        info "=== Starting $name - $ITERATIONS iterations ==="
        for i in $(seq 1 $ITERATIONS); do
            ((current_test++))
            log "Progress: $current_test/$total_tests"
            
            if ! execute_load_test "$endpoint" "$payload" "$name" "$i"; then
                error "Failed $name iteration $i"
            fi
        done
        info "=== Completed $name ==="
        echo ""
    done
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    log "All tests completed! Duration: ${duration}s"
    log "Results saved to: $OUTPUT_CSV"
    
    # Generate comprehensive summary
    echo ""
    echo "=== COMPREHENSIVE LOAD TEST RESULTS ==="
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
        print "=== PERFORMANCE ANALYSIS ==="
        
        # API Comparison
        if ("API_OO_TRUE" in avg_throughput && "API_OO_FALSE" in avg_throughput) {
            api_improvement = ((avg_throughput["API_OO_TRUE"] - avg_throughput["API_OO_FALSE"]) / avg_throughput["API_OO_FALSE"]) * 100
            printf "API: isOO=true vs isOO=false: %.1f%% performance difference\n", api_improvement
        }
        
        # SQL Comparison
        if ("SQL_OO_TRUE" in avg_throughput && "SQL_OO_FALSE" in avg_throughput) {
            sql_improvement = ((avg_throughput["SQL_OO_TRUE"] - avg_throughput["SQL_OO_FALSE"]) / avg_throughput["SQL_OO_FALSE"]) * 100
            printf "SQL: isOO=true vs isOO=false: %.1f%% performance difference\n", sql_improvement
        }
        
        # API vs SQL Comparisons
        if ("API_OO_TRUE" in avg_throughput && "SQL_OO_TRUE" in avg_throughput) {
            api_vs_sql_oo = ((avg_throughput["API_OO_TRUE"] - avg_throughput["SQL_OO_TRUE"]) / avg_throughput["SQL_OO_TRUE"]) * 100
            printf "API_OO_TRUE vs SQL_OO_TRUE: %.1f%% difference\n", api_vs_sql_oo
        }
        
        if ("API_OO_FALSE" in avg_throughput && "SQL_OO_FALSE" in avg_throughput) {
            api_vs_sql_nonoo = ((avg_throughput["API_OO_FALSE"] - avg_throughput["SQL_OO_FALSE"]) / avg_throughput["SQL_OO_FALSE"]) * 100
            printf "API_OO_FALSE vs SQL_OO_FALSE: %.1f%% difference\n", api_vs_sql_nonoo
        }
        
        # JDBC Comparisons
        if ("JDBC_LOAD_TEST" in avg_throughput && "API_OO_FALSE" in avg_throughput) {
            jdbc_vs_api = ((avg_throughput["JDBC_LOAD_TEST"] - avg_throughput["API_OO_FALSE"]) / avg_throughput["API_OO_FALSE"]) * 100
            printf "JDBC vs API_OO_FALSE: %.1f%% difference\n", jdbc_vs_api
        }
        
        if ("JDBC_LOAD_TEST" in avg_throughput && "SQL_OO_FALSE" in avg_throughput) {
            jdbc_vs_sql = ((avg_throughput["JDBC_LOAD_TEST"] - avg_throughput["SQL_OO_FALSE"]) / avg_throughput["SQL_OO_FALSE"]) * 100
            printf "JDBC vs SQL_OO_FALSE: %.1f%% difference\n", jdbc_vs_sql
        }
        
        print ""
        print "=== TOP PERFORMERS ==="
        # Find best performer
        best_throughput = 0
        best_scenario = ""
        for (s in avg_throughput) {
            if (avg_throughput[s] > best_throughput) {
                best_throughput = avg_throughput[s]
                best_scenario = s
            }
        }
        printf "Best Performance: %s (%.2f ops/min)\n", best_scenario, best_throughput
        
        # Find most reliable (lowest error rate)
        lowest_error_rate = 100
        most_reliable = ""
        for (s in scenario_count) {
            total_ops_scenario = scenario_ops[s]/scenario_count[s]
            total_errors_scenario = scenario_error[s]/scenario_count[s]
            error_rate = (total_errors_scenario / (total_ops_scenario + total_errors_scenario)) * 100
            if (error_rate < lowest_error_rate) {
                lowest_error_rate = error_rate
                most_reliable = s
            }
        }
        printf "Most Reliable: %s (%.2f%% error rate)\n", most_reliable, lowest_error_rate
    }' "$OUTPUT_CSV"
    
    log "Comprehensive load test completed successfully!"
    info "Results file: $OUTPUT_CSV"
}

main "$@"