#!/bin/bash

# Load Test Comparison Script
# Runs 3 scenarios 10 times each and generates CSV with specific metrics

BASE_URL="http://localhost:8080/sct"
OUTPUT_CSV="load_test_results.csv"
ITERATIONS=10

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log() {
    echo -e "${GREEN}[$(date '+%Y-%m-%d %H:%M:%S')]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if server is running
check_server() {
    curl -s -f "$BASE_URL/scanAll" > /dev/null 2>&1
    return $?
}

# Function to execute load test and extract metrics
execute_load_test() {
    local endpoint="$1"
    local json_payload="$2"
    local scenario_name="$3"
    local iteration="$4"
    
    log "Running $scenario_name - Iteration $iteration"
    
    # Execute the load test
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
    
    # Extract and sum success counts
    local insert_success=$(echo "$response" | jq -r '.successCounts.insert // 0')
    local select_success=$(echo "$response" | jq -r '.successCounts.select // 0')
    local update_success=$(echo "$response" | jq -r '.successCounts.update // 0')
    local delete_success=$(echo "$response" | jq -r '.successCounts.delete // 0')
    local total_success=$((insert_success + select_success + update_success + delete_success))
    
    # Extract and sum error counts
    local insert_error=$(echo "$response" | jq -r '.errorCounts.insert // 0')
    local select_error=$(echo "$response" | jq -r '.errorCounts.select // 0')
    local update_error=$(echo "$response" | jq -r '.errorCounts.update // 0')
    local delete_error=$(echo "$response" | jq -r '.errorCounts.delete // 0')
    local total_error=$((insert_error + select_error + update_error + delete_error))
    
    # Check if parsing was successful
    if [ "$total_ops" = "ERROR" ]; then
        error "Failed to parse response for $scenario_name iteration $iteration"
        return 1
    fi
    
    # Write to CSV
    echo "$scenario_name,$iteration,$total_ops,$stats_ops,$total_time,$stats_time,$ramp_time,$throughput,$total_success,$total_error" >> "$OUTPUT_CSV"
    
    log "$scenario_name Iteration $iteration: $stats_ops ops, ${throughput} ops/min, Success: $total_success, Errors: $total_error"
    
    sleep 2
    return 0
}

main() {
    log "Starting Load Test Comparison (10 iterations each)"
    
    # Check dependencies
    if ! command -v jq &> /dev/null; then
        error "jq is required. Please install: sudo apt-get install jq"
        exit 1
    fi
    
    # Check server
    if ! check_server; then
        error "Server not running. Please start: ./gradlew bootRun"
        exit 1
    fi
    
    # Create CSV header
    echo "scenario,iteration,totalOperations,statisticsOperations,totalExecutionTimeMs,statisticsExecutionTimeMs,rampUpTimeMs,throughputPerMinute,totalSuccessCount,totalErrorCount" > "$OUTPUT_CSV"
    
    # Test scenarios
    local scenarios=(
        "/loadTest|API_OO_TRUE|{\"testDurationSeconds\":300,\"selectRatio\":1,\"updateRatio\":1,\"deleteRatio\":1,\"pk\":0,\"startCk\":0,\"threadCount\":10,\"exceptionRetryInterval\":0,\"rampUpTimeSeconds\":60,\"operationDelayMs\":0,\"cleanupAfterTest\":true,\"isOO\":true}"
        "/loadTest|API_OO_FALSE|{\"testDurationSeconds\":300,\"selectRatio\":1,\"updateRatio\":1,\"deleteRatio\":1,\"pk\":0,\"startCk\":0,\"threadCount\":10,\"exceptionRetryInterval\":0,\"rampUpTimeSeconds\":60,\"operationDelayMs\":0,\"cleanupAfterTest\":true,\"isOO\":false}"
        "/loadTest/sql|SQL_LOAD_TEST|{\"testDurationSeconds\":300,\"selectRatio\":1,\"updateRatio\":1,\"deleteRatio\":1,\"pk\":0,\"startCk\":0,\"threadCount\":10,\"exceptionRetryInterval\":0,\"rampUpTimeSeconds\":60,\"operationDelayMs\":0,\"cleanupAfterTest\":true,\"isOO\":true}"
    )
    
    local total_tests=$((ITERATIONS * 3))
    local current_test=0
    
    # Run each scenario
    for scenario_def in "${scenarios[@]}"; do
        IFS='|' read -r endpoint name payload <<< "$scenario_def"
        
        log "Starting $name - $ITERATIONS iterations"
        for i in $(seq 1 $ITERATIONS); do
            ((current_test++))
            log "Progress: $current_test/$total_tests"
            
            if ! execute_load_test "$endpoint" "$payload" "$name" "$i"; then
                error "Failed $name iteration $i"
            fi
        done
    done
    
    log "All tests completed! Results: $OUTPUT_CSV"
    
    # Simple summary
    if command -v awk &> /dev/null; then
        echo ""
        echo "=== SUMMARY (Average Results) ==="
        awk -F',' '
        NR>1 {
            scenario_ops[$1] += $4
            scenario_throughput[$1] += $8
            scenario_success[$1] += $9
            scenario_error[$1] += $10
            scenario_count[$1]++
        }
        END {
            printf "%-15s %10s %15s %10s %10s\n", "Scenario", "Avg Ops", "Avg Throughput", "Avg Success", "Avg Errors"
            for (s in scenario_count) {
                printf "%-15s %10.0f %15.2f %10.0f %10.0f\n", 
                    s, 
                    scenario_ops[s]/scenario_count[s], 
                    scenario_throughput[s]/scenario_count[s],
                    scenario_success[s]/scenario_count[s],
                    scenario_error[s]/scenario_count[s]
            }
        }' "$OUTPUT_CSV"
    fi
}

# Run main function
main "$@"