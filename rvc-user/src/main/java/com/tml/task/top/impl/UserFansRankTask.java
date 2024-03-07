package com.tml.task.top.impl;

import com.tml.mapper.UserDataMapper;
import com.tml.pojo.DO.UserData;
import com.tml.pojo.vo.UserInfoVO;
import com.tml.service.UserService;
import com.tml.task.top.TopTask;
import com.tml.task.top.domain.TaskDataRes;
import com.tml.util.RedisCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @NAME: UserFansRankTask
 * @USER: yuech
 * @Description:
 * @DATE: 2024/2/29
 */
@Component
@Slf4j
public class UserFansRankTask extends TopTask implements Runnable{
    /**
     *      * 用户粉丝排行榜：分数=粉丝数
     */
    @Autowired
    private UserDataMapper userDataMapper;

    @Resource
    private UserService userService;

    @Override
    public void run() {
        //todo sql优化
        log.info("正在执行用户排行榜初始化的操作");
        TaskDataRes taskDataRes = mygetData();
        setData(taskDataRes);
    }

    private TaskDataRes mygetData() {
        //获取粉丝量前1000的用户
        List<UserData> top1000Users = userDataMapper.selectTop1000UsersByFans();

        //获取前100 user 的id
        List<String> top1000UsersUid = top1000Users.stream()
                .map(o -> o.getUid())
                .collect(Collectors.toList());

        Map<String, UserInfoVO> map = userService.list(top1000UsersUid);
        return new TaskDataRes(map,top1000Users);
    }
}