package com.tml.mapper.post;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tml.domain.entity.Post;
import com.tml.mapper.comment.CommentMapper;
import com.tml.mapper.common.CommonMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PostMapper extends CommonMapper<Post> {

}
