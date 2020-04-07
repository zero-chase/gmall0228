package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.util.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller
public class CartController {

    @Reference
    SkuService skuService;

    @Reference
    CartService cartService;


    @RequestMapping("addToCart")
    @LoginRequired(loginSuccess = false)
    public String addToCart(String skuId, int quantity, HttpServletRequest request, HttpServletResponse response){

        List<OmsCartItem> omsCartItems = new ArrayList<>();
        //1.调用商品服务查询商品信息
        PmsSkuInfo skuInfo = skuService.getSkuById(skuId, "");

        //2.将商品信息封装成购物车信息
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setCreateDate(new Date());
        omsCartItem.setDeleteStatus(0);
        omsCartItem.setModifyDate(new Date());
        omsCartItem.setPrice(skuInfo.getPrice());
        omsCartItem.setProductAttr("");
        omsCartItem.setProductBrand("");
        omsCartItem.setProductCategoryId(skuInfo.getCatalog3Id());
        omsCartItem.setProductId(skuInfo.getProductId());
        omsCartItem.setProductName(skuInfo.getSkuName());
        omsCartItem.setProductPic(skuInfo.getSkuDefaultImg());
        omsCartItem.setProductSkuCode("1111111111");
        omsCartItem.setProductSkuId(skuInfo.getId());
        omsCartItem.setQuantity(new BigDecimal(quantity));
        omsCartItem.setIsChecked("1");

        //3.判断用户是否登录
        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");

        if (StringUtils.isBlank(memberId)){
            //用户没有登录

            //cookie中原有的购物车数据
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);

            //判断是否存在cookie
            if (StringUtils.isBlank(cartListCookie)){
                //cookie为空
                omsCartItems.add(omsCartItem);

            }else{
                //cookie不为空
                omsCartItems = JSON.parseArray(cartListCookie, OmsCartItem.class);
                //判断添加的商品里面在购物车里面是否存在
                boolean exist = if_cart_exist(omsCartItems,omsCartItem);

                if (exist){
                    //之前添加过，更新购物车添加数量
                    for (OmsCartItem cartItem : omsCartItems) {
                        if (cartItem.getProductSkuId().equals(omsCartItem.getProductSkuId())){
                            cartItem.setQuantity(cartItem.getQuantity().add(omsCartItem.getQuantity())); //BigDecimal中add相当于加
                        }
                    }

                }else{
                    //之前没有添加，新增当前的购物车
                    omsCartItems.add(omsCartItem);
                }
            }

            //更新cookie
            CookieUtil.setCookie(request,response,"cartListCookie", JSON.toJSONString(omsCartItems),60*60*72,true);//3天过期

        }else {
            //用户已经登录

            //从DB中查询购物车数据
            OmsCartItem omsCartItemFromFb = cartService.ifCartExistByUser(memberId,skuId);

            if (omsCartItemFromFb==null){
                //该用户没有添加过当前商品
                omsCartItem.setMemberId(memberId);
                omsCartItem.setMemberNickname("测试");
                omsCartItem.setQuantity(new BigDecimal(quantity));
                cartService.addCart(omsCartItem);
            }else{
                //当前用户添加过当前商品
                omsCartItemFromFb.setQuantity(omsCartItemFromFb.getQuantity().add(omsCartItem.getQuantity()));
                cartService.updateCart(omsCartItemFromFb);
            }

            //同步缓存
            cartService.flushCartCache(memberId);

        }

        return "redirect:/success.html";
    }

    /**
     * 商品结算
     * @return
     */
    @RequestMapping("cartList")
    @LoginRequired(loginSuccess = false)
    public String cartList(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap){

        List<OmsCartItem> omsCartItems = new ArrayList<>();
        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");

        //查询cookie
        String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);

        if (StringUtils.isNotBlank(memberId)){
            //已经登录查询db(从缓存中拿数据)
            omsCartItems = cartService.cartList(memberId);
        }else{
            //没有登录查询cookie
            if(StringUtils.isNotBlank(cartListCookie)){
                omsCartItems = JSON.parseArray(cartListCookie,OmsCartItem.class);
            }
        }
        for (OmsCartItem omsCartItem : omsCartItems) {
            omsCartItem.setTotalPrice(omsCartItem.getPrice().multiply(omsCartItem.getQuantity())); //BigDecimal相乘
        }

        modelMap.put("cartList",omsCartItems);
        modelMap.put("memberId",memberId);
        //被勾选的商品的总额
        BigDecimal totalAmount = getTotalAmount(omsCartItems);
        modelMap.put("totalAmount",totalAmount);
        return "cartList";
    }

    private BigDecimal getTotalAmount(List<OmsCartItem> omsCartItems) {
        BigDecimal totalAmount = new BigDecimal("0");

        for (OmsCartItem omsCartItem : omsCartItems) {
            BigDecimal totalPrice = omsCartItem.getTotalPrice();
            //被选中
            if ("1".equals(omsCartItem.getIsChecked())){
                totalAmount = totalAmount.add(totalPrice);
            }
        }
        return totalAmount;
    }


    @RequestMapping("checkCart")
    public String checkCart(String isChecked, String skuId, String memberId,HttpServletRequest request, ModelMap modelMap){

        //调用服务，修改状态
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        omsCartItem.setIsChecked(isChecked);
        omsCartItem.setProductSkuId(skuId);
        cartService.checkCart(omsCartItem);

        //将最新的数据从缓存中查询，渲染给内嵌页
        List<OmsCartItem> omsCartItems = cartService.cartList(memberId);

        modelMap.put("cartList",omsCartItems);
        //被勾选的商品的总额
        BigDecimal totalAmount = getTotalAmount(omsCartItems);
        /*//同步缓存
        cartService.flushCartCache(memberId);*/
        modelMap.put("totalAmount",totalAmount);
        modelMap.put("memberId",memberId);

        return "cartListInner";
    }

    @RequestMapping("quantityChanged")
    public String quantityChanged(String quantity, String skuId,String memberId,HttpServletRequest request, ModelMap modelMap){

        //调用服务，修改购买商品数量
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        omsCartItem.setQuantity(new BigDecimal(quantity));
        omsCartItem.setProductSkuId(skuId);
        cartService.quantityChanged(omsCartItem);

        //将最新的数据从缓存中查询，渲染给内嵌页
        List<OmsCartItem> omsCartItems = cartService.cartList(memberId);

        modelMap.put("cartList",omsCartItems);
        //被勾选的商品的总额
        BigDecimal totalAmount = getTotalAmount(omsCartItems);

        modelMap.put("memberId",memberId);
        modelMap.put("totalAmount",totalAmount);

        return "cartListInner";
    }

    private boolean if_cart_exist(List<OmsCartItem> omsCartItems, OmsCartItem omsCartItem) {
        boolean isFind = false;
        for (OmsCartItem cartItem : omsCartItems) {
            String productSkuId = cartItem.getProductSkuId();
            if (productSkuId.equals(omsCartItem.getProductSkuId())){
                isFind = true;
            }
        }
        return isFind;
}
}
