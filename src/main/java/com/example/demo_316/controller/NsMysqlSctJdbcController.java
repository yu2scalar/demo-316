package com.example.demo_316.controller;

import com.example.demo_316.dto.LoadTestDto;
import com.example.demo_316.dto.LoadTestResultDto;
import com.example.demo_316.dto.NsMysqlSctDto;
import com.example.demo_316.dto.ResponseStatusDto;
import com.example.demo_316.exception.CustomException;
import com.example.demo_316.service.JdbcLoadTestService;
import com.example.demo_316.service.NsMysqlSctJdbcService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/jdbc/sct")
@RequiredArgsConstructor
public class NsMysqlSctJdbcController {
    
    private final NsMysqlSctJdbcService jdbcSctService;
    private final JdbcLoadTestService jdbcLoadTestService;
    
    @PostMapping
    public ResponseEntity<ResponseStatusDto> postNsMysqlSct(@RequestBody @Valid NsMysqlSctDto request) throws CustomException {
        jdbcSctService.insertNsMysqlSct(request);
        return ResponseEntity.ok(
            ResponseStatusDto.builder()
                .code(200)
                .message("Success")
                .build()
        );
    }
    
    @GetMapping("/{pk}/{ck}")
    public ResponseEntity<NsMysqlSctDto> getNsMysqlSct(@PathVariable Integer pk, @PathVariable Integer ck) throws CustomException {
        NsMysqlSctDto request = NsMysqlSctDto.builder().pk(pk).ck(ck).build();
        return ResponseEntity.ok(jdbcSctService.selectNsMysqlSct(request));
    }
    
    @GetMapping("scanByPk/{pk}")
    public ResponseEntity<List<NsMysqlSctDto>> getNsMysqlSctByPk(@PathVariable Integer pk) throws CustomException {
        NsMysqlSctDto request = NsMysqlSctDto.builder().pk(pk).build();
        return ResponseEntity.ok(jdbcSctService.selectNsMysqlSctListByPk(request));
    }
    
    @PutMapping
    public ResponseEntity<ResponseStatusDto> putNsMysqlSct(@RequestBody @Valid NsMysqlSctDto request) throws CustomException {
        jdbcSctService.updateNsMysqlSct(request);
        return ResponseEntity.ok(
            ResponseStatusDto.builder()
                .code(200)
                .message("Success")
                .build()
        );
    }
    
    @DeleteMapping("/{pk}/{ck}")
    public ResponseEntity<ResponseStatusDto> deleteNsMysqlSct(@PathVariable Integer pk, @PathVariable Integer ck) throws CustomException {
        NsMysqlSctDto request = NsMysqlSctDto.builder().pk(pk).ck(ck).build();
        jdbcSctService.deleteNsMysqlSct(request);
        return ResponseEntity.ok(
            ResponseStatusDto.builder()
                .code(200)
                .message("Success")
                .build()
        );
    }
    
    @PostMapping("/loadtest")
    public ResponseEntity<LoadTestResultDto> executeJdbcLoadTest(@RequestBody @Valid LoadTestDto request) throws CustomException {
        log.info("Starting JDBC load test with parameters: {}", request);
        LoadTestResultDto result = jdbcLoadTestService.executeJdbcLoadTest(request);
        log.info("JDBC load test completed successfully");
        return ResponseEntity.ok(result);
    }
    
    @ExceptionHandler(value = CustomException.class)
    public ResponseEntity<ResponseStatusDto> handleScalarDbException(CustomException ex) {
        return switch (ex.getErrorCode()) {
            case 9100 -> new ResponseEntity<>(ResponseStatusDto.builder().code(ex.getErrorCode()).message(ex.getMessage()).build(), HttpStatus.BAD_REQUEST);
            case 9200, 9300 -> new ResponseEntity<>(ResponseStatusDto.builder().code(ex.getErrorCode()).message(ex.getMessage()).build(), HttpStatus.INTERNAL_SERVER_ERROR);
            case 9400, 9404 -> new ResponseEntity<>(ResponseStatusDto.builder().code(ex.getErrorCode()).message(ex.getMessage()).build(), HttpStatus.NOT_FOUND);
            case 9409 -> new ResponseEntity<>(ResponseStatusDto.builder().code(ex.getErrorCode()).message(ex.getMessage()).build(), HttpStatus.CONFLICT);
            case 9500, 9503 -> new ResponseEntity<>(ResponseStatusDto.builder().code(ex.getErrorCode()).message(ex.getMessage()).build(), HttpStatus.INTERNAL_SERVER_ERROR);
            default -> new ResponseEntity<>(ResponseStatusDto.builder().code(ex.getErrorCode()).message(ex.getMessage()).build(), HttpStatus.INTERNAL_SERVER_ERROR);
        };
    }
}