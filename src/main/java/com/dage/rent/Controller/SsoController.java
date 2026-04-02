package com.dage.rent.Controller;

import com.dage.rent.config.CustomUserDetails;
import com.dage.rent.Service.SsoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/sso")
public class SsoController {

    private static final Logger log = LoggerFactory.getLogger(SsoController.class);

    private final SsoService ssoService;
    private final UserDetailsService userDetailsService;

    public SsoController(SsoService ssoService, UserDetailsService userDetailsService) {
        this.ssoService = ssoService;
        this.userDetailsService = userDetailsService;
    }

    /** authorize·token 양쪽에서 동일한 redirect_uri를 쓰기 위해 콜백 URL을 한 방식으로만 생성 */
    private static String buildCallbackUrl(HttpServletRequest request) {
        int port = request.getServerPort();
        return request.getScheme() + "://" + request.getServerName()
                + ((port == 80 || port == 443) ? "" : ":" + port)
                + request.getContextPath() + "/sso/callback";
    }

    /** SSO 로그인 시작 - SSO 서버 /authorize로 리다이렉트 */
    @GetMapping("/login")
    public String ssoLogin(HttpServletRequest request) {
        if (!ssoService.isEnabled()) {
            return "redirect:/login";
        }
        return "redirect:" + ssoService.buildAuthorizeUrl(buildCallbackUrl(request));
    }

    /** SSO 콜백 - authorization code를 받아 토큰 교환 후 세션 생성 */
    @GetMapping("/callback")
    public String ssoCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "error", required = false) String error,
            HttpServletRequest request) {

        if (error != null) {
            log.warn("SSO authentication error: {}", error);
            return "redirect:/login?error";
        }

        if (code == null || code.isBlank()) {
            log.warn("SSO callback without authorization code");
            return "redirect:/login?error";
        }

        // redirect_uri는 /sso/login 에서 쓴 것과 동일한 규칙으로 생성 (불일치 시 code 무효화 방지)
        String callbackUrl = buildCallbackUrl(request);

        try {
            SsoService.SsoTokenResponse tokenResponse = ssoService.exchangeToken(code, callbackUrl);
            SsoService.SsoUserInfo userInfo = ssoService.getUserInfo(tokenResponse.getAccessToken());
            log.info("SSO login - user: {} ({})", userInfo.getUsername(), userInfo.getName());

            UserDetails userDetails = userDetailsService.loadUserByUsername(userInfo.getUsername());
            // 기존 폼 로그인과 동일하게 principal을 LoginDTO로 설정
            Object principal = userDetails instanceof CustomUserDetails
                    ? ((CustomUserDetails) userDetails).getLoginDTO()
                    : userDetails;

            var authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            HttpSession session = request.getSession(true);
            session.setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext());
            session.setAttribute("SSO_ACCESS_TOKEN", tokenResponse.getAccessToken());

            return "redirect:/main";
        } catch (Exception e) {
            log.error("SSO authentication failed", e);
            return "redirect:/login?error";
        }
    }
}
