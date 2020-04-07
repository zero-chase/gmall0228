package com.atguigu.gmall.util;

import com.alibaba.fastjson.JSON;
import io.jsonwebtoken.impl.Base64UrlCodec;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TestJwt {

    public static void main(String[] args) {
        Map<String,Object> map = new HashMap<>();
        map.put("memberId","1");
        map.put("nickName","张三");
        String ip = "127.0.0.1";
        String time = new SimpleDateFormat("yyyyMMdd HHmmss").format(new Date());
        String encode = JwtUtil.encode("2020gmall0228", map, ip + time);
        System.err.println(encode);

        //base64解析
        //String tokenUserInfo = StringUtils.substringBetween(encode, ".");
        Base64UrlCodec base64UrlCodec = new Base64UrlCodec();
        //eyJhbGciOiJIUzI1NiJ9.eyJuaWNrTmFtZSI6IuW8oOS4iSIsIm1lbWJlcklkIjoiMSJ9.sc7qXzXbIFHkvG-CKWIJVfjucSj5qd7Jc2QHQaMDzr0
        byte[] tokenBytes = base64UrlCodec.decode("eyJuaWNrTmFtZSI6IuW8oOS4iSIsIm1lbWJlcklkIjoiMSJ9");
        String tokenJson = null;
        try {
            tokenJson = new String(tokenBytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Map map1 = JSON.parseObject(tokenJson, Map.class);
        System.out.println("64="+map1);
    }
}
