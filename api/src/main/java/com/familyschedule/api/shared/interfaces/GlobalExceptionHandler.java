package com.familyschedule.api.shared.interfaces;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.familyschedule.api.shared.domain.DomainRuleViolationException;
import com.familyschedule.api.shared.domain.ErrorCode;

/**
 * 例外 → HTTP レスポンスへの「翻訳の境界」。エラー処理の規則は
 * docs/api/architecture/backend-architecture.md を参照。
 *
 * すべてのエラー応答を RFC 9457 (Problem Details) 形式に統一し、
 * すべてのエラー応答に code 拡張メンバー（ErrorCode）を必ず付与する。
 *
 * - フレームワーク例外（壊れた JSON・未知のパス・未対応メソッドなど）は
 *   基底クラス ResponseEntityExceptionHandler が処理するが、その全経路が
 *   handleExceptionInternal を通るため、そこで code を一括付与する。
 * - ここに書くのは (1) 自作ドメイン例外の変換、(2) 検証エラーへの errors 追加、
 *   (3) 最後の砦（想定外の例外を、内部情報を漏らさずに 500 へ）、
 *   (4) フレームワーク例外への code 付与、の4つだけ。
 *
 * 拡張メンバー（RFC 9457 の extension members）:
 * - code:   機械可読なエラー識別子（ErrorCode.name()）。フロントエンドはこれで分岐する。
 * - errors: フィールド別の検証エラー一覧（検証失敗時のみ）。
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * フレームワーク例外 → ErrorCode の明示マッピング。
     * 「クライアントが分岐する価値がある」と判断したものだけを載せる。
     * 載っていない例外は 4xx なら REQUEST_REJECTED、5xx なら INTERNAL_ERROR に落ちる。
     * isInstance で照合するため、サブクラス（例: MethodArgumentTypeMismatchException）も拾う。
     */
    private static final List<Map.Entry<Class<? extends Exception>, ErrorCode>> FRAMEWORK_CODES = List.of(
            Map.entry(HttpMessageNotReadableException.class, ErrorCode.MALFORMED_REQUEST),
            Map.entry(TypeMismatchException.class, ErrorCode.MALFORMED_REQUEST),
            Map.entry(NoResourceFoundException.class, ErrorCode.RESOURCE_NOT_FOUND),
            Map.entry(NoHandlerFoundException.class, ErrorCode.RESOURCE_NOT_FOUND),
            Map.entry(HttpRequestMethodNotSupportedException.class, ErrorCode.METHOD_NOT_ALLOWED));

    /** ドメインルール違反。ステータスは code が持つ値を使う。 */
    @ExceptionHandler(DomainRuleViolationException.class)
    ProblemDetail handleDomainRuleViolation(DomainRuleViolationException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatusCode.valueOf(ex.getCode().status()), ex.getMessage());
        pd.setProperty("code", ex.getCode().name());
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
        pd.setProperty("code", ErrorCode.VALIDATION_ERROR.name());
        pd.setProperty("errors", ex.getFieldErrors().stream()
                .map(f -> Map.of(
                        "field", f.getField(),
                        "message", String.valueOf(f.getDefaultMessage())))
                .toList());
        return handleExceptionInternal(ex, pd, headers, status, request);
    }

    /**
     * 基底クラスの全ハンドラが最終的に通る単一の出口。
     * code が未設定なら、ここでフレームワーク例外由来の code を付与する。
     * （handleMethodArgumentNotValid のように既に code を設定済みの経路は上書きしない）
     * ステータスは Spring が決めた値を維持する。
     *
     * 注意: ErrorResponse 実装の例外（NoResourceFound 等）は body = null で届き、
     * ProblemDetail は基底クラスの内部で後から生成される。そのままでは code を
     * 付与できないため、ここで先に実体化してから渡す（基底クラスは body があれば
     * それをそのまま使う）。
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {
        if (body == null && ex instanceof ErrorResponse errorResponse) {
            body = errorResponse.updateAndGetBody(getMessageSource(), LocaleContextHolder.getLocale());
        }
        if (body instanceof ProblemDetail pd && !hasCode(pd)) {
            pd.setProperty("code", frameworkCodeFor(ex, statusCode).name());
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    /**
     * 最後の砦: 想定外の例外はすべて 500。
     * 詳細はサーバログにのみ出力し、クライアントには一般的な文言だけを返す
     * （内部実装・スタック情報を応答に漏らさない）。
     * どの層も catch しない例外はここに伝播してくるため、ログはこの1回だけ確実に出る。
     */
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        pd.setProperty("code", ErrorCode.INTERNAL_ERROR.name());
        return pd;
    }

    private static boolean hasCode(ProblemDetail pd) {
        return pd.getProperties() != null && pd.getProperties().containsKey("code");
    }

    private static ErrorCode frameworkCodeFor(Exception ex, HttpStatusCode status) {
        for (var entry : FRAMEWORK_CODES) {
            if (entry.getKey().isInstance(ex)) {
                return entry.getValue();
            }
        }
        return status.is5xxServerError() ? ErrorCode.INTERNAL_ERROR : ErrorCode.REQUEST_REJECTED;
    }
}
