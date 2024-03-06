package com.tml.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tml.client.FileServiceClient;
import com.tml.common.rabbitmq.RabbitMQListener;
import com.tml.config.FileConfig;
import com.tml.exception.RvcSQLException;
import com.tml.exception.ServerException;
import com.tml.mapper.UserDataMapper;
import com.tml.mapper.UserFollowMapper;
import com.tml.mapper.UserInfoMapper;
import com.tml.pojo.DO.*;
import com.tml.pojo.DTO.ReceiveUploadFileDTO;
import com.tml.pojo.Result;
import com.tml.pojo.VO.UploadModelForm;
import com.tml.pojo.dto.*;
import com.tml.pojo.enums.ResultEnums;
import com.tml.pojo.vo.UserInfoVO;
import com.tml.service.UserService;
import com.tml.util.*;
import org.apache.catalina.User;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tml.config.DetectionConfig.*;
import static com.tml.config.FileConfig.MAX_FILE_SIZE;
import static com.tml.config.UserConfig.DEFAULT_AVATAR;
import static com.tml.pojo.enums.EmailEnums.*;

/**
 * @Date 2023/12/10
 * @Author xiaochun
 */
@Service
public class UserServiceImpl implements UserService {
    @Resource
    UserInfoMapper userInfoMapper;

    @Resource
    FileServiceClient fileServiceClient;

    @Resource
    UserFollowMapper userFollowMapper;

    @Resource
    UserDataMapper userDataMapper;

    @Resource
    RabbitMQListener rabbitMQListener;

    @Resource
    CodeUtil codeUtil;

    @Override
    public Map<String, String> login(LoginDTO loginDTO) {
        String emailCode = loginDTO.getEmailCode();
        String password = loginDTO.getPassword();
        if(emailCode != null && password != null){
            throw new ServerException(ResultEnums.LOGIN_FORM_ERROR);
        }
        String email = loginDTO.getEmail();
        String token;
        if(emailCode == null){
            token = loginByPsw(email, password);
        } else{
            token = loginByCode(email, emailCode);
        }
        if (token == null) {
            throw new ServerException(ResultEnums.FAIL_LOGIN);
        }
        return Map.of("token", token);
    }

    @Override
    public void logout(String uid, String username) {
        TokenUtil.logout(uid, username);
    }

    @Override
    @Transactional(rollbackFor = RvcSQLException.class)
    public Map<String, String> register(RegisterDTO registerDTO) throws RvcSQLException {
        String email = registerDTO.getEmail();
        if(!codeUtil.emailVerify(email, REGISTER, registerDTO.getEmailCode())) {
            throw new ServerException(ResultEnums.VER_CODE_ERROR);
        }
        UserInfo userInfo = new UserInfo();
        UserData userData = new UserData("0",0,0,0,0,0);
        userInfo.setEmail(email);
        userInfo.setPassword(DigestUtils.md5Hex(registerDTO.getPassword()));
        userInfo.setRegisterData(LocalDateTime.now());
        userInfo.setUpdatedAt(LocalDateTime.now());
        userInfo.setUsername(RandomStringUtil.generateRandomString());
        userInfo.setNickname(userInfo.getUsername());
        userInfo.setAvatar(DEFAULT_AVATAR);
        userInfo.setSex("男");
        try {
            userInfoMapper.insert(userInfo);
            userData.setUid(userInfo.getUid());
            userDataMapper.insert(userData);
        } catch (Exception e){
            throw new RvcSQLException(e.getMessage());
        }
        return Map.of("token", TokenUtil.getToken(userInfo.getUid(), userInfo.getUsername()));
    }

    @Override
    public Map<String, String> preCode() {
        return codeUtil.image();
    }

    @Override
    public void sendCode(String email,String code, String uuid, int type) {
        if(!codeUtil.preVerify(uuid, code)){
            throw new ServerException(ResultEnums.PRE_CODE_ERROR);
        }
        switch (type){
            case 0:
                codeUtil.sendCode(email, REGISTER);
                break;
            case 1:
                codeUtil.sendCode(email, LOGIN);
                break;
            case 3:
                codeUtil.sendCode(email, FORGOT_PASSWORD);
                break;
            default:
                throw new ServerException(ResultEnums.FAIL_SEND_VER_CODE);
        }
    }

    @Override
    public void resetPwdEmailCode(String email, String code, String uuid, String uid) {
        if(!codeUtil.preVerify(uuid, code)){
            throw new ServerException(ResultEnums.PRE_CODE_ERROR);
        }
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("uid", uid)
                .eq("email", email);
        if(userInfoMapper.selectCount(queryWrapper) <= 0){
            throw new ServerException(ResultEnums.EMAIL_NOT_BELONG_USER);
        }
        codeUtil.sendCode(email, PASSWORD);
    }

    @Override
    public UserInfoVO one(String uid) {
        UserInfo user = userInfoMapper.selectByUid(uid);
        if(user == null){
            throw new ServerException(ResultEnums.UID_NOT_EIXST);
        }
        return UserInfo.toVO(user);
    }

