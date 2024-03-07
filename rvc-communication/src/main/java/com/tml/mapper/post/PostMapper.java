package com.tml.mapper.post;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tml.domain.entity.Post;
import com.tml.mapper.comment.CommentMapper;
import com.tml.mapper.common.CommonMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PostMapper extends CommonMapper<Post> {

    List<Post> getScoreList();
}
