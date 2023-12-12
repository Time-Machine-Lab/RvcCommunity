package com.tml.core.async;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.tml.common.log.AbstractLogger;
import com.tml.core.rabbitmq.ModelListener;
import com.tml.mapper.ModelMapper;
import com.tml.pojo.DO.ModelDO;
import com.tml.pojo.DTO.DetectionTaskDTO;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Description
 * @Author welsir
 * @Date 2023/12/7 22:13
 */
@Service
public class AsyncService {

    @Resource
    AbstractLogger logger;
    @Resource
    ModelMapper mapper;
    @Resource
    ModelListener listener;
    @Async
    public void processModelAsync(ModelDO modelDO) throws InterruptedException {
        try {
            String audioId = String.valueOf(modelDO.getId());
            String name = "model";
            DetectionTaskDTO build = DetectionTaskDTO.builder()
                    .id(audioId)
                    .content(modelDO.getDescription())
                    .name(name)
                    .build();
            // 调用审核模块接口
            //todo:下一版本优化调用方式，或许可以封装一个抽象框架去将所有需要审核的信息存放并且构建事务方法保证统一性
            listener.sendMsgToMQ(build,"text");
            build.setContent(modelDO.getName());
            listener.sendMsgToMQ(build,"text");
            build.setContent(modelDO.getPicture());
            listener.sendMsgToMQ(build,"image");
            logger.info("异步审核完毕");
        } catch (Exception e) {
            // 异常处理
            logger.error("调用审核服务失败%s:%s ", e.getMessage(), e);
        }

    }

    @Async
    public void asyncAddModelViewNums(String modelId){
        List<ModelDO> list = mapper.selectList(new QueryWrapper<ModelDO>().eq("id", modelId));
        ModelDO modelDO = list.get(0);
        mapper.update(modelDO,new UpdateWrapper<ModelDO>().set("view_num",modelDO.getViewNum()+1));
    }

}