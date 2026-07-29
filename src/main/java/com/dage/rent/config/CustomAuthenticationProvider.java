package com.dage.rent.config;

import com.dage.rent.DTO.LoginDTO;
import com.dage.rent.Service.RentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(CustomAuthenticationProvider.class);

    private final RentService rentService;

    @Autowired
    public CustomAuthenticationProvider(RentService rentService) {
        this.rentService = rentService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String userId = authentication.getName();
        String password = authentication.getCredentials().toString();


        // 입력값 인코딩 처리
        if (StringUtils.hasText(userId)) {
            userId = new String(userId.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        }
        if (StringUtils.hasText(password)) {
            password = new String(password.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        }

        try {
            LoginDTO loginUser = rentService.login(userId);
            
            if (loginUser == null) {
                log.warn("로그인 실패: 사용자를 찾을 수 없음");
                throw new BadCredentialsException("아이디 또는 비밀번호가 일치하지 않습니다.");
            }

            if (!"T".equalsIgnoreCase(loginUser.getUseFlag())) {
                log.warn("로그인 실패: 사용할 수 없는 계정");
                throw new BadCredentialsException("사용할 수 없는 계정입니다.");
            }

            if (!loginUser.getUserPassword().equals(password)) {
                log.warn("로그인 실패: 비밀번호 불일치");
                throw new BadCredentialsException("아이디 또는 비밀번호가 일치하지 않습니다.");
            }

            return new UsernamePasswordAuthenticationToken(
                loginUser,
                password,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            log.warn("로그인 처리 중 오류: {}", e.getMessage());
            throw new BadCredentialsException("로그인 처리 중 오류가 발생했습니다.");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
