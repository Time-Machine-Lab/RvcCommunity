package com.tml.aspect.annotation;

import com.tml.enums.ContentDetectionEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要进行内容检测的方法
 *传入 类型和交换机名字
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ContentDetection {
    ContentDetectionEnum type();
    String exchangeName();

}
