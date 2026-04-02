package com.dage.rent.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class SsoService {

    private static final Logger log = LoggerFactory.getLogger(SsoService.class);
    private static final MediaType FORM = MediaType.get("application/x-www-form-urlencoded");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final OkHttpClient httpClient = new OkHttpClient.Builder().build();

    @Value("${sso.enabled:true}")
    private boolean ssoEnabled;

    @Value("${sso.server-url}")
    private String serverUrl;

    @Value("${sso.client-id}")
    private String clientId;

    @Value("${sso.client-secret}")
    private String clientSecret;

    @Value("${sso.redirect-uri}")
    private String redirectUri;

    public boolean isEnabled() {
        return ssoEnabled;
    }

    /** SSO 인증 페이지 URL 생성 (설정된 redirect_uri 사용) */
    public String buildAuthorizeUrl() {
        return buildAuthorizeUrl(redirectUri);
    }

    /** SSO 인증 페이지 URL 생성 - redirectUriOverride 사용 (프록시/도커 환경에서 콜백 URL 일치용) */
    public String buildAuthorizeUrl(String redirectUriOverride) {
        String state = UUID.randomUUID().toString();
        String uri = redirectUriOverride != null ? redirectUriOverride : redirectUri;
        return serverUrl
                + "/authorize?response_type=code"
                + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(uri, StandardCharsets.UTF_8)
                + "&state=" + state;
    }

    /** Authorization code로 access_token 교환 (redirect_uri는 콜백 요청과 동일한 URL 사용 권장) */
    public SsoTokenResponse exchangeToken(String code) throws IOException {
        return exchangeToken(code, redirectUri);
    }

    /** Authorization code로 access_token 교환 - 콜백 시 실제 요청 URL을 redirectUri로 전달하면 400 방지 */
    public SsoTokenResponse exchangeToken(String code, String redirectUriToUse) throws IOException {
        String body = "grant_type=authorization_code"
                + "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(redirectUriToUse, StandardCharsets.UTF_8)
                + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8);

        Request request = new Request.Builder()
                .url(serverUrl + "/token")
                .post(RequestBody.create(body, FORM))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                log.error("SSO token exchange failed: {} body={}", response.code(), responseBody);
                throw new IOException("SSO token exchange failed: HTTP " + response.code() + " " + responseBody);
            }

            JsonNode json = objectMapper.readTree(responseBody);
            SsoTokenResponse tokenResponse = new SsoTokenResponse();
            tokenResponse.setAccessToken(json.path("access_token").asText());
            tokenResponse.setRefreshToken(json.path("refresh_token").asText());
            tokenResponse.setTokenType(json.path("token_type").asText());
            return tokenResponse;
        }
    }

    /** access_token으로 사용자 정보 조회 */
    public SsoUserInfo getUserInfo(String accessToken) throws IOException {
        Request request = new Request.Builder()
                .url(serverUrl + "/userinfo")
                .header("Authorization", "Bearer " + accessToken)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                log.error("SSO userinfo failed: {}", response.code());
                throw new IOException("SSO userinfo failed: HTTP " + response.code());
            }

            JsonNode json = objectMapper.readTree(response.body().string());
            SsoUserInfo userInfo = new SsoUserInfo();
            userInfo.setSub(json.path("sub").asText());
            userInfo.setUsername(json.path("username").asText());
            userInfo.setName(json.path("name").asText());
            userInfo.setEmpno(json.path("empno").asText());
            return userInfo;
        }
    }

    /** SSO 서버에 로그아웃 요청 */
    public void logout(String accessToken) {
        if (accessToken == null) return;

        Request request = new Request.Builder()
                .url(serverUrl + "/logout")
                .header("Authorization", "Bearer " + accessToken)
                .post(RequestBody.create("", FORM))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            log.debug("SSO logout response: {}", response.code());
        } catch (IOException e) {
            log.warn("SSO logout failed: {}", e.getMessage());
        }
    }

    @Data
    public static class SsoTokenResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
    }

    @Data
    public static class SsoUserInfo {
        private String sub;      // USER_NO (통합정보시스템)
        private String username; // USER_ID
        private String name;     // 사용자명
        private String empno;    // 사번
    }
}
