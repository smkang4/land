package com.dage.rent.config;

import com.dage.rent.Service.SsoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.CharacterEncodingFilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final CustomAuthenticationProvider customAuthenticationProvider;
    private final SsoService ssoService;

    @Autowired
    public SecurityConfig(CustomAuthenticationProvider customAuthenticationProvider, SsoService ssoService) {
        this.customAuthenticationProvider = customAuthenticationProvider;
        this.ssoService = ssoService;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(customAuthenticationProvider);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        filter.setEncoding("UTF-8");
        filter.setForceEncoding(true);
        
        http
            .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
            .csrf().disable()
            .authorizeRequests()
                .antMatchers("/sso/**").permitAll()
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
                .addLogoutHandler((HttpServletRequest req, HttpServletResponse res, org.springframework.security.core.Authentication a) -> {
                    HttpSession session = req.getSession(false);
                    if (session != null) {
                        String ssoToken = (String) session.getAttribute("SSO_ACCESS_TOKEN");
                        if (ssoToken != null && ssoService.isEnabled()) {
                            ssoService.logout(ssoToken);
                        }
                    }
                })
                .logoutSuccessUrl("/login")
                .permitAll()
            .and()
            .exceptionHandling()
                .authenticationEntryPoint((request, response, authException) -> {
                    System.out.println("Authentication failed: " + authException.getMessage());
                    response.sendRedirect("/login?error=true");
                });
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}