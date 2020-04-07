package com.atguigu.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.util.HttpclientUtil;
import com.atguigu.gmall.util.JwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {

    @Reference
    UserService userService;

    @RequestMapping("index")
    private String index(String ReturnUrl, ModelMap modelMap){
        modelMap.put("ReturnUrl",ReturnUrl);
        return "index";
    }

    @RequestMapping("login")
    @ResponseBody
    private String login(UmsMember umsMember, HttpServletRequest request){

        String token = "";

        //调用用户服务验证用户名和密码
        UmsMember umsMemberLogin = userService.login(umsMember);
        if (umsMemberLogin!=null){
            //登录成功

            //用jwt制作token
            String memberId = umsMemberLogin.getId();
            String nickname = umsMemberLogin.getNickname();
            token = getToken(memberId,nickname,request);

            //将token存入redis一份
            userService.addUserToken(token,memberId);

        }else{
            //登录失败
            token = "fail";
        }
        return token;
    }

    private String getToken(String memberId, String nickname, HttpServletRequest request) {

        String token = "";
        Map<String,Object> userMap = new HashMap<>();
        userMap.put("memberId",memberId);
        userMap.put("nickname",nickname);

        String ip = request.getHeader("x-forwarded-for"); //通过nginx转发的客户端ip
        if (StringUtils.isBlank(ip)){
            ip = request.getRemoteAddr(); //从request中获取ip
            if (StringUtils.isBlank(ip)){
                ip = "127.0.0.1";
            }
        }

        //按照设计算法对参数进行加密，生成token
        token = JwtUtil.encode("2020gmall0228", userMap, ip);

        return token;
    }

    /**
     * 第三方登录（微博登录）
     * @param code
     * @param request
     * @return
     */
    @RequestMapping("vlogin")
    private String vlogin(String code,HttpServletRequest request){

        //授权码换取access_token
        //获取access_token必须是post请求
        //App Secret(client_secret)：6b664203be177686e9c3b063ddb293b9
        //"access_token":"2.00d4janC5zRk3C48000a1179xkJyXD","remind_in":"157679999","expires_in":157679999,"uid":"2565060381","isRealName":"true"}"
        String s3 = "https://api.weibo.com/oauth2/access_token";
        Map<String,String> paramMap = new HashMap<>();
        paramMap.put("client_id","2168377298");
        paramMap.put("client_secret","6b664203be177686e9c3b063ddb293b9"); //安全码
        paramMap.put("grant_type","authorization_code");
        paramMap.put("redirect_uri","http://passport.gmall.com/vlogin");
        //code用后即毁
        //access_token在几天内是一样的
        paramMap.put("code",code);
        String access_token_json = HttpclientUtil.doPost(s3, paramMap);

        Map<String,Object> access_map = JSON.parseObject(access_token_json, Map.class);

        //access_token换取用户信息
        String access_token = (String) access_map.get("access_token");
        String uid = (String) access_map.get("uid");

        String show_user_url = "https://api.weibo.com/2/users/show.json?access_token="+access_token+"&uid="+uid;

        String user_json = HttpclientUtil.doGet(show_user_url);
        Map<String,Object> user_map = JSON.parseObject(user_json, Map.class);

        //将用户信息保存至数据库，用户类型设置为微博用户
        UmsMember umsMember = new UmsMember();
        umsMember.setSourceUid(uid);
        umsMember.setSourceType("2");
        umsMember.setAccessCode(code);
        umsMember.setAccessToken(access_token);
        umsMember.setCity((String) user_map.get("location"));
        umsMember.setNickname((String) user_map.get("screen_name"));
        umsMember.setCreateTime(new Date());

        UmsMember umsCheck = new UmsMember();
        umsCheck.setSourceUid(umsMember.getSourceUid());
        UmsMember umsMemberCheck = userService.checkOauthUser(umsCheck); //检查该用户（社交用户）以前是否登录过系统
        //判断之前有没有社交登录
        if (umsMemberCheck==null){
            umsMember = userService.addOauthUser(umsMember);
        }else {
            umsMember = umsMemberCheck;
        }

        //生成jwt的token,并且重定向到首页，携带该token
        String memberId = umsMember.getId();  //rpc的主键返回策略失效
        String nickname = umsMember.getNickname();
        String token = getToken(memberId, nickname, request);

        //将token存入redis一份
        userService.addUserToken(token,memberId);//memberId是保存数据库与后主键返回策略生成的id

        return  "redirect:http://search.gmall.com/index?token="+token;
    }

    @RequestMapping("verify")
    @ResponseBody
    private String verify(String token,String currentIp){

        //通过jwt校验token真假
        Map<String,String> map = new HashMap<>();

        Map<String, Object> decode = JwtUtil.decode(token, "2020gmall0228", currentIp);

        if (decode!=null){

            map.put("status","success");
            map.put("memberId", (String) decode.get("memberId"));
            map.put("nickname", (String) decode.get("nickname"));
        }else {
            map.put("status","fail");
        }
        return JSON.toJSONString(map);
    }

}
