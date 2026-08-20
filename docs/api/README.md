# ☕ API ドキュメント

Java (Spring Boot) 製バックエンド API の設計ドキュメント。
システム全体の設計は [../architecture.md](../architecture.md)、開発環境の方針は
[../development-environment.md](../development-environment.md) を参照。

## 構成

| 区分 | 場所 | 内容 |
|---|---|---|
| **全体設計** | [architecture/](./architecture/) | API 全体に効く横断的な設計方針（レイヤ構成・エラー契約など） |
| **機能設計** | [features/](./features/) | 個々の機能ごとの設計（機能単位で追加していく） |

### 全体設計 (architecture/)

- [backend-architecture.md](./architecture/backend-architecture.md) — レイヤ構成、パッケージ規約、依存の向き、機能追加の手順
- [error-contract.md](./architecture/error-contract.md) — エラー応答の契約（RFC 9457）、エラーコードの管理と同期、クライアントの扱い方

### 機能設計 (features/)

機能ごとの設計はここに置く。詳細は [features/README.md](./features/README.md) を参照。

---

## このドキュメント群の方針

**コードを読めば分かることは書かない。コードを読んでも分からないことを書く。**

ドキュメントが腐るのは、同じ事実をコードとドキュメントの二重に持つためである。
二重に持たなければ腐らない。

| 書かない（コードが真実） | 書く（コードに現れない） |
|---|---|
| フィールド名・型・必須かどうか | なぜその設計を選んだか |
| ライブラリのバージョン番号 | 層をまたぐときの約束ごと |
| メソッドの引数と戻り値 | 判断に迷ったときの指針 |

バージョン番号のような「どこかに正解があるもの」は、そのファイル（[pom.xml](../../api/pom.xml) など）へのリンクで示す。

決まっていないことは「決まっていない」と書く。それらしい記述を先回りで書くと、
実装時にほぼ確実に嘘になる。
