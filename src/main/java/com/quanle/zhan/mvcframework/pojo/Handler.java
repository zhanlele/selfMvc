package com.quanle.zhan.mvcframework.pojo;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 封装handler的相关信息
 * @author quanle
 * @date 2020/3/18 1:22 AM
 */
public class Handler {
    private Pattern pattern;
    private Map<String,Integer> paramIndexMapping;
    private Object controller;
    private Method method;

    public Handler(
            Object controller,
            Method method,
            Pattern pattern) {
        this.pattern = pattern;
        this.paramIndexMapping = new HashMap<>();
        this.controller = controller;
        this.method = method;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public Map<String, Integer> getParamIndexMapping() {
        return paramIndexMapping;
    }

    public void setParamIndexMapping(Map<String, Integer> paramIndexMapping) {
        this.paramIndexMapping = paramIndexMapping;
    }

    public Object getController() {
        return controller;
    }

    public void setController(Object controller) {
        this.controller = controller;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }
}
