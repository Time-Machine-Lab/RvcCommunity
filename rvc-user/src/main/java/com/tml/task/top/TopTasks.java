package com.tml.task.top;

import com.tml.pojo.DO.UserData;
import com.tml.pojo.vo.UserInfoVO;
import com.tml.task.top.impl.ModelRankTask;
import com.tml.task.top.impl.PostRankTask;
import com.tml.task.top.impl.UserFansRankTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @NAME: TopTasks
 * @USER: yuech
 * @Description:
 * @DATE: 2024/3/7
 */
@Component
@Slf4j
public class TopTasks {

    @Resource
    private UserFansRankTask userFansRankTask;
    @Resource
    private PostRankTask postRankTask;
    @Resource
    private ModelRankTask modelRankTask;

    // 创建一个自定义的线程池，包含2个核心线程，最大4个线程，任务队列大小为2
    public static final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(4, 4, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(4));

    public void startJob(){
        threadPool.execute(userFansRankTask);
        threadPool.execute(postRankTask);
        threadPool.execute(modelRankTask);
    }

}