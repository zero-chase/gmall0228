package com.atguigu.gmall.interceptors;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.HttpclientUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 拦截代码
        //1.判断拦截的请求的访问的方法的注解（是否需要拦截）
        HandlerMethod hm = (HandlerMethod) handler;
        LoginRequired methodAnnotation = hm.getMethodAnnotation(LoginRequired.class); //反射得到方法注解

        /*StringBuffer url = request.getRequestURL();
        System.out.println(url);*/

        //是否需要拦截
        if (methodAnnotation==null){
            //没有注解，不需要登录
            return true;
        }

        String token = "";
        //获取页面cookie中的token
        String oldToken = CookieUtil.getCookieValue(request, "oldToken", true);

        if(StringUtils.isNotBlank(oldToken)){
            token = oldToken;
        }
        //获取http请求中携带的token
        String newToken = request.getParameter("token");
        if(StringUtils.isNotBlank(newToken)){
            token = newToken;
        }



        //是否需要登录
        boolean loginSuccess = methodAnnotation.loginSuccess();  //获取该请求是否必须登录成功

        //调用认证中心进行验证
        String success = "fail";
        Map successMap = new HashMap();
        if(StringUtils.isNotBlank(token)){
            String ip = request.getHeader("x-forwarded-for"); //通过nginx转发的客户端ip
            if (StringUtils.isBlank(ip)){
                ip = request.getRemoteAddr(); //从request中获取ip
                if (StringUtils.isBlank(ip)){
                    ip = "127.0.0.1";
                }
            }
            String successJson = HttpclientUtil.doGet("http://passport.gmall.com/verify?token=" + token + "&currentIp="+ip);

            successMap = JSON.parseObject(successJson,Map.class);

            success = (String) successMap.get("status");
        }


        if (loginSuccess){ //true
            //必须成功之后才能使用
            if (!success.equals("success")){
                //重定向到passport登录
                StringBuffer requestURL = request.getRequestURL();
                response.sendRedirect("http://passport.gmall.com/index?ReturnUrl="+requestURL);
                return false;
            }
            //需要将token携带的用户信息写入
            request.setAttribute("memberId", successMap.get("memberId"));
            request.setAttribute("nickname", successMap.get("nickname"));
            //验证通过success="success"，覆盖cookie中的token
            if (StringUtils.isNotBlank(token)){

                CookieUtil.setCookie(request,response,"oldToken",token,60*60*2,true);
            }
        }else {  //false
            //没有登录也能用，但必须验证
            if (success.equals("success")){
                //需要将token携带的用户信息写入
                request.setAttribute("memberId",successMap.get("memberId"));
                request.setAttribute("nickname", successMap.get("nickname"));
                //验证通过success="success"，覆盖cookie中的token
                if (StringUtils.isNotBlank(token)){

                    CookieUtil.setCookie(request,response,"oldToken",token,60*60*2,true);
                }
            }
        }

        return true;
    }
}
