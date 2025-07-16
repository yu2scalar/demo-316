package com.example.demo_316.controller;

import com.example.demo_316.service.NsMysqlSctErrorService;
import com.example.demo_316.dto.NsMysqlSctErrorDto;
import com.example.demo_316.dto.ResponseStatusDto;
import com.example.demo_316.dto.SqlCommandDto;
import com.example.demo_316.exception.CustomException;
import com.scalar.db.exception.transaction.CrudException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

@RequestMapping(value = "/sctError")
@RestController
public class NsMysqlSctErrorController {
    @Autowired
    private NsMysqlSctErrorService sctErrorService;

    @PostMapping
    public ResponseEntity<ResponseStatusDto> postNsMysqlSctError(@RequestBody NsMysqlSctErrorDto sctErrorDto) throws CustomException {
        return ResponseEntity.ok(sctErrorService.postNsMysqlSctError(sctErrorDto));
    }

    @PostMapping("/upsert")
    public ResponseEntity<ResponseStatusDto> upsertNsMysqlSctError(@RequestBody NsMysqlSctErrorDto sctErrorDto) throws CustomException {
        return ResponseEntity.ok(sctErrorService.upsertNsMysqlSctError(sctErrorDto));
    }

    @GetMapping("/{pk}/{ck}")
    public ResponseEntity<NsMysqlSctErrorDto> getNsMysqlSctError(@PathVariable("pk") Integer pk, @PathVariable("ck") Integer ck) throws CustomException {
        NsMysqlSctErrorDto sctErrorDto = NsMysqlSctErrorDto.builder()
            .pk(pk)
                .ck(ck)
            .build();
        return ResponseEntity.ok(sctErrorService.getNsMysqlSctError(sctErrorDto));
    }

    @PutMapping
    public ResponseEntity<ResponseStatusDto> putNsMysqlSctError(@RequestBody NsMysqlSctErrorDto sctErrorDto) throws CustomException {
        return ResponseEntity.ok(sctErrorService.putNsMysqlSctError(sctErrorDto));
    }

    @DeleteMapping("/{pk}/{ck}")
    public ResponseEntity<ResponseStatusDto> deleteNsMysqlSctError(@PathVariable("pk") Integer pk, @PathVariable("ck") Integer ck) throws CustomException {
        NsMysqlSctErrorDto sctErrorDto = NsMysqlSctErrorDto.builder()
            .pk(pk)
                .ck(ck)
            .build();
        return ResponseEntity.ok(sctErrorService.deleteNsMysqlSctError(sctErrorDto));
    }

    @GetMapping("scanByPk/{pk}")
    public ResponseEntity<List<NsMysqlSctErrorDto>> getNsMysqlSctErrorByPk(@PathVariable("pk") Integer pk) throws CustomException {
        NsMysqlSctErrorDto sctErrorDto = NsMysqlSctErrorDto.builder()
            .pk(pk)
            .build();
        return ResponseEntity.ok(sctErrorService.getNsMysqlSctErrorListByPk(sctErrorDto));
    }

    @GetMapping("/scanAll")
    public ResponseEntity<List<NsMysqlSctErrorDto>> getNsMysqlSctErrorListAll() throws CustomException {
        return ResponseEntity.ok(sctErrorService.getNsMysqlSctErrorListAll());
    }

    @PostMapping("/executeSQL")
    public ResponseEntity<List<NsMysqlSctErrorDto>> executeSQL(@RequestBody SqlCommandDto sqlCommandDto) throws CustomException {
        return ResponseEntity.ok(sctErrorService.executeSQL(sqlCommandDto));
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
}