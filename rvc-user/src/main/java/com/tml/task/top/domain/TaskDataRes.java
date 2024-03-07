package com.tml.task.top.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * @NAME: TaskDataRes
 * @USER: yuech
 * @Description:
 * @DATE: 2024/3/7
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskDataRes {
    private Map map;
    private List list;
}