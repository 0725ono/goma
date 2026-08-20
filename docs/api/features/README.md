# ☕ API 機能設計 (features)

個々の機能ごとの設計ドキュメントを置く。1 機能 = 1 ファイル（または 1 サブフォルダ）を基本とする。

全体に効く横断的な方針（レイヤ構成・エラー契約など）は
[../architecture/](../architecture/) にあり、ここには**その方針に沿った個別機能の設計**を書く。

## 命名の目安

- `events.md` — 予定（Event）
- `tasks.md` — タスク（Task）
- `spaces.md` — スペースと所属（users / spaces / space_members）
- `auth.md` — Firebase JWT の検証とスペース解決

> まだ機能設計ドキュメントは無い。単純な CRUD で説明が要らないうちは書かず、
> 設計判断が必要になった機能から追加していく。
