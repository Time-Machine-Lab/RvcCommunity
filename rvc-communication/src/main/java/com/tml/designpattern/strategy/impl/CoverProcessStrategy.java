package com.tml.designpattern.strategy.impl;

import com.tml.designpattern.strategy.DetectionProcessStrategy;
import com.tml.mapper.CoverMapper;
import com.tml.pojo.dto.DetectionStatusDto;
import com.tml.pojo.entity.Cover;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class CoverProcessStrategy implements DetectionProcessStrategy {

     private final CoverMapper coverMapper;

    @Override
     public void process(DetectionStatusDto detectionStatusDto) {
        Cover cover = coverMapper.selectById(detectionStatusDto.getId());
        cover.setLabels(detectionStatusDto.getLabels());
        if (detectionStatusDto.getLabels().equals("nonLabel")){
            cover.setDetectionStatus(1);
        }else{
            cover.setDetectionStatus(2);
        }
        coverMapper.updateById(cover);

//        Cover cover = coverMapper.selectById(detectionStatusDto.getId());
//        cover.setViolationInformation(detectionStatusDto.getViolationInformation());
//        cover.setDetectionStatus(detectionStatusDto.getStatus());
//        coverMapper.updateById(cover);
    }
}