package com.tml.service;

import com.google.gson.JsonObject;
import com.tml.pojo.dto.ChatRoom;
import com.tml.pojo.vo.CreateRoomRequest;
import com.tml.pojo.vo.RoomDetailInfoVO;
import io.netty.channel.ChannelHandlerContext;

/**
 * @Description
 * @Author welsir
 * @Date 2024/2/19 17:06
 */

public interface RoomService {

    void createRoom(ChannelHandlerContext ctx, Object params, String uid);
}