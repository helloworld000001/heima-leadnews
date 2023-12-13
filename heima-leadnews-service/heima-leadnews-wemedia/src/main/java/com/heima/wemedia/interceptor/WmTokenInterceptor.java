package com.heima.wemedia.interceptor;

import com.heima.model.wemedia.pojos.WmUser;
import com.heima.utils.thread.WmThreadLocalUtil;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @auther 陈彤琳
 * @Description $
 * 2023/12/13 15:39
 */
public class WmTokenInterceptor implements HandlerInterceptor {

    /**
     * 前置处理器：得到header中的用户的信息，并且存入当前的线程中
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String userId = request.getHeader("userId");
        if(userId != null){
            // user存入当前的线程中
            WmUser wmUser = new WmUser();
            wmUser.setId(Integer.parseInt(userId));

            WmThreadLocalUtil.setUser(wmUser);
        }
        return true;
    }

    /**
     * 清理线程中的数据
     * @param request
     * @param response
     * @param handler
     * @param ex
     * @throws Exception
     */

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        WmThreadLocalUtil.clean();
    }
}
