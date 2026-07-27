-- 予定（Event）テーブル
CREATE TABLE events (
    id         UUID PRIMARY KEY,
    space_id   UUID          NOT NULL,
    title      VARCHAR(255)  NOT NULL,
    start_at   TIMESTAMPTZ   NOT NULL,
    end_at     TIMESTAMPTZ   NOT NULL,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    -- ドメインルール「終了は開始より後」を DB 側でも保証（多重防御）
    CONSTRAINT chk_events_time CHECK (end_at > start_at)
);

CREATE INDEX idx_events_space_id ON events (space_id);
