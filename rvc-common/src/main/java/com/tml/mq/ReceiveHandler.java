package com.tml.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tml.pojo.dto.DetectionStatusDto;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

import static com.tml.constant.DetectionConstants.*;
import static com.tml.constant.DetectionConstants.AUDIO_ROUTER_KEY;

/**
 * @NAME: ReceiveHandler
 * @USER: yuech
 * @Description:
 * @DATE: 2023/12/19
 */
@Component
public abstract class ReceiveHandler {
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(),
            exchange = @Exchange(name = RES_EXCHANGE_NAME,type = ExchangeTypes.TOPIC),
            key = TEXT_ROUTER_KEY
    ))
    public void receive_text(Message message) throws JsonProcessingException{
        DetectionStatusDto detectionTaskDto = getRes(message);
        process(detectionTaskDto);
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(),
            exchange = @Exchange(name = RES_EXCHANGE_NAME,type = ExchangeTypes.TOPIC),
            key = IMAGE_ROUTER_KEY
    ))
    public void receive_image(Message message) throws Exception {
        DetectionStatusDto detectionTaskDto = getRes(message);
        process(detectionTaskDto);
    }


    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(),
            exchange = @Exchange(name = RES_EXCHANGE_NAME,type = ExchangeTypes.TOPIC),
            key = AUDIO_ROUTER_KEY
    ))
    public void receive_audio(Message message) throws Exception {
        DetectionStatusDto detectionTaskDto = getRes(message);
        process(detectionTaskDto);
    }

    public abstract void process(DetectionStatusDto detectionTaskDto);


    public DetectionStatusDto getRes(Message message) throws JsonProcessingException {
        String content = new String(message.getBody(), StandardCharsets.UTF_8);
        ObjectMapper objectMapper = new ObjectMapper();
        DetectionStatusDto detectionTaskDto = objectMapper.readValue(content, DetectionStatusDto.class);
        return detectionTaskDto;
    }
}

