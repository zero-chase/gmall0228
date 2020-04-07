package com.atguigu.gmall.passport.controller;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.util.HttpclientUtil;

import java.util.HashMap;
import java.util.Map;

public class TestOauth2 {

    public static String getCode(){
        //1获取授权码
        //App Key：2168377298
        //授权回调页：http://passport.gmall.com/vlogin
        String s1 = HttpclientUtil.doGet("https://api.weibo.com/oauth2/authorize?client_id=2168377298&response_type=code&redirect_uri=http://passport.gmall.com/vlogin");
        //System.out.println(s1);

        //在第一步和第二步之间有个用户操作页面

        //2返回授权码到回调地址
        String code = "http://passport.gmall.com/vlogin?code=0dc218220d2c146d60d36d4b85ea92c7";

        //保存至数据库 access_code

        return code;
    }

    public static Map<String,String> getAccess_token(){

        //3.获取access_token必须是post请求
        //App Secret(client_secret)：6b664203be177686e9c3b063ddb293b9
        //"access_token":"2.00d4janC5zRk3C48000a1179xkJyXD","remind_in":"157679999","expires_in":157679999,"uid":"2565060381","isRealName":"true"}"
        String s3 = "https://api.weibo.com/oauth2/access_token";
        Map<String,String> paramMap = new HashMap<>();
        paramMap.put("client_id","2168377298");
        paramMap.put("client_secret","6b664203be177686e9c3b063ddb293b9");
        paramMap.put("grant_type","authorization_code");
        paramMap.put("redirect_uri","http://passport.gmall.com/vlogin");
        //code用后即毁
        paramMap.put("code",getCode());

        String access_token_json = HttpclientUtil.doPost(s3, paramMap);

        Map<String,String> map = JSON.parseObject(access_token_json, Map.class);
        //access_token在几天内是一样的
        String access_token = map.get("access_token");
        //保存至数据库 access_token

        return map;
    }

    public static Map<String,String> getUserInfo(){
        //4.用access_token查询用户信息
        Map<String, String> map1 = getAccess_token();
        String s4 = "https://api.weibo.com/2/users/show.json?access_token="+map1.get("access_token")+"&uid="+map1.get("uid");

        String user_json = HttpclientUtil.doGet(s4);
        Map<String,String> map = JSON.parseObject(user_json, Map.class);
        return map;
    }


    public static void main(String[] args) {

        //微博开放平台测试
        //https://open.weibo.com/
    }
}
