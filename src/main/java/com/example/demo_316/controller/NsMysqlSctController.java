package com.example.demo_316.controller;

import com.example.demo_316.service.NsMysqlSctService;
import com.example.demo_316.service.NsMysqlSctErrorService;
import com.example.demo_316.service.LoadTestService;
import com.example.demo_316.dto.NsMysqlSctDto;
import com.example.demo_316.dto.NsMysqlSctErrorDto;
import com.example.demo_316.dto.ResponseStatusDto;
import com.example.demo_316.dto.SqlCommandDto;
import com.example.demo_316.dto.LoadTestDto;
import com.example.demo_316.dto.LoadTestResultDto;
import com.example.demo_316.exception.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@RequestMapping(value = "/sct")
@RestController
public class NsMysqlSctController {
    @Autowired
    private NsMysqlSctService sctService;

    @Autowired
    private NsMysqlSctErrorService sctErrorService;

    @Autowired
    private LoadTestService loadTestService;

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
    public ResponseEntity<LoadTestResultDto> loadTest(@RequestBody LoadTestDto loadTestDto) throws CustomException {
        LoadTestResultDto result = loadTestService.executeLoadTest(loadTestDto);
        return ResponseEntity.ok(result);
    }
}
