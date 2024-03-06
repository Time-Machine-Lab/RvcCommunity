package com.tml.designpattern.chain.ext;

import com.tml.designpattern.chain.ApproveChain;
import com.tml.domain.dto.MqConsumerTaskDto;
import com.tml.domain.entity.Rule;
import com.tml.domain.entity.RvcLevelTask;
import org.springframework.context.annotation.Scope;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * @NAME: NumberApproveChain
 * @USER: yuech
 * @Description:
 * @DATE: 2024/2/26
 */
@Component
@Scope("prototype")
public class NumberApproveChain extends ApproveChain {

    @Resource
    private RedisTemplate redisTemplate;

    @Override
    public boolean approve() {
        if (numberCheck()){
            //下一个处理器
            return getNextChain().approve();
        }
        return false;
    }
    /**
     * 首先判断是否需要进行次数校验
     * 若不需要直接返回 真
     * 否则进行校验
     *
     * 校验方式：判断map中是否能得到 参数值
     */
    private boolean numberCheck(){
        MqConsumerTaskDto taskDto = getTaskDto();
        RvcLevelTask task = getTask();
        Rule rule = Rule.getRule(task);
        //不需要进行次数校验
        if (Objects.isNull(rule.getNumberRule())){
            return true;
        }

        ZSetOperations zSetOps = redisTemplate.opsForZSet();
        synchronized (this){
            //如果不存在先初始化
            Double score = zSetOps.score("rvc:grade:task:" +task.getId(), taskDto.getUserId());
            if (Objects.isNull(score)){
                zSetOps.add("rvc:grade:task:" + task.getId(), taskDto.getUserId(), 0.0);
                score = 0.0;
            }
            if (rule.getNumberRule() > score.intValue()){
                // 次数校验通过
                return true;
            }
        }
        return false;
    }

}