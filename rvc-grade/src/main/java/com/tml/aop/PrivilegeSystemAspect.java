package com.tml.aop;

import com.tml.domain.entity.RvcLevelPrivilege;
import com.tml.service.UserPrivilegeDao;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @NAME: PrivilegeSystemAspect
 * @USER: yuech
 * @Description:
 * @DATE: 2024/2/26
 */
@Slf4j
@Component
@Aspect
public class PrivilegeSystemAspect {

    @Resource
    private UserPrivilegeDao userPrivilegeDao;

    @Pointcut("@annotation(com.tml.aop.annotation.PrivilegeSystem)")
    public void pt(){

    }

    @Around("pt()")
    public Object printLog(ProceedingJoinPoint joinPoint) throws Throwable {
        handleBefore(joinPoint);
        Object ret = joinPoint.proceed();
        return ret;
    }

    /**
     * 进行用户权限校验
     * 1、获取用户uid
     * 2、获取用户权限（经验值获取用户角色  根据角色获取权限）
     * 3、进行权限校验
     * @param joinPoint
     */
    //todo 优化点：1、后面三个不走sql  走本地缓存或者redis  2、对于所有的操作 权限校验和读取数据一起进行  最后的时候判断是否有权限 若无不返回数据

    private void handleBefore(ProceedingJoinPoint joinPoint) throws Exception {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = requestAttributes.getRequest();
        String url = String.valueOf(request.getRequestURL());
        String[] parts = url.split("api");
        String path = parts[parts.length - 1];
        //todo
        // 获取用户uid
        String uid = "33231313445";
        //获取用户权限
        List<String> privilegeVo = userPrivilegeDao.getPrivilege(uid);
        //权限校验
        if (!privilegeVo.contains(path)){
            throw new Exception("用户无权限");
        }
    }
}