import { randomUUID } from "node:crypto";

const SENTENCE_END = /[.!?।؟]$/u;

export class SegmentChunker {
  constructor({
    stableMs = 700,
    silenceMs = 800,
    maxSegmentMs = 12_000,
    minChars = 8
  } = {}) {
    this.stableMs = stableMs;
    this.silenceMs = silenceMs;
    this.maxSegmentMs = maxSegmentMs;
    this.minChars = minChars;
    this.reset();
  }

  reset() {
    this.currentText = "";
    this.lastText = "";
    this.segmentStartedAt = 0;
    this.lastChangedAt = 0;
    this.lastSpeechAt = 0;
    this.committed = new Set();
  }

  observeTranscript({ text, isFinal = false, nowMs = Date.now() }) {
    const clean = normalizeText(text);
    if (!clean) return null;
    if (!this.segmentStartedAt) this.segmentStartedAt = nowMs;
    if (clean !== this.lastText) {
      this.lastText = clean;
      this.lastChangedAt = nowMs;
    }
    this.currentText = clean;
    if (isFinal) return this.commit("stt_final", nowMs);
    if (SENTENCE_END.test(clean)) return this.commit("punctuation", nowMs);
    if (clean.length >= this.minChars && nowMs - this.lastChangedAt >= this.stableMs) {
      return this.commit("stable_partial", nowMs);
    }
    if (nowMs - this.segmentStartedAt >= this.maxSegmentMs) {
      return this.commit("max_duration", nowMs);
    }
    return null;
  }

  observeVad({ vadState, nowMs = Date.now() }) {
    if (vadState === "SPEECH") {
      this.lastSpeechAt = nowMs;
      return null;
    }
    if (!this.currentText || !this.lastSpeechAt) return null;
    if (nowMs - this.lastSpeechAt >= this.silenceMs) {
      return this.commit("silence", nowMs);
    }
    return null;
  }

  commit(reason, nowMs = Date.now()) {
    const text = normalizeText(this.currentText);
    if (text.length < this.minChars || this.committed.has(text)) return null;
    const segment = {
      segmentId: randomUUID(),
      text,
      reason,
      committedAtMs: nowMs
    };
    this.committed.add(text);
    this.currentText = "";
    this.lastText = "";
    this.segmentStartedAt = 0;
    this.lastChangedAt = 0;
    return segment;
  }
}

export function normalizeText(text) {
  return String(text || "").replace(/\s+/g, " ").trim();
}
