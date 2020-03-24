package com.quanle.zhan.demo.service.impl;

import com.quanle.zhan.demo.service.IDemoService;
import com.quanle.zhan.mvcframework.annotations.ServiceX;

/**
 * @author quanle
 * @date 2020/3/18 12:58 AM
 */
@ServiceX
public class DemoServiceImpl implements IDemoService {

    @Override
    public String handler01() {
        return "%s 有handler01访问权限";
    }

    @Override
    public String handler02() {
        return "%s 有handler02访问权限";
    }
}
