-- 饮食记录
CREATE TABLE meal (
    id              UUID PRIMARY KEY,   -- 记录ID，UUIDv7
    title           VARCHAR(128),       -- 标题
    meal_type       VARCHAR(16),        -- 餐次：BREAKFAST/LUNCH/DINNER/SNACK
    content         TEXT,               -- 饮食内容文本
    remark          TEXT,               -- 备注
    calories        INTEGER,            -- 总热量，单位：kcal
    protein         INTEGER,            -- 蛋白质，单位：g
    fat             INTEGER,            -- 脂肪，单位：g
    carbs           INTEGER,            -- 碳水，单位：g
    occurred_date   DATE,               -- 进餐日期，用于按日期和餐次做唯一约束
    occurred_time   TIMESTAMP,          -- 进餐时间
    create_time     TIMESTAMP NOT NULL, -- 创建时间
    update_time     TIMESTAMP           -- 更新时间
);

CREATE UNIQUE INDEX idx_meal_occurred_date_meal_type
    ON meal (occurred_date, meal_type);

-- 会话
CREATE TABLE conversation (
    id                     UUID PRIMARY KEY,       -- 会话ID，UUIDv7
    openai_conversation_id VARCHAR(128) NOT NULL,  -- OpenAI侧会话ID
    title                  VARCHAR(128),           -- 会话标题
    create_time            TIMESTAMP NOT NULL,     -- 创建时间
    last_active_time       TIMESTAMP NOT NULL      -- 最近活跃时间
);

-- 会话轮次
CREATE TABLE conversation_turn (
    id                 UUID PRIMARY KEY,   -- 轮次ID，UUIDv7
    conversation_id    UUID,               -- 会话ID，UUIDv7
    user_input         TEXT,               -- 用户输入文本
    model_output       TEXT,               -- 模型输出文本
    openai_response_id VARCHAR(128),       -- OpenAI侧响应ID
    model              VARCHAR(128),       -- 本轮使用的模型
    create_time        TIMESTAMP NOT NULL  -- 创建时间，单位：本地时间
);
