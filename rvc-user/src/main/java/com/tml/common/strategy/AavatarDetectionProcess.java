package com.tml.common.strategy;

import com.tml.pojo.DO.UserInfo;
import com.tml.pojo.dto.DetectionStatusDto;

import java.time.LocalDateTime;

import static com.tml.config.DetectionConfig.*;

/**
 * @Date 2023/12/20
 * @Author xiaochun
 */
public class AavatarDetectionProcess extends UserDetectionStrategy{
    @Override
    public UserInfo process(DetectionStatusDto detectionStatusDto, UserInfo user, String content) {
        if (!detectionStatusDto.getLabels().equals(NON_LABEL)) {
            user.setAvatar(RETURN);
            user.setUpdatedAt(LocalDateTime.now());
            return user;
        }
        user.setUpdatedAt(LocalDateTime.now());
        user.setAvatar(content);
        return user;
    }
}
