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
  content: string;
  calories: number;
  protein: number;
  carbs: number;
  fat: number;
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

type AgentPlan = {
  intent?: string;
  summary: string;
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

type ToolState = "arguments_delta" | "arguments_done" | "executing" | "missing_tool" | "result";

type AgentStatus = "PREPARING" | "THINKING" | "USING_TOOL" | "RESPONDING" | "COMPLETED" | "FAILED";

type AgentEvent =
  | { type: "CONVERSATION"; conversationId: string }
  | { type: "STATUS"; status: AgentStatus }
  | { type: "PLAN_DELTA"; text: string }
  | { type: "PLAN"; plan: AgentPlan }
  | { type: "DELTA"; text: string }
  | ({ type: "TOOL" } & ToolEventPayload)
  | { type: "DONE"; responseId?: string | null }
  | { type: "ERROR"; error: string };

const statusLabels: Record<AgentStatus, string> = {
  PREPARING: "Preparing",
  THINKING: "Thinking",
  USING_TOOL: "Using tool",
  RESPONDING: "Responding",
  COMPLETED: "Done",
  FAILED: "Error"
};

const debug = (...args: unknown[]) => {
  console.log("[life-agent]", ...args);
};

const initialMeals: MealEntry[] = [
  {
    id: "meal-1",
    title: "Power Breakfast",
    type: "BREAKFAST",
    time: "08:12",
    content: "Greek yogurt, blueberries, oat granola",
    calories: 420,
    protein: 28,
    carbs: 46,
    fat: 12
  },
  {
    id: "meal-2",
    title: "Pepper Pork Lunch",
    type: "LUNCH",
    time: "12:38",
    content: "Hunan pepper fried pork, steamed rice",
    calories: 830,
    protein: 36,
    carbs: 92,
    fat: 34
  },
  {
    id: "meal-3",
    title: "Evening Reset",
    type: "DINNER",
    time: "18:44",
    content: "Salmon, greens, roasted potatoes",
    calories: 610,
    protein: 42,
    carbs: 48,
    fat: 24
  }
];

const mealTypeLabels: Record<MealType, string> = {
  BREAKFAST: "Breakfast",
  LUNCH: "Lunch",
  DINNER: "Dinner",
  SNACK: "Snack"
};

function initialChatMessages(): ChatMessage[] {
  return [
    {
      id: crypto.randomUUID(),
      role: "assistant",
      text: "Tell me what you ate, or ask me to record a meal. I can call tools and show every step."
    }
  ];
}

function App() {
  const [meals, setMeals] = React.useState(initialMeals);
  const [messages, setMessages] = React.useState<ChatMessage[]>(() => initialChatMessages());
  const [input, setInput] = React.useState("Record my lunch: Hunan pepper fried pork and one bowl of rice.");
  const [status, setStatus] = React.useState("Ready");
  const [plan, setPlan] = React.useState<AgentPlan | null>(null);
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
  }, [messages, plan, planDraft, tools, status]);

  React.useEffect(() => {
    void loadRecentConversations();
  }, []);

  function addManualMeal(event: React.FormEvent) {
    event.preventDefault();
    if (!draftMeal.title.trim() || !draftMeal.content.trim()) return;
    const now = new Date();
    const calories = Number(draftMeal.calories) || 0;
    setMeals((current) => [
      {
        id: crypto.randomUUID(),
        title: draftMeal.title.trim(),
        type: draftMeal.type,
        time: now.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
        content: draftMeal.content.trim(),
        calories,
        protein: Math.round(calories * 0.08),
        carbs: Math.round(calories * 0.13),
        fat: Math.round(calories * 0.04)
      },
      ...current
    ]);
    setDraftMeal({ title: "", type: "SNACK", calories: "260", content: "" });
  }

  function askAiToRecord(meal: MealEntry) {
    setInput(
      `Record this ${mealTypeLabels[meal.type].toLowerCase()}: ${meal.content}. Estimated ${meal.calories} kcal.`
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
    setPlan(null);
    setPlanDraft("");
    setStatus("Connecting");
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
      const text = error instanceof Error ? error.message : "Chat stream failed";
      debug("chat failed", text, error);
      setStatus("Error");
      setMessages((current) =>
        current.map((item) =>
          item.id === assistantId.current ? { ...item, text: item.text ? `${item.text}\n\nError: ${text}` : `Error: ${text}` } : item
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
        setStatus("Planning");
        return;
      case "PLAN":
        setPlan(event.plan);
        setPlanDraft("");
        setStatus("Plan ready");
        return;
      case "DELTA":
        enqueueText(event.text);
        return;
      case "TOOL":
        setStatus(toolStatus(event));
        upsertTool(event);
        return;
      case "DONE":
        setStatus("Done");
        void loadRecentConversations();
        return;
      case "ERROR":
        setStatus(event.error);
        setMessages((current) =>
          current.map((item) =>
            item.id === assistantId.current ? { ...item, text: item.text ? `${item.text}\n\nError: ${event.error}` : `Error: ${event.error}` } : item
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
      setHistoryError("History unavailable");
    }
  }

  function selectConversation(conversation: ChatConversationRecord) {
    if (streaming) return;
    setConversationId(conversation.id);
    setMessages(messagesFromTurns(conversation.turns));
    setPlan(null);
    setPlanDraft("");
    setTools([]);
    setStatus("Ready");
  }

  function startNewConversation() {
    if (streaming) return;
    setConversationId("");
    setMessages(initialChatMessages());
    setPlan(null);
    setPlanDraft("");
    setTools([]);
    setStatus("Ready");
  }

  function enqueueText(delta: string) {
    textQueue.current += delta;
    setStatus("Streaming");
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
              <h1>Life Agent</h1>
              <p>Diet intelligence with tool-aware chat</p>
            </div>
          </div>
          <div className="date-pill">
            <CalendarDays size={17} />
            <span>{new Date().toLocaleDateString(undefined, { weekday: "short", month: "short", day: "numeric" })}</span>
          </div>
        </header>

        <section className="hero-band">
          <div>
            <p className="eyebrow">Today&apos;s Nutrition</p>
            <h2>{totals.calories.toLocaleString()} kcal</h2>
            <p className="hero-copy">A clean daily readout for meals, macros, and AI-assisted logging.</p>
          </div>
          <div className="rings" aria-hidden="true">
            <div className="ring ring-cal">{Math.min(100, Math.round((totals.calories / 2200) * 100))}%</div>
            <div className="ring ring-pro">{Math.min(100, Math.round((totals.protein / 120) * 100))}%</div>
          </div>
        </section>

        <section className="metric-grid">
          <Metric icon={<Flame />} label="Calories" value={`${totals.calories}`} tone="warm" />
          <Metric icon={<Activity />} label="Protein" value={`${totals.protein}g`} tone="mint" />
          <Metric icon={<Zap />} label="Carbs" value={`${totals.carbs}g`} tone="blue" />
          <Metric icon={<Target />} label="Fat" value={`${totals.fat}g`} tone="pink" />
        </section>

        <section className="content-grid">
          <section className="panel meal-panel">
            <div className="panel-head">
              <div>
                <h3>Meal Timeline</h3>
                <p>Tap a meal to hand it to the agent.</p>
              </div>
              <Utensils size={19} />
            </div>
            <div className="meal-list">
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
                      <span>P {meal.protein}g</span>
                      <span>C {meal.carbs}g</span>
                      <span>F {meal.fat}g</span>
                    </div>
                  </div>
                </button>
              ))}
            </div>
          </section>

          <section className="panel form-panel">
            <div className="panel-head">
              <div>
                <h3>Quick Add</h3>
                <p>Local scratchpad while the agent handles durable records.</p>
              </div>
              <Plus size={19} />
            </div>
            <form onSubmit={addManualMeal} className="meal-form">
              <label>
                <span>Title</span>
                <input
                  value={draftMeal.title}
                  onChange={(event) => setDraftMeal({ ...draftMeal, title: event.target.value })}
                  placeholder="Late snack"
                />
              </label>
              <label>
                <span>Content</span>
                <textarea
                  value={draftMeal.content}
                  onChange={(event) => setDraftMeal({ ...draftMeal, content: event.target.value })}
                  placeholder="Apple, almonds, tea"
                />
              </label>
              <div className="form-pair">
                <label>
                  <span>Type</span>
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
                  <span>Calories</span>
                  <input
                    value={draftMeal.calories}
                    onChange={(event) => setDraftMeal({ ...draftMeal, calories: event.target.value })}
                    inputMode="numeric"
                  />
                </label>
              </div>
              <button className="primary-button" type="submit">
                <Plus size={16} />
                <span>Add meal</span>
              </button>
            </form>
          </section>
        </section>
      </section>

      <aside className="chat-panel">
        <div className="chat-head">
          <div>
            <p className="eyebrow">AI Agent</p>
            <h2>Tool Chat</h2>
          </div>
          <div className={`status-dot ${streaming ? "live" : ""}`}>
            {streaming ? <Loader2 size={16} className="spin" /> : <CheckCircle2 size={16} />}
            <span>{status}</span>
          </div>
        </div>

        <div className="conversation-history">
          <div className="conversation-history-head">
            <span>History</span>
            <button disabled={streaming} onClick={startNewConversation} type="button">
              <Plus size={14} />
              <span>New</span>
            </button>
          </div>
          <div className="conversation-list">
            {historyError && <div className="conversation-empty">{historyError}</div>}
            {!historyError && conversations.length === 0 && <div className="conversation-empty">No conversations yet</div>}
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
          {(plan || planDraft) && (
            <div className="plan-strip">
              <Bot size={17} />
              <span>{plan?.summary || planDraft}</span>
            </div>
          )}

          {messages.map((message) => (
            <div className={`message ${message.role}`} key={message.id}>
              <div className="message-avatar">
                {message.role === "assistant" ? <Bot size={16} /> : <MessageCircle size={16} />}
              </div>
              <div className="message-bubble">
                {message.text || (message.role === "assistant" && streaming ? "Thinking..." : "")}
              </div>
            </div>
          ))}

          {tools.length > 0 && (
            <div className="tool-stack">
              {tools.map((tool) => (
                <div className={`tool-card state-${tool.state}`} key={tool.id}>
                  <div className="tool-card-head">
                    <span>{tool.name || "Tool call"}</span>
                    <b>{tool.state}</b>
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
            placeholder="Ask the agent to record, inspect, or summarize a meal..."
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
    phase: tool.phase,
    callId: tool.callId,
    arguments: parseJson(tool.arguments),
    result: tool.result
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
  const title = conversation.title || conversation.turns[0]?.userInput || "Untitled chat";
  return title.length <= 28 ? title : `${title.slice(0, 28)}...`;
}

function conversationTime(value: string) {
  if (!value) return "";
  return value.replace("T", " ").slice(0, 16);
}

function toolStatus(tool: ToolEventPayload) {
  switch (tool.state) {
    case "arguments_delta":
      return "Preparing tool call";
    case "arguments_done":
      return tool.name ? `Tool arguments ready: ${tool.name}` : "Tool arguments ready";
    case "executing":
      return tool.name ? `Executing tool: ${tool.name}` : "Executing tool";
    case "missing_tool":
      return tool.name ? `Missing tool implementation: ${tool.name}` : "Missing tool implementation";
    case "result":
      return "Tool execution completed";
    default:
      return "Tool activity";
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
