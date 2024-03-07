package com.tml.pojo.dto;

import lombok.Data;

@Data
public class DetectionStatusDto {
    //返回的唯一id
    private String id;
    //审核结果
    private String labels;


    //废弃字段
    private String name;
}