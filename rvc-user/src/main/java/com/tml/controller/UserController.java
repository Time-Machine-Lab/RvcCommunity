package com.tml.controller;

import com.tml.annotation.apiAuth.InternalApi;
import com.tml.annotation.apiAuth.LaxTokenApi;
import com.tml.annotation.apiAuth.WhiteApi;
import com.tml.common.annotation.ListNotEmpty;
import com.tml.exception.RvcSQLException;
import com.tml.exception.ServerException;
import com.tml.pojo.dto.*;
import com.tml.pojo.enums.ResultEnums;
import com.tml.service.UserService;
import io.github.common.web.Result;
import org.hibernate.validator.constraints.Length;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @Date 2023/12/10
 * @Author xiaochun
 */
@RestController
@Validated
@RequestMapping("/user")
public class UserController {
    @Resource
    UserService userService;

    @Resource
    RabbitTemplate rabbitTemplate;


    /**
     * @param loginDTO LoginDTO
     * @return {@link Result}
     */
    @PostMapping("/login")
    public Result login(@RequestBody
                            @Valid
                            LoginDTO loginDTO){
        return Result.success(userService.login(loginDTO));
    }

    @PostMapping("/logout")
    @WhiteApi
    public Result logout(@RequestHeader String uid,@RequestHeader String username){
        userService.logout(uid, username);
        return Result.success();
    }

    /**
     * @param registerDTO RegisterDTO
     * @return {@link Result}
     */
    @PostMapping("/register")
    public Result register(@RequestBody
                               @Valid
                               RegisterDTO registerDTO) throws RvcSQLException {
        return Result.success(userService.register(registerDTO));
    }

    @GetMapping("/preCode")
    public Result preCode(){
        return Result.success(userService.preCode());
    }

    /**
     * @param email String
     * @param type boolean
     * @return {@link Result}
     */
    @GetMapping("/email")
    public Result email(@RequestParam
                            @Valid
                            @NotNull(message = "邮箱不能为空")
                            @Length(min = 6, max = 30, message = "邮箱长度必须在6到30之间")
                            @Email( message = "参数必须为邮箱")
                            String email,
                        @RequestParam
                            @Valid
                            @Length(min = 4, max = 10, message = "验证码长度必须在4到6之间")
                            @NotBlank(message = "前置验证码不为空")
                            String code,
                        @RequestParam
                            @Valid
                            @NotBlank(message = "前置验证码标识不能为空")
                            String uuid,
                        @RequestParam
                            @NotNull(message = "验证码类型不能为空")
                            int type){
        userService.sendCode(email, code, uuid, type);
        return Result.success();
    }

    @GetMapping("/resetPwdEmailCode")
    @WhiteApi
    public Result resetPwdEmailCode(@RequestHeader String uid,
                                    @RequestParam
                                        @Valid
                                        @NotNull(message = "邮箱不能为空")
                                        @Length(min = 6, max = 30, message = "邮箱长度必须在6到30之间")
                                        @Email( message = "参数必须为邮箱")
                                        String email,
                                    @RequestParam
                                        @Valid
                                        @Length(min = 4, max = 10, message = "验证码长度必须在4到6之间")
                                        @NotBlank(message = "前置验证码不为空")
                                        String code,
                                    @RequestParam
                                        @Valid
                                        @NotBlank(message = "前置验证码标识不能为空")
                                        String uuid){
        userService.resetPwdEmailCode(email, code, uuid, uid);
        return Result.success();
    }

    /**
     * @param uid String
     * @return {@link Result}
     */
    @GetMapping("/one")
    @InternalApi
    public Result one(@RequestParam String uid){
        return Result.success(userService.one(uid));
    }

    /**
     * @param uidList List<String>
     * @return {@link Result}
     */
    @PostMapping("/list")
    @InternalApi
    public Result list(@RequestBody
                           @ListNotEmpty
                           List<String> uidList){
        return Result.success(userService.list(uidList));
    }

    /**
     * @param userInfoDTO UserInfoDTO
     * @return {@link Result} 待完成
     */
    @PostMapping("/update")
    @WhiteApi
    public Result update(@RequestBody
                             @Valid
                             UserInfoDTO userInfoDTO,
                         @RequestHeader String uid){
        userService.update(userInfoDTO, uid);
        return Result.success();
    }

    /**
     * @param uid String
     * @return {@link Result}
     */
    @PostMapping("/follow")
    @WhiteApi
    public Result follow(@RequestPart
                             @Valid
                             @NotBlank(message = "uid不能为空")
                             @Length(min = 19, max = 19, message = "uid长度为19")
                             String followUid,
                         @RequestHeader String uid) throws RvcSQLException {
        userService.follow(followUid, uid);
        return Result.success();
    }

    @PostMapping("/updatePassword")
    @WhiteApi
    public Result updatePassword(@RequestBody
                                     @Valid
                                     UpdatePasswordDTO updatePasswordDTO,
                                 @RequestHeader String uid){
        if(userService.updatePassword(updatePasswordDTO, uid))
            throw new ServerException(ResultEnums.FAIL_RESET);
        return Result.success();
    }

    @PostMapping("/forgotPassword")
    @LaxTokenApi
    public Result forgotPassword(@RequestBody
                                 @Valid
                                     ForgotPassword forgotPassword){
        userService.forgotPassword(forgotPassword);
        return Result.success();
    }

    @GetMapping("/getUserInfo")
    @WhiteApi
    public Result getUserInfo(@RequestHeader String uid){
        return Result.success(userService.getUserInfo(uid));
    }

    @GetMapping("/getUserInfoById")
    @LaxTokenApi
    public Result getUserInfoById(@RequestParam
                                      @Valid
                                      @NotBlank
                                      String targetUid,
                                  @RequestHeader(required = false) String uid){
        return Result.success(userService.getUserInfoById(targetUid, uid));
    }

    @PostMapping("/avatar")
    @WhiteApi
    public Result avatar(@RequestPart("file") MultipartFile file,
                         @RequestHeader String uid){
        userService.avatar(file, uid);
        return Result.success("审核中");
    }


    @GetMapping("/exist")
    @InternalApi
    public Result exist(@RequestParam String uid){
        return Result.success(userService.exist(uid));
    }

    @GetMapping("/getMyFollowUser")
    @WhiteApi
    public Result getMyFollowUser(@RequestHeader String uid){
        return Result.success(userService.getMyFollowUser(uid));
    }

    @GetMapping("/isFollowed")
    @InternalApi
    public Result isFollowed(@RequestParam String uid1, @RequestParam String uid2){
        return Result.success(userService.isFollowed(uid1, uid2));
    }
}