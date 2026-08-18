package com.dage.rent.config;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

/**
 * 업로드 중에는 connectionTimeout(기본 요청)과 별도의 긴 타임아웃을 쓴다.
 * disableUploadTimeout=true(톰캣 기본)이면 업로드에도 5~120초 제한이 그대로 적용되어
 * 브라우저에 net::ERR_FAILED 로 보인다.
 */
@Component
public class TomcatUploadTimeoutConfig implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

    private static final String UPLOAD_TIMEOUT_MS = "600000";

    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        factory.addConnectorCustomizers((Connector connector) -> {
            connector.setProperty("disableUploadTimeout", "false");
            connector.setProperty("connectionUploadTimeout", UPLOAD_TIMEOUT_MS);
        });
    }
}
