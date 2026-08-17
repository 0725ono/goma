/**
 * 予定（Event）のドメイン型。バックエンドの EventResponse と対応する。
 * 日時は API の返す ISO-8601 文字列（UTC）。表示時にローカルタイムへ変換する。
 */
export type Event = {
  id: string;
  spaceId: string;
  title: string;
  startAt: string;
  endAt: string;
};
