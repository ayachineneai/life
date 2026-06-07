DROP TABLE IF EXISTS meal;
CREATE TABLE meal (
    id              UUID PRIMARY KEY,
    title           VARCHAR(128),
    meal_type       VARCHAR(16),
    content         TEXT,
    remark          TEXT,
    calories        INTEGER,
    protein         INTEGER,
    fat             INTEGER,
    carbs           INTEGER,
    occurred_date   DATE,
    occurred_time   TIMESTAMP,
    create_time     TIMESTAMP NOT NULL,
    update_time     TIMESTAMP
);
CREATE UNIQUE INDEX idx_meal_occurred_date_meal_type
    ON meal (occurred_date, meal_type);

DROP TABLE IF EXISTS conversation;
CREATE TABLE conversation (
    id                     UUID PRIMARY KEY,
    openai_conversation_id VARCHAR(128) NOT NULL,
    title                  VARCHAR(128),
    create_time            TIMESTAMP NOT NULL,
    last_active_time       TIMESTAMP NOT NULL
);

DROP TABLE IF EXISTS conversation_turn;
CREATE TABLE conversation_turn (
    id                 UUID PRIMARY KEY,
    conversation_id    UUID,
    user_input         TEXT,
    model_output       TEXT,
    openai_response_id VARCHAR(128),
    model              VARCHAR(128),
    create_time        TIMESTAMP NOT NULL
);
