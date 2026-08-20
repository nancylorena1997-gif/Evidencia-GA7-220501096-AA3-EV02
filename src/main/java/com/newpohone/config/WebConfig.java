package com.newpohone.config;

import com.newpohone.web.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/",
                        "/catalog",
                        "/catalog/**",
                        "/cart",
                        "/checkout",
                        "/checkout/**",
                        "/seguimiento",
                        "/favoritos",
                        "/favoritos/**",
                        "/login",
                        "/register",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/error",
                        "/favicon.ico");
    }
}
