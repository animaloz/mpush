package cn.mpush.base.util.rest;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import net.sf.cglib.beans.BeanMap;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.lang.reflect.*;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * rest 请求
 * 依赖包
 * apache http client
 * cglib
 * <p>
 * alibaba json
 * spring beanmap
 * apache common utils
 *
 * @author guanxingya[OF3449]
 * company qianmi.com
 * Date 2019-12-18
 */
public class RestClient {

    /**
     * 代理
     */
    private static class DynamicProxy implements InvocationHandler, MethodInterceptor {
        /**
         * 请求地址
         */
        private String baseUrl;

        private DynamicProxy(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass().equals(Object.class)) {
                return method.invoke(proxy, args);
            }
            // 默认取方法名作为路径
            String apiPath = method.getName();
            // method 参数数组
            Parameter[] parameters = method.getParameters();
            // 默认非form表单请求方式
            boolean isFormParam = false;
            // 默认GET请求
            HttpMethod httpMethod = HttpMethod.GET;
            RestMethod restMethod = method.getDeclaredAnnotation(RestMethod.class);
            if (restMethod != null) {
                httpMethod = restMethod.method();
                if (StringUtils.isNotBlank(restMethod.url())) {
                    apiPath = restMethod.url();
                }
                isFormParam = restMethod.isFormParam();
                if (restMethod.isParamExecute()) {
                    // 替换url中的占位符{0} {1} {2}
                    apiPath = doParamExexute(apiPath, parameters, args);
                }
            }
            // url 对于GET请求还未拼接 ?...
            String url = String.format("%s/%s", baseUrl, apiPath);
            // 默认超时配置
            RequestConfig config = RequestConfig.custom().setConnectTimeout(10000).setSocketTimeout(10000).setConnectionRequestTimeout(10000).build();
            try (CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(config).build()) {
                if (HttpMethod.GET.equals(httpMethod)) {
                    return doGet(method, args, parameters, url, client);
                } else if (HttpMethod.POST.equals(httpMethod)) {
                    return doPost(method, args, parameters, isFormParam, url, client);
                }
                throw new IllegalArgumentException(MessageFormat.format("不支持的请求格式{0}", httpMethod.name()));
            }
        }

        /**
         * post 请求
         *
         * @param method
         * @param args
         * @param parameters
         * @param isFormParam
         * @param url
         * @param client
         * @return
         * @throws IOException
         */
        private Object doPost(Method method, Object[] args, Parameter[] parameters, boolean isFormParam, String url, HttpClient client) throws IOException {
            // POST 请求
            HttpPost httpPost = new HttpPost(url);
            String jsonBody;
            if (isFormParam) {
                // form 表单请求
                httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
                Map<Object, Object> params = new HashMap<>();
                for (int i = 0; i < parameters.length; i++) {
                    Parameter p = parameters[i];
                    RestParam restParam = p.getDeclaredAnnotation(RestParam.class);
                    if (restParam == null || StringUtils.isBlank(restParam.name())) {
                        throw new IllegalArgumentException("参数注解RestParam.name不能为空!");
                    }
                    // 过滤掉ParamExecute 参数
                    if (restParam.isParamExecute()) {
                        continue;
                    }
                    Object arg = args[i];
                    // POST form表单请求 支持 requestBody
                    if (restParam.requestBody()) {
                        params.putAll(BeanMap.create(arg));
                        continue;
                    }
                    params.put(restParam.name(), arg2String(arg));
                }
                jsonBody = JSON.toJSONString(params);
            } else {
                // json请求
                httpPost.addHeader("Content-Type", "application/json");
                if (parameters.length == 1) {
                    jsonBody = getJsonString(args[0], parameters[0]);
                } else {
                    JSONObject jsonEntity = new JSONObject();
                    for (int i = 0; i < parameters.length; i++) {
                        Parameter p = parameters[i];
                        RestParam restParam = p.getDeclaredAnnotation(RestParam.class);
                        if (restParam == null || StringUtils.isBlank(restParam.name())) {
                            throw new IllegalArgumentException("参数注解RestParam.name不能为空!");
                        }
                        jsonEntity.put(restParam.name(), args[i]);
                    }
                    jsonBody = jsonEntity.toString();
                }
            }
            httpPost.setEntity(new StringEntity(jsonBody, Charset.defaultCharset()));
            HttpResponse httpResponse = client.execute(httpPost);
            return resolveResponse(method, url, httpPost, httpResponse);
        }

