package com.atguigu.gmall.search.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.AttrService;
import com.atguigu.gmall.service.SearchService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;

@Controller
public class SearchController {

    @Reference
    SearchService searchService;

    @Reference
    AttrService attrService;

    @RequestMapping("index")
    @LoginRequired(loginSuccess = false)
    public String index(){
        return "index";
    }

    @GetMapping("list.html")
    public String index(PmsSearchParam pmsSearchParam, ModelMap modelMap){ //三级分类、关键字

        //调用搜索服务，返回搜索结果
        List<PmsSearchSkuInfo> pmsSearchSkuInfos = searchService.list(pmsSearchParam);
        modelMap.put("skuLsInfoList",pmsSearchSkuInfos);

        //抽取检索结果所包含的平台属性集合
        Set<String> valueIdSet = new HashSet<>();
        for (PmsSearchSkuInfo pmsSearchSkuInfo : pmsSearchSkuInfos) {
            List<PmsSkuAttrValue> skuAttrValueList = pmsSearchSkuInfo.getSkuAttrValueList();
            for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
                String valueId = pmsSkuAttrValue.getValueId();
                valueIdSet.add(valueId);
            }
        }
        //根据valueId将属性列表查询出来
        List<PmsBaseAttrInfo> pmsBaseAttrInfos = attrService.getAttrValueListByValueId(valueIdSet);
        modelMap.put("attrList",pmsBaseAttrInfos);

        //对平台属性集合进一步处理，去掉当前条件中valueId所在的属性组
        String[] delValueIds = pmsSearchParam.getValueId();
        if (delValueIds!=null){
            //面包屑
            //pmsSearchParam
            //delValueIds
            List<PmsSearchCrumb> pmsSearchCrumbs = new ArrayList<>();
            for (String delValueId : delValueIds) {
                //使用迭代器(使用arrayList会报数组下标越界)
                Iterator<PmsBaseAttrInfo> iterator = pmsBaseAttrInfos.iterator();//放在循环里面
                PmsSearchCrumb pmsSearchCrumb = new PmsSearchCrumb();
                //生成面包屑的参数
                pmsSearchCrumb.setValueId(delValueId);
                pmsSearchCrumb.setUrlParam(getUrlParamForCrumb(pmsSearchParam,delValueId));


                while (iterator.hasNext()) {
                    PmsBaseAttrInfo pmsBaseAttrInfo = iterator.next();
                    List<PmsBaseAttrValue> attrValueList = pmsBaseAttrInfo.getAttrValueList();
                    for (PmsBaseAttrValue pmsBaseAttrValue : attrValueList) {
                        String valueId = pmsBaseAttrValue.getId();
                        if (valueId.equals(delValueId)) {
                            //查找面包屑属性值名称
                            pmsSearchCrumb.setValueName(pmsBaseAttrValue.getValueName());
                            //删除该属性值所在的属性组
                            iterator.remove();
                        }
                    }
                }
                pmsSearchCrumbs.add(pmsSearchCrumb);
            }
            modelMap.put("attrValueSelectedList",pmsSearchCrumbs);
        }

        String urlParam = getUrlParam(pmsSearchParam);
        modelMap.put("urlParam",urlParam);
        String keyword = pmsSearchParam.getKeyword();
        if (StringUtils.isNotBlank(keyword)){
            modelMap.put("keyword",keyword);
        }

        return "list";
    }

    private String getUrlParam(PmsSearchParam pmsSearchParam,String ... delValueId) {
        String keyword = pmsSearchParam.getKeyword();
        String catalog3Id = pmsSearchParam.getCatalog3Id();
        String[] skuAttrValueList = pmsSearchParam.getValueId();

        String urlParam = "";
        if (StringUtils.isNotBlank(keyword)){
            if (StringUtils.isNotBlank(urlParam)){
                urlParam = urlParam+"&";
            }
            urlParam = urlParam+"keyword="+keyword;
        }
        if (StringUtils.isNotBlank(catalog3Id)){
            if (StringUtils.isNotBlank(urlParam)){
                urlParam = urlParam+"&";
            }
            urlParam = urlParam+"catalog3Id="+catalog3Id;
        }
        if (skuAttrValueList!=null){
            for (String valueId : skuAttrValueList) {
                if (!valueId.equals(delValueId)){

                    urlParam = urlParam+"&valueId="+valueId;
                }
            }
        }
        return urlParam;
    }

    private String getUrlParamForCrumb(PmsSearchParam pmsSearchParam,String delValueId) {
        String keyword = pmsSearchParam.getKeyword();
        String catalog3Id = pmsSearchParam.getCatalog3Id();
        String[] skuAttrValueList = pmsSearchParam.getValueId();

        String urlParam = "";
        if (StringUtils.isNotBlank(keyword)){
            if (StringUtils.isNotBlank(urlParam)){
                urlParam = urlParam+"&";
            }
            urlParam = urlParam+"keyword="+keyword;
        }
        if (StringUtils.isNotBlank(catalog3Id)){
            if (StringUtils.isNotBlank(urlParam)){
                urlParam = urlParam+"&";
            }
            urlParam = urlParam+"catalog3Id="+catalog3Id;
        }
        if (skuAttrValueList!=null){
            for (String valueId : skuAttrValueList) {
                if (!valueId.equals(delValueId)){

                    urlParam = urlParam+"&valueId="+valueId;
                }
            }
        }
        return urlParam;
    }
}
