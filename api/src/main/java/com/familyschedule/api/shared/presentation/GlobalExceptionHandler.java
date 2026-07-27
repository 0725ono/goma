package com.familyschedule.api.shared.presentation;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.familyschedule.api.shared.domain.DomainRuleViolationException;

/**
 * 例外 → HTTP レスポンスへの「翻訳の境界」。
 *
 * すべてのエラー応答を RFC 9457 (Problem Details) 形式に統一する。
 * - ResponseEntityExceptionHandler を継承することで、Spring MVC が投げる
 *   フレームワーク例外（Bean Validation 違反・パース不能 JSON・型変換失敗など）は
 *   基底クラスが ProblemDetail 形式で処理する（個別に書き直す必要はない）。
 * - ここに書くのは (1) 自作ドメイン例外の変換、(2) 検証エラーへの拡張メンバー追加、
 *   (3) 最後の砦（想定外の例外を、内部情報を漏らさずに 500 へ）の3つだけ。
 *
 * 拡張メンバー（RFC 9457 の extension members）:
 * - code:   機械可読なエラー識別子。フロントエンドはこれで分岐する（文言に依存しない）。
 * - errors: フィールド別の検証エラー一覧（検証失敗時のみ）。
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** ドメインルール違反 → 400。code を拡張メンバーとして載せる。 */
    @ExceptionHandler(DomainRuleViolationException.class)
    ProblemDetail handleDomainRuleViolation(DomainRuleViolationException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setProperty("code", ex.getCode());
        return pd;
    }

    /** Bean Validation 違反（@NotBlank / @Size 等）に code と errors を追加する。 */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ProblemDetail pd = ex.getBody();
        pd.setProperty("code", "VALIDATION_ERROR");
        pd.setProperty("errors", ex.getFieldErrors().stream()
                .map(f -> Map.of(
                        "field", f.getField(),
                        "message", String.valueOf(f.getDefaultMessage())))
                .toList());
        return handleExceptionInternal(ex, pd, headers, status, request);
    }

    /**
     * 最後の砦: 想定外の例外はすべて 500。
     * 詳細はサーバログにのみ出力し、クライアントには一般的な文言だけを返す
     * （内部実装・スタック情報を応答に漏らさない）。
     */
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        pd.setProperty("code", "INTERNAL_ERROR");
        return pd;
    }
}
