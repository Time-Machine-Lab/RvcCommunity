package com.tml.feign.user;

import io.github.common.web.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(value = "tml-user-service",
        url = "1.94.28.8:9000",
        path = "/user")
//@FeignClient(name = "tml-user-service",
//        path = "/user")
public interface RvcUserServiceFeignClient {
    @GetMapping("/one")
    Result one(@RequestParam("uid") String uid);

    @PostMapping("/list")
    Result list(@RequestBody List<String> uidList);
}
