package com.tml.mapper.post;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tml.domain.entity.Cover;
import com.tml.mapper.common.CommonMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CoverMapper  extends CommonMapper<Cover> {
}