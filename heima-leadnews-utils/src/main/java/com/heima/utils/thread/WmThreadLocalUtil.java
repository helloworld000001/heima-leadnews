package com.heima.utils.thread;

import com.heima.model.wemedia.pojos.WmUser;

/**
 * @auther 陈彤琳
 * @Description $
 * 2023/12/13 15:46
 */
public class WmThreadLocalUtil {
    private final static ThreadLocal<WmUser> WM_USER_THREAD_LOCAL = new ThreadLocal<>();

    // user存入线程中
    public static void setUser(WmUser wmUser){
        WM_USER_THREAD_LOCAL.set(wmUser);
    }

    // 从线程中获取user
    public static WmUser getUser(){
        return WM_USER_THREAD_LOCAL.get();
    }


    // 清理
    public static void clean(){
        WM_USER_THREAD_LOCAL.remove();
    }
}
