package com.example.demo_316.mapper;

import com.example.demo_316.model.NsMysqlSctError;
import com.example.demo_316.dto.NsMysqlSctErrorDto;
import java.util.ArrayList;
import java.util.List;
import org.modelmapper.ModelMapper;

public class NsMysqlSctErrorMapper {
    private static final ModelMapper modelMapper = new ModelMapper();

    // Convert Model to DTO
    public static NsMysqlSctErrorDto mapToNsMysqlSctErrorDto(NsMysqlSctError sctError) {
        return modelMapper.map(sctError, NsMysqlSctErrorDto.class);
    }

    // Convert DTO to Model
    public static NsMysqlSctError mapToNsMysqlSctError(NsMysqlSctErrorDto sctErrorDto) {
        return modelMapper.map(sctErrorDto, NsMysqlSctError.class);
    }

    // Convert Model List to DTO List
    public static List<NsMysqlSctErrorDto> mapToNsMysqlSctErrorDtoList(List<NsMysqlSctError> sctErrorList) {
        List<NsMysqlSctErrorDto> sctErrorDtoList = new ArrayList<>();
        for (NsMysqlSctError sctError : sctErrorList) {
            sctErrorDtoList.add(mapToNsMysqlSctErrorDto(sctError));
        }
        return sctErrorDtoList;
    }
}