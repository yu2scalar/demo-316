package com.example.demo_316.controller;

import com.example.demo_316.service.GenericSqlService;
import com.example.demo_316.dto.ResponseStatusDto;
import com.example.demo_316.dto.SqlCommandDto;
import com.example.demo_316.exception.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.Map;

@RequestMapping(value = "/genericSql")
@RestController
public class GenericSqlController {
    @Autowired
    private GenericSqlService genericSqlService;

    @PostMapping("/execute")
    public ResponseEntity<List<Map<String, Object>>> executeSQLGeneric(@RequestBody SqlCommandDto sqlCommandDto) throws CustomException {
        return ResponseEntity.ok(genericSqlService.executeSQLGeneric(sqlCommandDto));
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
