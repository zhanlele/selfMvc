package com.quanle.zhan.demo.controller;

import com.quanle.zhan.demo.service.IDemoService;
import com.quanle.zhan.mvcframework.annotations.AutoWiredX;
import com.quanle.zhan.mvcframework.annotations.ControllerX;
import com.quanle.zhan.mvcframework.annotations.RequestMappingX;
import com.quanle.zhan.mvcframework.annotations.SecurityX;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author quanle
 * @date 2020/3/18 12:57 AM
 */
@ControllerX
@RequestMappingX("/demo")
public class DemoController {

    @AutoWiredX
    private IDemoService demoService;

    @RequestMappingX("/handler01")
    @SecurityX(value = {"zhangsan", "lisi"})
    public void handler01(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String handler01 = demoService.handler01();
        resp.getWriter().write(String.format(handler01, req.getParameter("username")));
    }

    @RequestMappingX("/handler02")
    @SecurityX(value = {"wangliu", "zhaowu"})
    public void handler02(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String handler02 = demoService.handler02();
        resp.getWriter().write(String.format(handler02, req.getParameter("username")));
    }
}
