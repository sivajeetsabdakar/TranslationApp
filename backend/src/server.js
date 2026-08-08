import express from "express";
import http from "node:http";
import { URL } from "node:url";
import { WebSocketServer } from "ws";
import { SegmentChunker } from "./segmentChunker.js";
import { createSpeechStream, synthesizeSpeech, translateText } from "./googlePipeline.js";
import { createMockSpeechStream, mockSynthesizeSpeech, mockTranslateText } from "./mockPipeline.js";

const PORT = Number(process.env.PORT || 8080);
const MOCK = process.env.VOCALINGO_MOCK === "1";
const API_TOKEN = process.env.VOCALINGO_API_TOKEN || "";
const MAX_CONNECTIONS_PER_IP = Number(process.env.VOCALINGO_MAX_CONNECTIONS_PER_IP || 4);
const MAX_SESSION_MS = Number(process.env.VOCALINGO_MAX_SESSION_MS || 10 * 60 * 1000);
const MAX_AUDIO_FRAME_BYTES = Number(process.env.VOCALINGO_MAX_AUDIO_FRAME_BYTES || 64 * 1024);
const MAX_MESSAGES_PER_MINUTE = Number(process.env.VOCALINGO_MAX_MESSAGES_PER_MINUTE || 900);
const activeConnectionsByIp = new Map();

const app = express();
app.get("/health", (_, res) => res.json({ ok: true, mock: MOCK, authEnabled: Boolean(API_TOKEN) }));

const server = http.createServer(app);
const wss = new WebSocketServer({
  server,
  path: "/ws",
  verifyClient: ({ req }) => isAuthorized(req) && isConnectionAllowed(req)
});

wss.on("connection", (socket) => {
  let state = null;
  const ip = clientIp(socket);
  incrementConnection(ip);

  socket.on("message", async (raw) => {
    try {
      if (!allowMessage(socket)) return;
      const message = JSON.parse(raw.toString());
      if (message.type === "startSession") {
        cleanup(state);
        state = startSession(socket, message);
        return;
      }
      if (!state) return;
      if (message.type === "audioFrame") {
        const frame = Buffer.from(message.pcmFrame || "", "base64");
        if (frame.length > MAX_AUDIO_FRAME_BYTES) {
          send(socket, { type: "sessionError", text: "Audio frame exceeded the session limit." });
          socket.close(1009, "audio frame too large");
          return;
        }
        state.speechStream.write(frame);
        const segment = state.chunker.observeVad({ vadState: message.vadState });
        if (segment) await processSegment(socket, state, segment);
        return;
      }
      if (message.type === "vadState") {
        const segment = state.chunker.observeVad({ vadState: message.vadState });
        if (segment) await processSegment(socket, state, segment);
        return;
      }
      if (message.type === "stopSession") {
        cleanup(state);
        state = null;
        send(socket, { type: "sessionStopped" });
        return;
      }
      if (message.type === "cancelPlayback") {
        send(socket, { type: "playbackCancelled" });
      }
    } catch (error) {
      send(socket, { type: "sessionError", text: error.message || "Backend error" });
    }
  });

  socket.on("close", () => {
    cleanup(state);
    decrementConnection(ip);
  });
});

function startSession(socket, message) {
  const validationError = validateStartSession(message);
  if (validationError) {
    send(socket, { type: "sessionError", text: validationError });
    return null;
  }
  const chunker = new SegmentChunker();
  const state = {
    sessionId: message.sessionId,
    speakerSide: message.speakerSide,
    sourceLang: message.sourceLang,
    targetLang: message.targetLang,
    targetTtsLang: message.targetTtsLang,
    context: [],
    chunker,
    speechStream: null,
    timeout: null
  };
  const onTranscript = async ({ text, isFinal }) => {
    send(socket, { type: "partialTranscript", text, isFinal });
    const segment = chunker.observeTranscript({ text, isFinal });
    if (segment) await processSegment(socket, state, segment);
  };
  const onError = (error) => send(socket, { type: "sessionError", text: error.message || "Speech failed" });
  state.speechStream = MOCK
    ? createMockSpeechStream({ onTranscript })
    : createSpeechStream({ sourceLang: state.sourceLang, onTranscript, onError });
  state.timeout = setTimeout(() => {
    send(socket, { type: "sessionError", text: "Session time limit reached. Start again to continue." });
    cleanup(state);
    socket.close(1000, "session time limit");
  }, MAX_SESSION_MS);
  send(socket, { type: "latencyMetrics", latencyMs: 0 });
  return state;
}

