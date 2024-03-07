package com.tml.task.top.impl;

import com.tml.task.top.TopTask;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @NAME: ModelRankTask
 * @USER: yuech
 * @Description:
 * @DATE: 2024/2/29
 */
@Slf4j
@AllArgsConstructor
@Component
public class ModelRankTask extends TopTask implements Runnable{

    @Override
    public void run() {
        //todo sql优化
        log.info("正在执行模型排行榜初始化的操作");
        //调用feign 获取前1000 的数据 和分数


    }
}