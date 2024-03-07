package com.tml.task;

import com.tml.task.top.TopTasks;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class ScheduledTasks {

    @Resource
    private TopTasks topTasks;

    // 每隔5秒执行一次
    @Scheduled(fixedRate = 5000)
    public void doTask() {
        // 执行需要定时执行的任务
        System.out.println("定时任务执行中...");
        startJob();
    }

    public void startJob() {
        topTasks.startJob();
    }
}