async function processSegment(socket, state, segment) {
  const startedAt = Date.now();
  state.context.push(segment.text);
  state.context = state.context.slice(-6);
  send(socket, {
    type: "segmentCommitted",
    segmentId: segment.segmentId,
    text: segment.text,
    isFinal: true
  });
  const translated = MOCK
    ? await mockTranslateText({ text: segment.text, targetLang: state.targetLang })
    : await translateText({ text: segment.text, targetLang: state.targetLang, context: state.context });
  send(socket, {
    type: "translatedText",
    segmentId: segment.segmentId,
    text: translated,
    isFinal: true
  });
  const audio = MOCK
    ? await mockSynthesizeSpeech({ text: translated, targetTtsLang: state.targetTtsLang })
    : await synthesizeSpeech({ text: translated, targetTtsLang: state.targetTtsLang });
  send(socket, {
    type: "ttsAudio",
    segmentId: segment.segmentId,
    audioBase64: Buffer.from(audio).toString("base64"),
    latencyMs: Date.now() - startedAt
  });
  send(socket, { type: "latencyMetrics", latencyMs: Date.now() - startedAt });
}

function cleanup(state) {
  if (state?.timeout) clearTimeout(state.timeout);
  if (!state?.speechStream) return;
  try {
    state.speechStream.end();
  } catch {
    state.speechStream.destroy?.();
  }
}

function send(socket, payload) {
  if (socket.readyState === socket.OPEN) socket.send(JSON.stringify(payload));
}

function isAuthorized(req) {
  if (!API_TOKEN) return true;
  const authHeader = req.headers.authorization || "";
  if (authHeader === `Bearer ${API_TOKEN}`) return true;
  try {
    const url = new URL(req.url || "", "http://localhost");
    return url.searchParams.get("token") === API_TOKEN;
  } catch {
    return false;
  }
}

function isConnectionAllowed(req) {
  const ip = req.socket.remoteAddress || "unknown";
  return (activeConnectionsByIp.get(ip) || 0) < MAX_CONNECTIONS_PER_IP;
}

function validateStartSession(message) {
  const required = ["sessionId", "speakerSide", "sourceLang", "targetLang", "targetTtsLang"];
  for (const field of required) {
    if (!message[field] || typeof message[field] !== "string") return `Missing ${field}.`;
  }
  if (!["LEFT_USER", "RIGHT_USER"].includes(message.speakerSide)) return "Invalid speaker side.";
  return "";
}

function clientIp(socket) {
  return socket?._socket?.remoteAddress || "unknown";
}

function incrementConnection(ip) {
  activeConnectionsByIp.set(ip, (activeConnectionsByIp.get(ip) || 0) + 1);
}

function decrementConnection(ip) {
  const next = Math.max(0, (activeConnectionsByIp.get(ip) || 1) - 1);
  if (next === 0) activeConnectionsByIp.delete(ip);
  else activeConnectionsByIp.set(ip, next);
}

function allowMessage(socket) {
  const now = Date.now();
  const windowMs = 60_000;
  const rate = socket.rate || { startedAt: now, count: 0 };
  if (now - rate.startedAt >= windowMs) {
    rate.startedAt = now;
    rate.count = 0;
  }
  rate.count += 1;
  socket.rate = rate;
  if (rate.count <= MAX_MESSAGES_PER_MINUTE) return true;
  send(socket, { type: "sessionError", text: "Session message rate limit reached." });
  socket.close(1008, "rate limited");
  return false;
}

server.listen(PORT, () => {
  console.log(`VocaLingo backend listening on :${PORT} mock=${MOCK} auth=${Boolean(API_TOKEN)}`);
});
