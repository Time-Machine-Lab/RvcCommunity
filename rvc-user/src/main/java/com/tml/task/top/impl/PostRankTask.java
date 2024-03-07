package com.tml.task.top.impl;

import com.tml.client.CommunicationServiceClient;
import com.tml.pojo.VO.TaskDataRes;
import com.tml.task.top.TopTask;
import com.tml.task.top.domain.Post;
import com.tml.task.top.domain.ScoreListVo;
import com.tml.util.RedisCache;
import io.github.common.web.Result;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @NAME: PostRankTask
 * @USER: yuech
 * @Description:
 * @DATE: 2024/2/29
 */
@Slf4j
@AllArgsConstructor
@Component
public class PostRankTask extends TopTask implements Runnable{

    /**
     * * 贴子排行榜：分数=浏览数 * 50% + 点赞数 * 50% + 收藏数 50% +  评论数 * 50%
     */
    @Resource
    private CommunicationServiceClient communicationServiceClient;


    @Resource
    private RedisCache redisCache;

    @Resource
    private RedisTemplate redisTemplate;




    @Override
    public void run() {
        //todo sql优化
        log.info("正在执行用户排行榜初始化的操作");
        //调用feign 获取前1000 的数据 和分数
        TaskDataRes data = null;
        data = communicationServiceClient.scoreList().getData();

        Map<String, Post> map = data.getMap();

        //存 map
        //存 map  和zset
        redisCache.setCacheMap("rvc:top:post:1000",map);


        List top1000Users = data.getList();
        List<LinkedHashMap<String, Double>> rawData = top1000Users;

        System.out.println(top1000Users);

//        //存zset
        ZSetOperations zSetOps = redisTemplate.opsForZSet();
        rawData.stream()
                .forEach(o ->{
                    zSetOps.add("rvc:top:post:1000:score",o.get("id"),o.get("score"));
                });
    }
}