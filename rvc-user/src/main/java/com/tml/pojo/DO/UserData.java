package com.tml.pojo.DO;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tml.pojo.vo.UserInfoVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Date 2023/12/8
 * @Author xiaochun
 */

@TableName("rvc_user_data")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserData {
    @TableId("uid")
    private String uid;

    private int followNum;

    private int postNum;

    private int fansNum;

    private int modelNum;

    @TableField(exist = false)
    private double totalScore;



    

    public static UserInfoVO toVO(UserInfoVO userInfoVO, UserData userData){
        userInfoVO.setFollowNum(userData.getFollowNum());
        userInfoVO.setFansNum(userData.getFansNum());
        return userInfoVO;
    }
}
