package com.hmall.api.conf;

import com.hmall.api.fallback.ItemFeignFallbackFactory;
import com.hmall.common.utils.UserContext;
import feign.Logger;
import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class OpenFeignConf {

    @Bean
    public Logger.Level feignLogLevel(){
        return Logger.Level.FULL;
    }

    @Bean
    public RequestInterceptor userInfoRequestInterceptor(){
        return template -> {
            Long user = UserContext.getUser();
            if(user!= null) {
                template.header("user-info", user.toString());
            }
        };
    }

    @Bean
    public ItemFeignFallbackFactory itemFeignFallbackFactory(){
        return new ItemFeignFallbackFactory();
    }
}
