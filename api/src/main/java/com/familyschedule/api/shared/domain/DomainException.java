package com.familyschedule.api.shared.domain;

/**
 * ドメインルール違反を表す例外の基底クラス。
 *
 * - Java 標準の RuntimeException を継承する（Spring には依存しない）。
 *   domain 層は最内層であり、フレームワークを知ってはいけないため。
 * - unchecked（非検査）にする理由: throws 宣言の伝播で全レイヤを汚染しないこと、
 *   および @Transactional の既定ロールバック対象が RuntimeException であるため。
 * - code はフロントエンドが分岐に使う機械可読な識別子（API 契約の一部、ErrorCode を参照）。
 *   メッセージ文言は変わり得るが、code は互換性を維持する。
 */
public abstract class DomainException extends RuntimeException {

    private final ErrorCode code;

    protected DomainException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode getCode() {
        return code;
    }
}
