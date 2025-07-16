package com.example.demo_316.mapper;

import com.example.demo_316.model.NsMysqlSct;
import com.example.demo_316.dto.NsMysqlSctDto;
import java.util.ArrayList;
import java.util.List;
import org.modelmapper.ModelMapper;

public class NsMysqlSctMapper {
    private static final ModelMapper modelMapper = new ModelMapper();

    // Convert Model to DTO
    public static NsMysqlSctDto mapToNsMysqlSctDto(NsMysqlSct sct) {
        return modelMapper.map(sct, NsMysqlSctDto.class);
    }

    // Convert DTO to Model
    public static NsMysqlSct mapToNsMysqlSct(NsMysqlSctDto sctDto) {
        return modelMapper.map(sctDto, NsMysqlSct.class);
    }

    // Convert Model List to DTO List
    public static List<NsMysqlSctDto> mapToNsMysqlSctDtoList(List<NsMysqlSct> sctList) {
        List<NsMysqlSctDto> sctDtoList = new ArrayList<>();
        for (NsMysqlSct sct : sctList) {
            sctDtoList.add(mapToNsMysqlSctDto(sct));
        }
        return sctDtoList;
    }
}