package cn.mpush.base.util.rest;

import java.lang.annotation.*;

/**
 * rest 请求注解
 * 方法注解
 * 方法的返回值不支持如下数据格式
 * List<...Vo>
 * 支持
 * List<JSONObject>  alibaba json
 * @author guanxingya[OF3449]
 * company qianmi.com
 * Date 2019-12-18
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RestMethod {
    /**
     * url
     * @return
     */
    String url() default "";

    /**
     * 请求方式
     * 支持 POST GET
     * @return
     */
    HttpMethod method() default HttpMethod.GET;

    /**
     * 是否表单参数
     * @return
     */
    boolean isFormParam() default false;

    /**
     * 参数是否执行url formatter
     * @return
     */
    boolean isParamExecute() default false;
}
