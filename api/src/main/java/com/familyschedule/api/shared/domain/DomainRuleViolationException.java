package com.familyschedule.api.shared.domain;

/**
 * ユーザー入力がドメインルールに違反したことを表す例外（HTTP 400 に対応）。
 *
 * クラス＝HTTP ステータスの選択、code＝個別エラーの識別、という2軸で運用する。
 * 404 相当（ResourceNotFound）や 409 相当（Conflict）が必要になったら、
 * 同様に DomainException のサブクラスとして追加する。
 */
public class DomainRuleViolationException extends DomainException {

    public DomainRuleViolationException(String code, String message) {
        super(code, message);
    }
}
