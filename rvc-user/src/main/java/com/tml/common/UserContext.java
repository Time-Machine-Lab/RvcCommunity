package com.tml.common;

import com.tml.exception.ServerException;
import com.tml.pojo.DO.AuthUser;
import com.tml.pojo.enums.ResultEnums;

/**
 * @Date 2023/12/12
 * @Author xiaochun
 */
public class UserContext {
    private static final ThreadLocal<AuthUser> threadLocal = new ThreadLocal<>();
    AuthUser authUser;
    public static AuthUser getCurrentUser(){
        AuthUser authUser;
        try {
            authUser = threadLocal.get();
            System.out.println("UserContext Thread ID: " + Thread.currentThread().getId());
        } catch (Exception e){
            throw new ServerException(ResultEnums.NO_LOGIN);
        }
        return authUser;
    }


    public static void setCurruntUser(String uid, String username){

    }
}
