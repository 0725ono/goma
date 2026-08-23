package com.familyschedule.api.shared.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

/**
 * mobile 側の ErrorCode の写し（errorCodes.ts）が ErrorCode enum と一致することを検証する契約テスト。
 *
 * エラーコードは API とクライアントの契約であり、両側が別言語（Java / TypeScript）である以上、
 * 定義の二重化は避けられない。このテストは「黙って乖離する」ことを防ぐ見張り役。
 * 落ちたら「enum に足したのに TS に足していない」（またはその逆）。
 *
 * モノレポ前提で相対パス参照している。api を別リポジトリに切り出して CI を分けたら
 * このテストは成立しない。そのときは OpenAPI からの型生成へ移行する
 * （docs/api/architecture/error-contract.md の判断基準を参照）。
 */
class ErrorCodeContractTest {

    private static final Path TS_MIRROR = Path.of("../mobile/src/lib/api/errorCodes.ts");

    @Test
    void tsMirrorMatchesEnum() throws IOException {
        String ts = Files.readString(TS_MIRROR);
        Set<String> tsCodes = Pattern.compile("\"([A-Z][A-Z_]*)\"").matcher(ts).results()
                .map(m -> m.group(1))
                .collect(Collectors.toSet());
        Set<String> javaCodes = Arrays.stream(ErrorCode.values())
                .map(Enum::name)
                .collect(Collectors.toSet());
        assertEquals(javaCodes, tsCodes,
                "ErrorCode enum と mobile/src/lib/api/errorCodes.ts が一致していない。両方を更新すること");
    }
}
