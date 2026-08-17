package com.familyschedule.api.shared.interfaces;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS 設定。ブラウザで動くクライアント（Expo web 等）を正式にサポートするための恒久設定。
 *
 * - 許可オリジンは application.properties の app.cors.allowed-origins（明示リスト）から読む。
 *   ワイルドカード(*)は使わない（将来 Authorization: Bearer を送る前提では特に不適切なため）。
 * - ネイティブアプリ（iOS/Android）には同一オリジンポリシーが無いため、この設定の影響を受けない。
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final List<String> allowedOrigins;

    public CorsConfig(@Value("${app.cors.allowed-origins:}") List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (allowedOrigins.isEmpty()) {
            return; // 許可リストが空なら CORS を一切開放しない
        }
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "Authorization")
                .maxAge(3600); // プリフライト結果のキャッシュ（秒）。毎リクエストの OPTIONS を抑制
    }
}
