package com.quanle.zhan.mvcframework.servlet;

import com.quanle.zhan.mvcframework.annotations.AutoWiredX;
import com.quanle.zhan.mvcframework.annotations.ControllerX;
import com.quanle.zhan.mvcframework.annotations.RequestMappingX;
import com.quanle.zhan.mvcframework.annotations.SecurityX;
import com.quanle.zhan.mvcframework.annotations.ServiceX;
import com.quanle.zhan.mvcframework.pojo.Handler;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author quanle
 * @date 2020/3/17 10:36 PM
 */
public class ZhanDispatcherServlet extends HttpServlet {

    private Properties properties = new Properties();
    //缓存所有的全限定类名
    private List<String> classNames = new ArrayList<>();
    //缓存所有的全限定类名极其实例
    private Map<String, Object> iocMap = new HashMap<>();
    //缓存HandlerMapping映射
    private List<Handler> handlerMappingList = new ArrayList<>();
    //缓存映射后的handler权限用户
    private Map<Handler, Set<String>> handlerSecurityMap = new HashMap<>();

    /**
     * 真正接受处理请求，可以调用doPost
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 设置浏览器字符集编码.
        resp.setHeader("Content-Type", "text/html;charset=UTF-8");
        // 设置response的缓冲区的编码.
        resp.setCharacterEncoding("UTF-8");
        //处理请求
        Handler handler = getHandler(req);
        if (handler == null) {
            resp.getWriter().write("404 not found");
            return;
        }
        //做权限校验
        if (!securityCheck(req, handler)) {
            String username = req.getParameter("username");
            String url = req.getRequestURI();
            resp.getWriter().write("user:" + username + " has been denied to access this url :" + url);
            return;
        }
        //调用之前准备信息，参数绑定
        //获取所有参数类型数组，数组长度，就是args数组的长度
        Class<?>[] parameterTypes = handler.getMethod().getParameterTypes();
        //根据上述数组长度，创建一个新的数组args
        Object[] paramValues = new Object[parameterTypes.length];
        //以下就是为了向上面的数组中放值，且保证顺序一致
        Map<String, String[]> parameterMap = req.getParameterMap();
        parameterMap.forEach((k, v) -> {
            String newV = StringUtils.join(v, ",");
            if (!handler.getParamIndexMapping().containsKey(k)) {
                return;
            }
            Integer kIndex = handler.getParamIndexMapping().get(k);
            paramValues[kIndex] = newV;
        });
        Integer reqIndex = handler.getParamIndexMapping().get(HttpServletRequest.class.getSimpleName());
        Integer respIndex = handler.getParamIndexMapping().get(HttpServletResponse.class.getSimpleName());
        paramValues[reqIndex] = req;
        paramValues[respIndex] = resp;

        try {
            handler.getMethod().invoke(handler.getController(), paramValues);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    //权限校验
    private boolean securityCheck(HttpServletRequest req, Handler handler) {
        if (handlerSecurityMap.size() == 0) {
            return true;
        }
        String username = req.getParameter("username");
        Set<String> users = handlerSecurityMap.get(handler);
        return users.size() > 0 && users.contains(username);
    }

    private Handler getHandler(HttpServletRequest req) {
        if (handlerMappingList.isEmpty()) {
            return null;
        }
        String url = req.getRequestURI();
        for (Handler handler : handlerMappingList) {
            Matcher matcher = handler.getPattern().matcher(url);
            if (!matcher.matches()) {continue;}
            return handler;
        }
        return null;
    }


    @Override
    public void init(ServletConfig config) {
        //加载配置文件 springmvc.properties
        String contextConfigLocation = config.getInitParameter("contextConfigLocation");
        doLoadConfig(contextConfigLocation);
        //扫描注解
        doScan(properties.getProperty("scanPackage"));
        //初始化bean，添加到ioc容器中
        doInstance();
        //维护bean的依赖关系，依赖注入
        doAutoWired();
        //构造一个HandlerMapping（处理器映射器）：将配置好的url和Method建立映射关系
        initHandlerMapping();
        System.out.println("selfMvc 初始化完成。。。。。。");
        //等待请求进入，处理请求
    }

    //构造映射器，最关键环节
    //增加用户权限校验
    private void initHandlerMapping() {
        if (iocMap.isEmpty()) {
            return;
        }
        iocMap.forEach((name, instance) -> {
            Class<?> aClass = instance.getClass();
            //过滤没有ControllerX注解的实例
            if (!aClass.isAnnotationPresent(ControllerX.class)) {
                return;
            }
            //判断类上面是不是有RequestMappingX注解
            String baseUrl = "";
            Set<String> users = new HashSet<>();
            if (aClass.isAnnotationPresent(RequestMappingX.class)) {
                RequestMappingX annotation = aClass.getAnnotation(RequestMappingX.class);
                baseUrl = annotation.value();
            }
            if (aClass.isAnnotationPresent(SecurityX.class)) {
                SecurityX securityX = aClass.getAnnotation(SecurityX.class);
                String[] value = securityX.value();
                if (value.length > 0) {
                    Collections.addAll(users, value);
                }
            }
            Method[] methods = aClass.getMethods();
            for (Method method : methods) {
                //获取method的RequestMappinX的注解
                if (!method.isAnnotationPresent(RequestMappingX.class)) {
                    continue;
                }
                RequestMappingX annotation = method.getAnnotation(RequestMappingX.class);
                String methodUrl = annotation.value();
                String url = baseUrl + methodUrl;
                //把method所有信息及url封装成handler
                Handler handler = new Handler(instance, method, Pattern.compile(url));
//                handlerMapping.put(url, method);
                Parameter[] parameters = method.getParameters();
                for (int i = 0; i < parameters.length; i++) {
                    Parameter parameter = parameters[i];
                    if (parameter.getType() == HttpServletRequest.class
                            || parameter.getType() == HttpServletResponse.class) {
                        //如果参数是以上两个，那么参数名对应为HttpServletRequest/HttpServletResponse
                        handler.getParamIndexMapping().put(parameter.getType().getSimpleName(), i);
                    } else {
                        handler.getParamIndexMapping().put(parameter.getName(), i);
                    }
                }
                handlerMappingList.add(handler);
                //权限用户
                Set<String> methodUsers = new HashSet<>(users);
                if (method.isAnnotationPresent(SecurityX.class)) {
                    SecurityX securityX = method.getAnnotation(SecurityX.class);
                    String[] value = securityX.value();
                    if (value.length > 0) {
                        Collections.addAll(methodUsers, value);
                    }
                }
                handlerSecurityMap.put(handler, methodUsers);
            }
        });
    }

    //实现依赖注入
    private void doAutoWired() {
        if (iocMap.size() == 0) {
            return;
        }
        iocMap.forEach((k, v) -> {
            //获取bean对象中的字段信息
            Field[] declaredFields = v.getClass().getDeclaredFields();
            for (Field field : declaredFields) {
                if (!field.isAnnotationPresent(AutoWiredX.class)) {
                    //没有注解AutoWiredX
                    continue;
                }
                //有注解AutoWiredX
                AutoWiredX autoWiredX = field.getAnnotation(AutoWiredX.class);
                //获取注解的value
                String value = autoWiredX.value();
                if ("".equals(value)) {
                    //如果为空，则默认用当前字段类型注入
                    value = field.getType().getName();
                }
                //开启赋值
                field.setAccessible(true);
                try {
                    field.set(v, iocMap.get(value));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    //初始化Ioc容器
    //基于缓存的classNames类的全限定类名，以及反射技术，完成对象的创建与管理。
    private void doInstance() {
        if (classNames.size() == 0) {
            return;
        }
        try {
            for (String className : classNames) {
                Class<?> aClass = Class.forName(className);
                if (aClass.isAnnotationPresent(ControllerX.class)) {
                    String simpleName = aClass.getSimpleName();
                    String lowerFirst = lowerFirst(simpleName);
                    Object instance = aClass.newInstance();
                    iocMap.put(lowerFirst, instance);
                } else if (aClass.isAnnotationPresent(ServiceX.class)) {
                    ServiceX serviceX = aClass.getAnnotation(ServiceX.class);
                    //获取注解的value值
                    String value = serviceX.value();
                    //如果value不为空，就以此值为准
                    if ("".equals(value.trim())) {
                        //如果value为空，就用类名首字母小写来当做value
                        value = lowerFirst(aClass.getSimpleName());
                    }
                    iocMap.put(value, aClass.newInstance());
                    //处理service层的接口
                    Class<?>[] aClassInterfaces = aClass.getInterfaces();
                    for (Class<?> anInterface : aClassInterfaces) {
                        iocMap.put(anInterface.getName(), aClass.newInstance());
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
    }

    private String lowerFirst(String simpleName) {
        if (Character.isLowerCase(simpleName.charAt(0))) {
            return simpleName;
        }
        return Character.toLowerCase(simpleName.charAt(0))
                + simpleName.substring(1);
    }

    //扫描类
    private void doScan(String scanPackage) {
        String scanPackagePath =
                Thread.currentThread().getContextClassLoader().getResource("").getPath() + scanPackage.replaceAll("\\.",
                        "/");
        File file = new File(scanPackagePath);
        File[] files = file.listFiles();
        assert files != null;
        for (File f : files) {
            if (f.isDirectory()) {
                doScan(scanPackage + "." + f.getName());
            } else if (f.getName().endsWith(".class")) {
                String s = scanPackage + "." + f.getName().replaceAll(".class", "");
                classNames.add(s);
            }
        }
    }

    //加载配置文件
    private void doLoadConfig(String contextConfigLocation) {
        //首先要将文件读成流
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            properties.load(resourceAsStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
