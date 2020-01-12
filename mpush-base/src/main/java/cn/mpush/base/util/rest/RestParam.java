package cn.mpush.base.util.rest;

import java.lang.annotation.*;

/**
 * rest 请求注解
 * 入参注解
 * @author guanxingya[OF3449]
 * company qianmi.com
 * Date 2019-12-18
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RestParam {

    /**
     * 参数名称
     * @return
     */
    String name() default "";

    /**
     * 参数是否执行url formatter
     * RestMethod isParamExecute为true 该配置才生效
     * @return
     */
    boolean isParamExecute() default false;

    /**
     * 参数执行url formatter的顺序
     * RestMethod isParamExecute为true
     * RestParam  isParamExecute为true
     * 该配置才生效
     * @return
     */
    int executeIndex() default 0;

    /**
     * 参数是否是请求的body对象
     * body对象直接转换为json 发送http请求
     * @return
     */
    boolean requestBody() default false;
}