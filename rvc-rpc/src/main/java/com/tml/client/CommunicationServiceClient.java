package com.tml.client;

import com.tml.pojo.VO.TaskDataRes;
import io.github.common.web.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;


@FeignClient(name = "rvc-communication-service")
public interface CommunicationServiceClient {
    @GetMapping(value = "/communication/post/scoreList")
    public Result<TaskDataRes>  scoreList();
}