        /**
         * GET 请求
         *
         * @param method
         * @param args
         * @param parameters
         * @param url
         * @param client
         * @return
         * @throws IOException
         */
        private Object doGet(Method method, Object[] args, Parameter[] parameters, String url, HttpClient client) throws IOException {
            // GET 请求
            List<String> queryParam = new ArrayList<>(parameters.length);
            for (int i = 0; i < parameters.length; i++) {
                Object arg = args[i];
                if (arg == null) {
                    continue;
                }
                Parameter p = parameters[i];
                RestParam restParam = p.getDeclaredAnnotation(RestParam.class);
                if (restParam == null || StringUtils.isBlank(restParam.name())) {
                    throw new IllegalArgumentException("参数注解RestParam.name不能为空!");
                }
                // GET 请求不支持 requestBody
                // 过滤掉ParamExecute 参数
                if (restParam.requestBody() || restParam.isParamExecute()) {
                    continue;
                }
                String value = arg2String(arg);
                queryParam.add(String.format("%s=%s", restParam.name(), URLEncoder.encode(value, Charset.defaultCharset().name())));
            }
            if (!queryParam.isEmpty()) {
                url = url + "?" + queryParam.stream().collect(Collectors.joining("&"));
            }
            HttpGet httpGet = new HttpGet(url);
            httpGet.addHeader("Content-Type", "application/json;charset=UTF-8");
            HttpResponse httpResponse = client.execute(httpGet);
            return resolveResponse(method, url, httpGet, httpResponse);
        }

        /**
         * 解析http请求的response
         * status = 200 成功
         * 其他 -> 失败
         *
         * @param method
         * @param url
         * @param uriRequest
         * @param httpResponse
         * @return
         * @throws IOException
         */
        private Object resolveResponse(Method method, String url, HttpUriRequest uriRequest, HttpResponse httpResponse) throws IOException {
            // 请求返回数据格式
            String returnMsg = null;
            HttpEntity httpEntity = httpResponse.getEntity();
            int status = httpResponse.getStatusLine().getStatusCode();
            // 返回status 2xx 成功
            if (status >= HttpStatus.SC_OK && status < HttpStatus.SC_MULTIPLE_CHOICES) {
                if (httpEntity != null) {
                    returnMsg = EntityUtils.toString(httpEntity, Charset.defaultCharset()); //应答消息默认使用与请求相同的字符编码
                    EntityUtils.consume(httpEntity);
                }
                uriRequest.abort();
                return setResult(method.getAnnotatedReturnType().getType(), returnMsg);
            } else {
                // 非200 状态提示报错信息
                if (httpEntity != null) {
                    returnMsg = EntityUtils.toString(httpEntity, Charset.defaultCharset()); //应答消息默认使用与请求相同的字符编码
                    EntityUtils.consume(httpEntity);
                    uriRequest.abort();
                    throw new RuntimeException("http 请求报错： " + returnMsg + " url： " + url);
                } else {
                    throw new RuntimeException("http 请求报错： 无返回实体！ url： " + url);
                }
            }
        }

        /**
         * object 对象转换为string
         * String JSON.toJSONString（"8"） -> ""8""
         *
         * @param arg
         * @return
         */
        private String arg2String(Object arg) {
            if (arg instanceof String) {
                return String.valueOf(arg);
            } else {
                return JSON.toJSONString(arg);
            }
        }

        /**
         * json 单个单数请求 需要区分 requestBody
         *
         * @param arg
         * @param parameter
         * @return
         */
        private String getJsonString(Object arg, Parameter parameter) {
            RestParam restParam = parameter.getDeclaredAnnotation(RestParam.class);
            if (restParam == null) {
                throw new IllegalArgumentException("参数注解RestParam不能为空!");
            } else {
                if (restParam.requestBody()) {
                    return JSON.toJSONString(arg);
                } else {
                    if (StringUtils.isBlank(restParam.name())) {
                        throw new IllegalArgumentException("参数注解RestParam.name不能为空!");
                    }
                    JSONObject json = new JSONObject();
                    json.put(restParam.name(), JSON.toJSONString(arg));
                    return json.toString();
                }
            }
        }

