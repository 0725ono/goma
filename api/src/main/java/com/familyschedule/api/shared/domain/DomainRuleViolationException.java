package com.familyschedule.api.shared.domain;

/**
 * ユーザー入力がドメインルールに違反したことを表す例外。
 *
 * HTTP ステータスは code（ErrorCode）が持つため、この例外自体は
 * 「ユーザー入力起因のルール違反である」という分類だけを表す。
 * リソース不在や競合など別の分類が必要になったら、
 * 同様に DomainException のサブクラスとして追加する。
 */
public class DomainRuleViolationException extends DomainException {

    public DomainRuleViolationException(ErrorCode code, String message) {
        super(code, message);
    }
}
