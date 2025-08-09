package com.example.Kcsj.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * CORS跨域配置
 */
@Configuration
public class CorsConfig {
    
    @Bean
    public CorsFilter corsFilter() {
        // 创建CORS配置对象
        CorsConfiguration config = new CorsConfiguration();
        // 允许跨域的域名，* 表示允许所有域名
        config.addAllowedOrigin("*");
        // 允许携带cookie (注意：当使用addAllowedOrigin("*")时，不能同时使用setAllowCredentials(true))
        // config.setAllowCredentials(true); // 由于使用了通配符*，不能启用凭据
        // 允许所有请求头
        config.addAllowedHeader("*");
        // 允许所有请求方法
        config.addAllowedMethod("*");
        // 设置最大有效期
        config.setMaxAge(3600L);
        
        // 创建URL配置源
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有URL应用CORS配置
        source.registerCorsConfiguration("/**", config);
        
        // 返回CORS过滤器实例
        return new CorsFilter(source);
    }
}
