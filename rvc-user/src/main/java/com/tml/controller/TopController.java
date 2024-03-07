package com.tml.controller;

import com.tml.annotation.apiAuth.LaxTokenApi;
import com.tml.annotation.apiAuth.WhiteApi;
import io.github.common.web.Result;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * @Date 2024/2/28
 * @Author xiaochun
 */
@RestController
@Validated
@RequestMapping("/top")
public class TopController {
    @GetMapping("/user")
    @LaxTokenApi
    public Result userTop(){
        return Result.success();
    }
}
