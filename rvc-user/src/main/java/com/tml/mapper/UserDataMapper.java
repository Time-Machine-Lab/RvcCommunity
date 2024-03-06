package com.tml.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tml.pojo.DO.UserData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @Date 2023/12/8
 * @Author xiaochun
 */
@Mapper
public interface UserDataMapper extends BaseMapper<UserData> {
    @Select("SELECT * FROM rvc_user_data WHERE uid = #{uid}")
    UserData selectByUid(@Param("uid") String uid);

    List<UserData> selectTop1000UsersByFans();

    List<UserData> selectTop10000UsersTop();
}