package com.tml.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tml.mapper.FeedbackMapper;
import com.tml.pojo.FeedbackDO;
import com.tml.pojo.vo.FeedbackVO;
import com.tml.service.DetectionService;
import com.tml.service.IFeedbackDaoService;
import io.github.query.QueryParamGroup;
import io.github.service.AssistantMJPServiceImpl;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Service
public class IFeedbackDaoServiceImpl extends AssistantMJPServiceImpl<FeedbackMapper, FeedbackDO> implements IFeedbackDaoService, DetectionService {

    @Resource
    QueryParamGroup queryParamGroup;

    @Override
    public IPage<FeedbackVO> feedbackPageVO(int page, int limit, String queryType, String order) {
        return this.BeanPageVOList(page,limit,
                queryParamGroup.getQueryParams(queryType),
                List.of("fb_id",order),true,
                FeedbackVO.class);
    }

    @Override
    public Boolean feedbackAdd(FeedbackDO feedbackDO) {
        return baseMapper.insert(feedbackDO)>0;
    }

    @Override
    public Boolean feedbackUpdate(String uid, Long fb_id, FeedbackDO feedback) {
        return this.updateBean(feedback,Map.of("fb_id",fb_id,"uid",uid));
    }

    @Override
    public Boolean feedbackDelete(String uid, Long fb_id) {
        return this.deleteBean(Map.of("fb_id",fb_id,"uid",uid));
    }

    @Override
    public Boolean changeDetection(Long fb_id, Integer status) {
        FeedbackDO build = FeedbackDO.builder()
                .hasShow(status)
                .build();
        return this.updateBean(build,Map.of("fb_id",fb_id));
    }
}