    @Override
    public Map<String, UserInfoVO> list(List<String> uidList) {
        if(uidList.isEmpty()){
            throw new ServerException(ResultEnums.UID_LIST_IS_EMPTY);
        }
        QueryWrapper<UserInfo> userQuery = new QueryWrapper<>();
        userQuery.in("uid", uidList);

        List<UserInfoVO> userList = new ArrayList<>();
        CopyUtil.copyPropertiesForList(userInfoMapper.selectList(userQuery), userList, UserInfoVO.class);
        return userList.stream().collect(Collectors.toMap(UserInfoVO::getUid, userInfoVO -> userInfoVO));
    }

    @Override
    public void update(UserInfoDTO userInfoDTO, String uid) {
        UserInfo user = userInfoMapper.selectByUid(uid);
//      审核代码
//        boolean flag = false;
//
//        if(!userInfoDTO.getNickname().equals(user.getNickname())){
//            DetectionTaskDTO nicknameTask = DetectionTaskDTO.builder()
//                    .id(uid)
//                    .name(USER_NICKNAME)
//                    .content(userInfoDTO.getNickname())
//                    .build();
//            rabbitMQListener.submit(nicknameTask, "text");
//            user.setNickname("审核中");
//            flag = true;
//        }
//
//        if(!userInfoDTO.getDescription().equals(user.getDescription())){
//            DetectionTaskDTO descriptionTask = DetectionTaskDTO.builder()
//                    .id(uid)
//                    .name(USER_DESCRIPTION)
//                    .content(userInfoDTO.getDescription())
//                    .build();
//            rabbitMQListener.submit(descriptionTask, "text");
//            user.setDescription("审核中");
//            flag = true;
//        }
//        switch (userInfoDTO.getSex()) {
//            case "男":
//            case "女":
//                if (!user.getSex().equals(userInfoDTO.getSex())) {
//                    user.setSex(userInfoDTO.getSex());
//                    flag = true;
//                }
//                break;
//            default:
//               throw new ServerException(ResultEnums.SEX_VALUE_ERROR);
//        }
//        if(userInfoDTO.getBirthday() != null && user.getBirthday() != userInfoDTO.getBirthday()){
//            user.setBirthday(userInfoDTO.getBirthday());
//            flag = true;
//        }
//
//        if(flag){
//            user.setUpdatedAt(LocalDateTime.now());
//            userInfoMapper.updateById(user);
//        }
        BeanUtils.copyProperties(userInfoDTO, user);
        userInfoMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = RvcSQLException.class)
    public void follow(String followUid, String uid) throws RvcSQLException {
        if(uid.equals(followUid)){
            throw new ServerException(ResultEnums.CANT_FOLLOW_YOURSELF);
        }
        if(!userInfoMapper.exist("uid", followUid)){
            throw new ServerException(ResultEnums.USER_NOT_EXIST);
        }
        QueryWrapper<UserFollow> followWrapper = new QueryWrapper<>();
        followWrapper
                .eq("follow_uid", uid)
                .eq("followed_uid", followUid);
        if(userFollowMapper.selectCount(followWrapper) > 0){
            userFollowMapper.delete(followWrapper);
            UserData userData = userDataMapper.selectByUid(uid);
            userData.setFollowNum(userData.getFollowNum() - 1);
            UserData followUserData = userDataMapper.selectByUid(followUid);
            followUserData.setFansNum(followUserData.getFansNum() - 1);
            try {
                userDataMapper.updateById(userData);
                userDataMapper.updateById(followUserData);
            } catch (Exception e){
                throw new RvcSQLException(e.getMessage());
            }
            return;
        }
        try {
            UserFollow follow = new UserFollow();
            follow.setFollowUid(uid);
            follow.setFollowedUid(followUid);
            UserData userData = userDataMapper.selectByUid(uid);
            userData.setFollowNum(userData.getFollowNum() + 1);
            UserData followUserData = userDataMapper.selectByUid(followUid);
            followUserData.setFansNum(followUserData.getFansNum() + 1);
            userDataMapper.updateById(userData);
            userDataMapper.updateById(followUserData);
            userFollowMapper.insert(follow);
        } catch (Exception e){
            throw new RvcSQLException(e.getMessage());
        }
    }

    @Override
    public boolean updatePassword(UpdatePasswordDTO updatePasswordDTO, String uid) {
        if(!codeUtil.emailVerify(updatePasswordDTO.getEmail(), PASSWORD, updatePasswordDTO.getEmailCode())){
            throw new ServerException(ResultEnums.VER_CODE_ERROR);
        }
        UserInfo userInfo = userInfoMapper.selectByUid(uid);
        userInfo.setPassword(DigestUtils.md5Hex(updatePasswordDTO.getPassword()));
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("uid", uid)
                .eq("email", updatePasswordDTO.getPassword());
        return userInfoMapper.update(userInfo, queryWrapper) >= 1;
    }

