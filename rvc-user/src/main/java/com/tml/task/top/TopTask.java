package com.tml.task.top;

import com.tml.pojo.DO.UserData;
import com.tml.pojo.vo.UserInfoVO;
import com.tml.task.top.domain.TaskDataRes;
import com.tml.util.RedisCache;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * @NAME: TopTask
 * @USER: yuech
 * @Description:
 * @DATE: 2024/3/7
 */
@Component
public abstract class TopTask {

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private RedisCache redisCache;

     public TaskDataRes getData(){
         return null;
     }

    public void setData(TaskDataRes taskDataRes){
        Map<String, UserInfoVO> list = taskDataRes.getMap();
        List<UserData> top1000Users = taskDataRes.getList();
         //存 map
        //存 map  和zset
        redisCache.setCacheMap("rvc:top:user:1000",list);

        //存zset
        ZSetOperations zSetOps = redisTemplate.opsForZSet();
        top1000Users.stream()
                .forEach(o ->{
                    zSetOps.add("rvc:top:user:1000:score",o.getUid(),o.getFansNum());
                });
    }
}