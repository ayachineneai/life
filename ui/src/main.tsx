import React from "react";
import { createRoot } from "react-dom/client";
import {
  Activity,
  ArrowUp,
  Bot,
  CalendarDays,
  CheckCircle2,
  Flame,
  Loader2,
  MessageCircle,
  Plus,
  Salad,
  Sparkles,
  Target,
  Utensils,
  Zap
} from "lucide-react";
import "./styles.css";

type MealType = "BREAKFAST" | "LUNCH" | "DINNER" | "SNACK";

type MealEntry = {
  id: string;
  title: string;
  type: MealType;
  time: string;
  occurredTime: string;
  content: string;
  calories: number;
  protein: number;
  carbs: number;
  fat: number;
};

type MealRecord = {
  id: string;
  title?: string | null;
  mealType?: MealType | null;
  content?: string | null;
  calories?: number | null;
  protein?: number | null;
  carbs?: number | null;
  fat?: number | null;
  occurredTime?: string | null;
};

type MealListResponse = {
  meals: MealRecord[];
};

type ChatRole = "user" | "assistant" | "system";

type ChatMessage = {
  id: string;
  role: ChatRole;
  text: string;
};

type ChatTurnRecord = {
  id: string;
  userInput?: string | null;
  modelOutput?: string | null;
  createTime: string;
};

type ChatConversationRecord = {
  id: string;
  title?: string | null;
  createTime: string;
  lastActiveTime: string;
  turns: ChatTurnRecord[];
};

type ChatConversationsResponse = {
  conversations: ChatConversationRecord[];
};

type ToolEvent = {
  id: string;
  phase?: string;
  state: ToolState;
  callId?: string;
  itemId?: string;
  name?: string;
  arguments?: string;
  result?: unknown;
};

type ToolEventPayload = Omit<ToolEvent, "id">;

type ToolState = "executing" | "missing_tool" | "result";

type AgentStatus = "PREPARING" | "THINKING" | "USING_TOOL" | "RESPONDING" | "COMPLETED" | "FAILED";

type AgentEvent =
  | { type: "CONVERSATION"; conversationId: string }
  | { type: "STATUS"; status: AgentStatus }
  | { type: "PLAN_DELTA"; text: string }
  | { type: "DELTA"; text: string }
  | ({ type: "TOOL" } & ToolEventPayload)
  | { type: "DONE"; responseId?: string | null }
  | { type: "ERROR"; error: string };

const statusLabels: Record<AgentStatus, string> = {
  PREPARING: "准备中",
  THINKING: "规划中",
  USING_TOOL: "调用工具",
  RESPONDING: "回复中",
  COMPLETED: "完成",
  FAILED: "错误"
};

const debug = (...args: unknown[]) => {
  console.log("[life-agent]", ...args);
};

const mealTypeLabels: Record<MealType, string> = {
  BREAKFAST: "早餐",
  LUNCH: "午餐",
  DINNER: "晚餐",
  SNACK: "加餐"
};

const mealTypeOrder: Record<MealType, number> = {
  BREAKFAST: 0,
  LUNCH: 1,
  DINNER: 2,
  SNACK: 3
};

function initialChatMessages(): ChatMessage[] {
  return [
    {
      id: crypto.randomUUID(),
      role: "assistant",
      text: "告诉我你吃了什么，或者让我帮你记录一餐。我会在需要时调用工具，并把过程展示出来。"
    }
  ];
}

