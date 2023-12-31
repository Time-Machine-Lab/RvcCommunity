package com.tml.core.client;

import com.tml.common.Result;
import com.tml.constant.RemoteModuleURL;
import com.tml.pojo.DTO.UserInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


/**
 * @Description
 * @Author welsir
 * @Date 2023/12/13 23:24
 */
@FeignClient(name = "tml-user-service")
public interface UserServiceClient {

    @GetMapping(value = RemoteModuleURL.GET_USERINFO)
    Result<UserInfoDTO> getUserInfo(@RequestParam("uid") String uid);

}