        /**
         * url {0} {1} 占位符替换
         *
         * @param apiPath
         * @param parameters
         * @param args
         * @return
         */
        private String doParamExexute(String apiPath, Parameter[] parameters, Object[] args) {
            if (parameters == null || parameters.length == 0 || args == null || args.length == 0) {
                return apiPath;
            }
            SortedMap<Integer, String> sortedMap = new TreeMap<>(Integer::compareTo);
            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                if (parameter == null) {
                    continue;
                }
                RestParam restParam = parameter.getDeclaredAnnotation(RestParam.class);
                if (restParam == null) {
                    continue;
                }
                boolean paramExecute = restParam.isParamExecute();
                if (!paramExecute) {
                    continue;
                }
                Object arg = args[i];
                if (arg == null) {
                    continue;
                }
                int executeIndex = restParam.executeIndex();
                if (sortedMap.containsKey(executeIndex)) {
                    throw new IllegalArgumentException(MessageFormat.format("{0}参数注解executeIndex设置存在相同executeIndex", parameter.getName()));
                }
                sortedMap.put(executeIndex, arg2String(arg));
            }
            Collection<String> values = sortedMap.values();
            return MessageFormat.format(apiPath, values.toArray());
        }

        @Override
        public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
            return invoke(o, method, objects);
        }

        /**
         * 数据转换
         *
         * @param <T>
         * @param returnType
         * @param resText
         * @return
         */
        private <T> T setResult(Type returnType, String resText) {
            if (returnType.equals(Void.TYPE)) {
                return null;
            }
            return JSON.parseObject(resText, returnType);
        }
    }

    /**
     * 代理
     *
     * @param restInterface
     * @param baseUrl
     * @param <T>
     * @return
     */
    public static <T> T create(Class<? extends T> restInterface, String baseUrl) {
        return create(restInterface, baseUrl, true);
    }

    /**
     * 代理
     *
     * @param restInterface
     * @param baseUrl
     * @param byCglib
     * @param <T>
     * @return
     */
    public static <T> T create(Class<? extends T> restInterface, String baseUrl, boolean byCglib) {
        DynamicProxy handler = new DynamicProxy(baseUrl);
        return (T) (byCglib ? Enhancer.create(restInterface, handler)
                : Proxy.newProxyInstance(handler.getClass().getClassLoader(), new Class[]{restInterface}, handler));
    }

    public static void main(String[] args) throws NoSuchMethodException {
        PostTest t = RestClient.create(PostTest.class, "http://login2.qianmi2.com:8080");
        JSON json = JSON.parseObject("{\"nickName\": \"\", \"mobile\": \"1234\",\"smsCode\": \"102231\",\"password\": \"MVPqhqe8LL2ErJFZm7TeQxezPh3ySgFR1VaiLWL64bRyLBzZj3XjMiRe/j1oxy5rj8RMVMmGNtdSTaWGv2eu2UWKBP+lLnqrI/NXaRtJqILuLQP2sYDubIuITPXJSEl4yqaDEvY0SW/7jjY3zFUF9UX3GTuqQ2bGfb4yij4m5aY=\",\"source\": \"\",\"actionSource\": \"2\",\"appVersion\": \"1.6.0\",\"protocolIdList\": [15,17]}");
        System.out.println(t.send(json));
        System.out.println(t.adduser("15210203040", "528333", "000205d08e4cf5fd0aec3673e64f27e420153083c510775c5f9d3ba5cfa9f7b3"));
        t.send("send", "m", "", "1234", "102231", "MVPqhqe8LL2ErJFZm7TeQxezPh3ySgFR1VaiLWL64bRyLBzZj3XjMiRe", "", Arrays.asList(15, 17));
    }

    private interface PostTest {
        @RestMethod(url = "/reg/m/send", method = HttpMethod.POST)
        Map<String, Object> send(@RestParam(requestBody = true) JSON json);

        @RestMethod(url = "/reg/{1}/{0}", method = HttpMethod.POST, isParamExecute = true)
        void send(@RestParam(name = "send", isParamExecute = true) String send,
                  @RestParam(name = "m", isParamExecute = true, executeIndex = 1) String m,
                  @RestParam(name = "nickName") String nickName,
                  @RestParam(name = "mobile") String mobile,
                  @RestParam(name = "smsCode") String smsCode,
                  @RestParam(name = "password") String password,
                  @RestParam(name = "source") String source,
                  @RestParam(name = "protocolIdList") List<Integer> protocolIdList);

        @RestMethod(url = "/reg/adduser", method = HttpMethod.POST, isFormParam = true)
        Object adduser(
                @RestParam(name = "mobile") String mobile,
                @RestParam(name = "smsCode") String smsCode,
                @RestParam(name = "password") String password);
    }
}
