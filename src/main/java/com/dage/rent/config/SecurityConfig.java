package com.dage.rent.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.web.filter.CharacterEncodingFilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final CustomAuthenticationProvider customAuthenticationProvider;

    @Autowired
    public SecurityConfig(CustomAuthenticationProvider customAuthenticationProvider) {
        this.customAuthenticationProvider = customAuthenticationProvider;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(customAuthenticationProvider);
    }

    @Override
    public void configure(WebSecurity web) {
        web.httpFirewall(new DefaultHttpFirewall());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        CharacterEncodingFilter filter = new CharacterEncodingFilter("UTF-8", true) {
            @Override
            protected boolean shouldNotFilter(HttpServletRequest request) {
                String contentType = request.getContentType();
                return contentType != null && contentType.toLowerCase().startsWith("multipart/");
            }
        };
        
        http
            .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
            .csrf().disable()
            .authorizeRequests()
                .antMatchers("/", "/view", "/login", "/css/**", "/js/**", "/images/**").permitAll()
                .anyRequest().authenticated()
            .and()
            .formLogin()
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("userId")
                .passwordParameter("password")
                .defaultSuccessUrl("/main")
                .failureUrl("/login?error=true")
                .permitAll()
            .and()
            .logout()
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login")
                .permitAll()
            .and()
            .exceptionHandling()
                .authenticationEntryPoint((request, response, authException) -> {
                    if (isApiOrAjaxRequest(request)) {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }
                    response.sendRedirect(buildLoginRedirectUrl(request));
                });
    }

    private static boolean isApiOrAjaxRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri != null && uri.startsWith("/api/")) {
            return true;
        }
        if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            return true;
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }

    /** 미인증 페이지 접근 시 로그인으로만 보냄. error=true는 실제 로그인 실패에만 사용. */
    private static String buildLoginRedirectUrl(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        if (contextPath == null || "/".equals(contextPath)) {
            contextPath = "";
        }
        return contextPath + "/login";
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}