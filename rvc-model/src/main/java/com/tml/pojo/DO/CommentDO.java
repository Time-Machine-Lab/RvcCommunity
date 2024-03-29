package com.tml.pojo.DO;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.tml.pojo.VO.CommentFormVO;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Description
 * @Author welsir
 * @Date 2023/12/17 14:58
 */
@Data
@TableName("rvc_model_comment")
public class CommentDO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String content;
    private String uid;
    private String modelId;
    private String parentId;
    private Long likesNum;
    private String hasShow;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

}