    @Override
    public void forgotPassword(ForgotPassword forgotPassword) {
        if(!codeUtil.emailVerify(forgotPassword.getEmail(), FORGOT_PASSWORD, forgotPassword.getEmailCode())){
            throw new ServerException(ResultEnums.VER_CODE_ERROR);
        }
        UserInfo userInfo = userInfoMapper.selectByClumneAndValue("email", forgotPassword.getEmail());
        userInfo.setPassword(DigestUtils.md5Hex(forgotPassword.getPassword()));
        userInfoMapper.updateById(userInfo);
    }

    @Override
    public UserInfoVO getUserInfo(String uid) {
        return UserData.toVO(
                UserInfo.toVO(
                        userInfoMapper.selectByUid(uid)
                ),
                userDataMapper.selectByUid(uid)
        );
    }

    @Override
    public Map<String, ?> getUserInfoById(String targetUid, String uid) {
        if(!userInfoMapper.exist("uid", targetUid)){
            throw new ServerException(ResultEnums.USER_NOT_EXIST);
        }
        UserInfoVO userInfoVO = UserData.toVO(
                UserInfo.toVO(
                        userInfoMapper.selectByUid(targetUid)
                ),
                userDataMapper.selectByUid(targetUid)
        );
        boolean follow;
        if(uid.isEmpty()){
            follow = false;
        } else follow = userFollowMapper.exist("follow_uid", uid, "followed_uid", targetUid);
        return Map.of("userInfo", userInfoVO, "follow", follow);
    }

    @Override
    public void avatar(MultipartFile file, String uid){
        if(file.getSize() > MAX_FILE_SIZE){
            throw new ServerException(ResultEnums.FILE_SIZE_LIMIT);
        }
        try {
            UserInfo userInfo = userInfoMapper.selectByUid(uid);
            UploadModelForm form = UploadModelForm.builder()
                    .bucket(FileConfig.USER_BUCKET)
                    .path(FileConfig.USER_PATH)
                    .md5(org.springframework.util.DigestUtils.md5DigestAsHex(file.getInputStream()))
                    .file(file)
                    .build();
            Result<ReceiveUploadFileDTO> result = fileServiceClient.uploadModel(form);
            if(result.getCode() != 200){
                throw new ServerException(result.getCode().toString(), result.getMessage());
            }
            ReceiveUploadFileDTO receiveUploadFileDTO = result.getData();
//            审核代码
//            DetectionTaskDTO imageTask = DetectionTaskDTO.builder()
//                    .id(uid)
//                    .name(USER_AVATAR)
//                    .content(receiveUploadFileDTO.getUrl())
//                    .build();
//            rabbitMQListener.submit(imageTask, "image");
            userInfo.setAvatar(receiveUploadFileDTO.getUrl());
            userInfoMapper.updateById(userInfo);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean exist(String uid) {
        if(!userInfoMapper.exist("uid", uid)){
            throw new ServerException(ResultEnums.USER_NOT_EXIST);
        }
        return true;
    }

    @Override
    public List<UserInfoVO> getMyFollowUser(String uid) {
        List<UserFollow> userFollows = userFollowMapper.selectByMap(Map.of("follow_uid", uid));
        List<String> followedUids = userFollows.stream().map(UserFollow::getFollowedUid).collect(Collectors.toList());
        if(followedUids.isEmpty()){
            return null;
        }
        QueryWrapper<UserInfo> userInfoQueryWrapper = new QueryWrapper<>();
        userInfoQueryWrapper.in("uid", followedUids);
        QueryWrapper<UserData> dataQueryWrapper = new QueryWrapper<>();
        dataQueryWrapper.in("uid", followedUids);
        List<UserInfo> userInfoList = userInfoMapper.selectList(userInfoQueryWrapper);
        Map<String, UserData> userDataMap = userDataMapper.selectList(dataQueryWrapper).stream().collect(Collectors.toMap(UserData::getUid, userData -> userData));
        List<UserInfoVO> userInfoVOS = new ArrayList<>();
        for (UserInfo userInfo:userInfoList){
            UserInfoVO userInfoVO = new UserInfoVO();
            BeanUtils.copyProperties(userInfo, userInfoVO);
            BeanUtils.copyProperties(userDataMap.get(userInfo.getUid()), userInfoVO);
            userInfoVOS.add(userInfoVO);
        }
        return userInfoVOS;
    }

    @Override
    public boolean isFollowed(String uid1, String uid2) {
        return userFollowMapper.exist("follow_uid", uid1, "followed_uid", uid2);
    }

    private String loginByPsw(String email, String password){
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("email", email);

        UserInfo userInfo = userInfoMapper.selectOne(queryWrapper);

        if(userInfo == null || !DigestUtils.md5Hex(password).equals(userInfo.getPassword())){
            throw new ServerException(ResultEnums.WRONG_USERNAME_OR_PASSWORD);
        }
        return TokenUtil.getToken(userInfo.getUid(), userInfo.getUsername());
    }

    private String loginByCode(String email, String code){
        if (!codeUtil.emailVerify(email, LOGIN, code)) {
            throw new ServerException(ResultEnums.VER_CODE_ERROR);
        }
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("email", email);

        UserInfo userInfo = userInfoMapper.selectOne(queryWrapper);

        return TokenUtil.getToken(userInfo.getUid(), userInfo.getUsername());

    }
}
