package com.mydubbo.gateway.config;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * http请求模板
 *
 * @author weiwei
 * @since 2020/4/11 16:47
 */
@Configuration
public class RestTemplateConfig {

    //    @Bean
//    public RestTemplate restTemplate(ClientHttpRequestFactory factory) {
//        RestTemplate restTemplate = new RestTemplate(factory);
//        restTemplate.getMessageConverters().set(1, new StringHttpMessageConverter(Charset.forName("UTF-8")));//设置编码集UTF-8
//        restTemplate.setErrorHandler(new CustomResponseErrorHandler());
//        return restTemplate;
//    }
//
//    @Bean
//    public ClientHttpRequestFactory simpleClientHttpRequestFactory() {
//        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
//        factory.setConnectTimeout(10000);//ms
//        factory.setReadTimeout(10000);//ms
//        return factory;
//    }
    @Bean
    public ClientHttpRequestFactory httpRequestFactory() {
        return new HttpComponentsClientHttpRequestFactory(httpClient());
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate(httpRequestFactory());
    }

    @Bean
    public HttpClient httpClient() {
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", SSLConnectionSocketFactory.getSocketFactory()).build();
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(registry);
        connectionManager.setMaxTotal(3000);
        connectionManager.setDefaultMaxPerRoute(1000);
        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(8000).setConnectTimeout(8000)
                .setConnectionRequestTimeout(8000).build();
        return HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).setConnectionManager(connectionManager)
                .build();
    }
}
