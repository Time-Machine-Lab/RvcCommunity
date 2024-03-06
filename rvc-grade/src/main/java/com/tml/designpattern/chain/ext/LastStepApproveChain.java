package com.tml.designpattern.chain.ext;


import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tml.designpattern.chain.ApproveChain;
import com.tml.domain.dto.MqConsumerTaskDto;
import com.tml.domain.entity.Rule;
import com.tml.domain.entity.RvcLevelTask;
import com.tml.domain.entity.RvcLevelUser;
import com.tml.mapper.RvcLevelUserMapper;
import org.springframework.context.annotation.Scope;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;


@Component
@Scope("prototype")
public class LastStepApproveChain extends ApproveChain {
    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private RvcLevelUserMapper rvcLevelUserMapper;
    /**
     * 在最后一个处理器进行结果处理
     * 需要处理的内容：1、如果通过次数校验 redis 中set得分增加
     *              2、数据库中用户经验增加
     * @return
     */
    @Override
    @Transactional
    public boolean approve() {
        String uid = getTaskDto().getUserId();
        Rule rule = Rule.getRule(getTask());
        ZSetOperations zSetOps = redisTemplate.opsForZSet();
        //redis中set得分增加
        if (!Objects.isNull(rule.getNumberRule())){
            zSetOps.add("rvc:grade:task:" + getTask().getId(), getTaskDto().getUserId(), 1.0);
        }
        // 数据库中用户经验值增加
        //        帖子的评论数+1
        LambdaUpdateWrapper<RvcLevelUser> updateWrapper = Wrappers.<RvcLevelUser>lambdaUpdate()
                .eq(RvcLevelUser::getUid, uid)
                .setSql("exp = exp + " + getTask().getExp());
        return rvcLevelUserMapper.update(null,updateWrapper) > 0;
    }
}