function App() {
  const [meals, setMeals] = React.useState<MealEntry[]>([]);
  const [mealsLoading, setMealsLoading] = React.useState(true);
  const [mealsError, setMealsError] = React.useState("");
  const [messages, setMessages] = React.useState<ChatMessage[]>(() => initialChatMessages());
  const [input, setInput] = React.useState("记录我的午餐：湖南辣椒小炒肉和一碗米饭。");
  const [status, setStatus] = React.useState("就绪");
  const [planDraft, setPlanDraft] = React.useState("");
  const [tools, setTools] = React.useState<ToolEvent[]>([]);
  const [streaming, setStreaming] = React.useState(false);
  const [conversationId, setConversationId] = React.useState("");
  const [conversations, setConversations] = React.useState<ChatConversationRecord[]>([]);
  const [historyError, setHistoryError] = React.useState("");
  const [draftMeal, setDraftMeal] = React.useState({
    title: "",
    type: "SNACK" as MealType,
    calories: "260",
    content: ""
  });

  const assistantId = React.useRef<string>("");
  const textQueue = React.useRef("");
  const pumping = React.useRef(false);
  const scrollRef = React.useRef<HTMLDivElement>(null);

  const totals = React.useMemo(() => {
    return meals.reduce(
      (sum, meal) => ({
        calories: sum.calories + meal.calories,
        protein: sum.protein + meal.protein,
        carbs: sum.carbs + meal.carbs,
        fat: sum.fat + meal.fat
      }),
      { calories: 0, protein: 0, carbs: 0, fat: 0 }
    );
  }, [meals]);

  React.useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: "smooth" });
  }, [messages, planDraft, tools, status]);

  React.useEffect(() => {
    void loadRecentConversations();
    void loadTodayMeals();
  }, []);

  async function addManualMeal(event: React.FormEvent) {
    event.preventDefault();
    if (!draftMeal.title.trim() || !draftMeal.content.trim()) return;
    const now = new Date();
    const calories = Number(draftMeal.calories) || 0;
    const protein = Math.round(calories * 0.08);
    const carbs = Math.round(calories * 0.13);
    const fat = Math.round(calories * 0.04);

    try {
      const response = await fetch("/meals", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          title: draftMeal.title.trim(),
          mealType: draftMeal.type,
          content: draftMeal.content.trim(),
          remark: null,
          calories,
          protein,
          carbs,
          fat,
          occurredTime: toLocalDateTime(now)
        })
      });

      if (!response.ok) throw new Error(await response.text());
      setDraftMeal({ title: "", type: "SNACK", calories: "260", content: "" });
      await loadTodayMeals();
    } catch (error) {
      debug("meal create failed", error);
      setMealsError("餐食保存失败");
    }
  }

  async function loadTodayMeals() {
    setMealsLoading(true);
    try {
      const response = await fetch("/meals/today");
      if (!response.ok) throw new Error(await response.text());
      const data = (await response.json()) as MealListResponse;
      setMeals(sortMeals(data.meals.map(toMealEntry)));
      setMealsError("");
    } catch (error) {
      debug("meals load failed", error);
      setMealsError("餐食记录暂不可用");
    } finally {
      setMealsLoading(false);
    }
  }

  function toMealEntry(meal: MealRecord): MealEntry {
    const type = meal.mealType || "SNACK";
    return {
      id: meal.id,
      title: meal.title || mealTypeLabels[type],
      type,
      time: mealTime(meal.occurredTime),
      occurredTime: meal.occurredTime || "",
      content: meal.content || "",
      calories: meal.calories || 0,
      protein: meal.protein || 0,
      carbs: meal.carbs || 0,
      fat: meal.fat || 0
    };
  }

  function sortMeals(items: MealEntry[]) {
    return [...items].sort((left, right) => {
      const typeCompare = mealTypeOrder[left.type] - mealTypeOrder[right.type];
      if (typeCompare !== 0) return typeCompare;
      return left.occurredTime.localeCompare(right.occurredTime);
    });
  }

  function askAiToRecord(meal: MealEntry) {
    setInput(
      `帮我记录这顿${mealTypeLabels[meal.type]}：${meal.content}。估算 ${meal.calories} kcal。`
    );
  }

  async function sendMessage(event?: React.FormEvent) {
    event?.preventDefault();
    const message = input.trim();
    if (!message || streaming) return;

    const userMessage: ChatMessage = { id: crypto.randomUUID(), role: "user", text: message };
    const assistantMessage: ChatMessage = { id: crypto.randomUUID(), role: "assistant", text: "" };
    assistantId.current = assistantMessage.id;
    textQueue.current = "";
    pumping.current = false;
    setMessages((current) => [...current, userMessage, assistantMessage]);
    setTools([]);
    setPlanDraft("");
    setStatus("连接中");
    debug("chat send", { conversationId: conversationId || null, messageLength: message.length });
    setInput("");
    setStreaming(true);

    try {
      const response = await fetch("/chat/stream", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          conversationId: conversationId || null,
          message
        })
      });

      if (!response.ok || !response.body) {
        const body = await response.text();
        debug("chat http error", response.status, body);
        throw new Error(body || `HTTP ${response.status}`);
      }

      await readSse(response.body);
    } catch (error) {
      const text = error instanceof Error ? error.message : "聊天流失败";
      debug("chat failed", text, error);
      setStatus("错误");
      setMessages((current) =>
        current.map((item) =>
          item.id === assistantId.current ? { ...item, text: item.text ? `${item.text}\n\n错误：${text}` : `错误：${text}` } : item
        )
      );
    } finally {
      setStreaming(false);
      pumping.current = false;
    }
  }

  async function readSse(body: ReadableStream<Uint8Array>) {
    const reader = body.getReader();
    const decoder = new TextDecoder();
    let buffer = "";
    while (true) {
      const { value, done } = await reader.read();
      if (done) break;
      buffer = consumeSse(buffer + decoder.decode(value, { stream: true }));
    }
    consumeSse(buffer + decoder.decode());
  }

  function consumeSse(buffer: string) {
    let index = buffer.indexOf("\n\n");
    while (index >= 0) {
      const block = buffer.slice(0, index);
      buffer = buffer.slice(index + 2);
      const payload = parseSse(block);
      if (payload) handleAgentEvent(payload);
      index = buffer.indexOf("\n\n");
    }
    return buffer;
  }

  function parseSse(block: string): AgentEvent | null {
    const data = block
      .split("\n")
      .filter((line) => line.startsWith("data:"))
      .map((line) => line.slice(5).trimStart())
      .join("\n");
    if (!data) return null;
    try {
      return JSON.parse(data) as AgentEvent;
    } catch (error) {
      debug("sse parse failed", { data, error });
      throw error;
    }
  }

  function handleAgentEvent(event: AgentEvent) {
    debug("sse event", event);
    switch (event.type) {
      case "CONVERSATION":
        setConversationId(event.conversationId);
        return;
      case "STATUS":
        setStatus(statusLabels[event.status]);
        return;
      case "PLAN_DELTA":
        setPlanDraft((current) => current + event.text);
        setStatus("规划中");
        return;
      case "DELTA":
        enqueueText(event.text);
        return;
      case "TOOL":
        setStatus(toolStatus(event));
        upsertTool(event);
        return;
      case "DONE":
        flushQueuedText();
        setStatus("完成");
        void loadRecentConversations();
        void loadTodayMeals();
        return;
      case "ERROR":
        setStatus(event.error);
        setMessages((current) =>
          current.map((item) =>
            item.id === assistantId.current ? { ...item, text: item.text ? `${item.text}\n\n错误：${event.error}` : `错误：${event.error}` } : item
          )
        );
    }
  }

  async function loadRecentConversations() {
    try {
      const response = await fetch("/chat/conversations?limit=10&turnLimit=10");
      if (!response.ok) throw new Error(await response.text());
      const data = (await response.json()) as ChatConversationsResponse;
      setConversations(data.conversations);
      setHistoryError("");

      const latest = data.conversations[0];
      if (!latest) {
        setConversationId("");
        setMessages(initialChatMessages());
        return;
      }

      setConversationId(latest.id);
      setMessages(messagesFromTurns(latest.turns));
    } catch (error) {
      debug("conversation history load failed", error);
      setHistoryError("历史记录暂不可用");
    }
  }

  function selectConversation(conversation: ChatConversationRecord) {
    if (streaming) return;
    setConversationId(conversation.id);
    setMessages(messagesFromTurns(conversation.turns));
    setPlanDraft("");
    setTools([]);
    setStatus("就绪");
  }

  function startNewConversation() {
    if (streaming) return;
    setConversationId("");
    setMessages(initialChatMessages());
    setPlanDraft("");
    setTools([]);
    setStatus("就绪");
  }

  function enqueueText(delta: string) {
    textQueue.current += delta;
    setStatus("回复中");
    if (!pumping.current) pumpText();
  }

  function pumpText() {
    if (!textQueue.current) {
      pumping.current = false;
      return;
    }
    pumping.current = true;
    const next = textQueue.current.slice(0, 1);
    textQueue.current = textQueue.current.slice(1);
    setMessages((current) =>
      current.map((item) =>
        item.id === assistantId.current ? { ...item, text: item.text + next } : item
      )
    );
    window.setTimeout(pumpText, next.charCodeAt(0) > 127 ? 18 : 10);
  }

  function flushQueuedText() {
    const rest = textQueue.current;
    if (!rest) return;
    textQueue.current = "";
    pumping.current = false;
    setMessages((current) =>
      current.map((item) =>
        item.id === assistantId.current ? { ...item, text: item.text + rest } : item
      )
    );
  }

  function upsertTool(tool: ToolEventPayload) {
    const id = tool.callId || tool.itemId || `${tool.phase}-${tool.state}-${tools.length}`;
    setTools((current) => {
      const index = current.findIndex((item) => item.id === id);
      const next = { id, ...tool };
      if (index < 0) return [...current, next];
      return current.map((item, currentIndex) =>
        currentIndex === index ? { ...item, ...next } : item
      );
    });
  }

  return (
    <main className="app-shell">
      <section className="workspace">
        <header className="topbar">
          <div className="brand">
            <div className="brand-mark">
              <Sparkles size={20} />
            </div>
            <div>
              <h1>生活助手</h1>
              <p>饮食记录与智能对话</p>
            </div>
          </div>
          <div className="date-pill">
            <CalendarDays size={17} />
            <span>{new Date().toLocaleDateString("zh-CN", { weekday: "short", month: "short", day: "numeric" })}</span>
          </div>
        </header>

        <section className="hero-band">
          <div>
            <p className="eyebrow">今日营养</p>
            <h2>{totals.calories.toLocaleString()} kcal</h2>
            <p className="hero-copy">清晰查看今天的餐食、宏量营养和 AI 辅助记录。</p>
          </div>
          <div className="rings" aria-hidden="true">
            <div className="ring ring-cal">{Math.min(100, Math.round((totals.calories / 2200) * 100))}%</div>
            <div className="ring ring-pro">{Math.min(100, Math.round((totals.protein / 120) * 100))}%</div>
          </div>
        </section>

        <section className="metric-grid">
          <Metric icon={<Flame />} label="热量" value={`${totals.calories}`} tone="warm" />
          <Metric icon={<Activity />} label="蛋白质" value={`${totals.protein}g`} tone="mint" />
          <Metric icon={<Zap />} label="碳水" value={`${totals.carbs}g`} tone="blue" />
          <Metric icon={<Target />} label="脂肪" value={`${totals.fat}g`} tone="pink" />
        </section>

        <section className="content-grid">
          <section className="panel meal-panel">
            <div className="panel-head">
              <div>
                <h3>餐食时间线</h3>
                <p>点击餐食，让 AI 帮你整理成记录。</p>
              </div>
              <Utensils size={19} />
            </div>
            <div className="meal-list">
              {mealsLoading && <div className="meal-empty">正在读取数据库...</div>}
              {!mealsLoading && mealsError && <div className="meal-empty">{mealsError}</div>}
              {!mealsLoading && !mealsError && meals.length === 0 && (
                <div className="meal-empty">今天还没有餐食记录</div>
              )}
              {meals.map((meal) => (
                <button className="meal-row" key={meal.id} onClick={() => askAiToRecord(meal)}>
                  <div className="meal-icon">
                    <Salad size={18} />
                  </div>
                  <div className="meal-main">
                    <div className="meal-title-line">
                      <strong>{meal.title}</strong>
                      <span>{meal.time}</span>
                    </div>
                    <p>{meal.content}</p>
                    <div className="macro-line">
                      <span>{mealTypeLabels[meal.type]}</span>
                      <span>{meal.calories} kcal</span>
                      <span>蛋白 {meal.protein}g</span>
                      <span>碳水 {meal.carbs}g</span>
                      <span>脂肪 {meal.fat}g</span>
                    </div>
                  </div>
                </button>
              ))}
            </div>
          </section>

          <section className="panel form-panel">
            <div className="panel-head">
              <div>
                <h3>快速添加</h3>
                <p>直接写入数据库，左侧列表会自动刷新。</p>
              </div>
              <Plus size={19} />
            </div>
            <form onSubmit={addManualMeal} className="meal-form">
              <label>
                <span>标题</span>
                <input
                  value={draftMeal.title}
                  onChange={(event) => setDraftMeal({ ...draftMeal, title: event.target.value })}
                  placeholder="夜宵"
                />
              </label>
              <label>
                <span>内容</span>
                <textarea
                  value={draftMeal.content}
                  onChange={(event) => setDraftMeal({ ...draftMeal, content: event.target.value })}
                  placeholder="苹果、杏仁、茶"
                />
              </label>
              <div className="form-pair">
                <label>
                  <span>类型</span>
                  <select
                    value={draftMeal.type}
                    onChange={(event) =>
                      setDraftMeal({ ...draftMeal, type: event.target.value as MealType })
                    }
                  >
                    {Object.entries(mealTypeLabels).map(([value, label]) => (
                      <option value={value} key={value}>
                        {label}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  <span>热量</span>
                  <input
                    value={draftMeal.calories}
                    onChange={(event) => setDraftMeal({ ...draftMeal, calories: event.target.value })}
                    inputMode="numeric"
                  />
                </label>
              </div>
              <button className="primary-button" type="submit">
                <Plus size={16} />
                <span>添加餐食</span>
              </button>
            </form>
          </section>
        </section>
      </section>

      <aside className="chat-panel">
        <div className="chat-head">
          <div>
            <p className="eyebrow">AI 助手</p>
            <h2>智能对话</h2>
          </div>
          <div className={`status-dot ${streaming ? "live" : ""}`}>
            {streaming ? <Loader2 size={16} className="spin" /> : <CheckCircle2 size={16} />}
            <span>{status}</span>
          </div>
        </div>

        <div className="conversation-history">
          <div className="conversation-history-head">
            <span>历史记录</span>
            <button disabled={streaming} onClick={startNewConversation} type="button">
              <Plus size={14} />
              <span>新建</span>
            </button>
          </div>
          <div className="conversation-list">
            {historyError && <div className="conversation-empty">{historyError}</div>}
            {!historyError && conversations.length === 0 && <div className="conversation-empty">暂无会话</div>}
            {!historyError && conversations.map((conversation) => (
              <button
                className={`conversation-row ${conversation.id === conversationId ? "active" : ""}`}
                disabled={streaming}
                key={conversation.id}
                onClick={() => selectConversation(conversation)}
                title={conversation.title || conversation.id}
                type="button"
              >
                <MessageCircle size={15} />
                <span>{conversationTitle(conversation)}</span>
                <time>{conversationTime(conversation.lastActiveTime)}</time>
              </button>
            ))}
          </div>
        </div>

        <div className="chat-scroll" ref={scrollRef}>
          {planDraft && (
            <div className="plan-strip">
              <Bot size={17} />
              <span>{planDraft}</span>
            </div>
          )}

          {messages.map((message) => (
            <div className={`message ${message.role}`} key={message.id}>
              <div className="message-avatar">
                {message.role === "assistant" ? <Bot size={16} /> : <MessageCircle size={16} />}
              </div>
              <div className="message-bubble">
                {message.text || (message.role === "assistant" && streaming ? "思考中..." : "")}
              </div>
            </div>
          ))}

          {tools.length > 0 && (
            <div className="tool-stack">
              {tools.map((tool) => (
                <div className={`tool-card state-${tool.state}`} key={tool.id}>
                  <div className="tool-card-head">
                    <span>{tool.name || "工具调用"}</span>
                    <b>{toolStateLabel(tool.state)}</b>
                  </div>
                  <pre>{formatTool(tool)}</pre>
                </div>
              ))}
            </div>
          )}
        </div>

        <form className="chat-input" onSubmit={sendMessage}>
          <textarea
            value={input}
            onChange={(event) => setInput(event.target.value)}
            placeholder="让 AI 帮你记录、检查或总结一餐..."
            disabled={streaming}
          />
          <button className="send-button" disabled={streaming || !input.trim()} type="submit">
            <ArrowUp size={18} />
          </button>
        </form>
      </aside>
    </main>
  );
}

function Metric({
  icon,
  label,
  value,
  tone
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
  tone: string;
}) {
  return (
    <div className={`metric metric-${tone}`}>
      <div className="metric-icon">{icon}</div>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function formatTool(tool: ToolEvent) {
  const payload = {
    阶段: tool.phase,
    调用ID: tool.callId,
    参数: parseJson(tool.arguments),
    结果: tool.result
  };
  return JSON.stringify(payload, null, 2);
}

function messagesFromTurns(turns: ChatTurnRecord[]) {
  const messages = turns.flatMap((turn) => {
    const items: ChatMessage[] = [];
    if (turn.userInput) {
      items.push({
        id: `${turn.id}-user`,
        role: "user",
        text: turn.userInput
      });
    }
    if (turn.modelOutput) {
      items.push({
        id: `${turn.id}-assistant`,
        role: "assistant",
        text: turn.modelOutput
      });
    }
    return items;
  });
  return messages.length > 0 ? messages : initialChatMessages();
}

function conversationTitle(conversation: ChatConversationRecord) {
  const title = conversation.title || conversation.turns[0]?.userInput || "未命名会话";
  return title.length <= 28 ? title : `${title.slice(0, 28)}...`;
}

function conversationTime(value: string) {
  if (!value) return "";
  return value.replace("T", " ").slice(0, 16);
}

function mealTime(value?: string | null) {
  if (!value) return "--:--";
  return value.replace("T", " ").slice(11, 16);
}

function toLocalDateTime(value: Date) {
  const pad = (part: number) => String(part).padStart(2, "0");
  return [
    value.getFullYear(),
    pad(value.getMonth() + 1),
    pad(value.getDate()),
  ].join("-") + "T" + [
    pad(value.getHours()),
    pad(value.getMinutes()),
    pad(value.getSeconds()),
  ].join(":");
}

function toolStatus(tool: ToolEventPayload) {
  switch (tool.state) {
    case "executing":
      return tool.name ? `正在执行工具：${tool.name}` : "正在执行工具";
    case "missing_tool":
      return tool.name ? `缺少工具实现：${tool.name}` : "缺少工具实现";
    case "result":
      return "工具执行完成";
    default:
      return "工具活动";
  }
}

function toolStateLabel(state: ToolState) {
  switch (state) {
    case "executing":
      return "执行中";
    case "missing_tool":
      return "缺少工具";
    case "result":
      return "已完成";
  }
}

function parseJson(value?: string) {
  if (!value) return undefined;
  try {
    return JSON.parse(value);
  } catch {
    return value;
  }
}

createRoot(document.getElementById("root")!).render(<App />);
