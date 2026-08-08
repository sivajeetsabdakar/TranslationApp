import test from "node:test";
import assert from "node:assert/strict";
import { SegmentChunker } from "../src/segmentChunker.js";

test("commits final STT transcript", () => {
  const chunker = new SegmentChunker();
  const segment = chunker.observeTranscript({ text: "Where is the station", isFinal: true, nowMs: 1000 });
  assert.equal(segment.text, "Where is the station");
  assert.equal(segment.reason, "stt_final");
});

test("commits sentence punctuation without waiting for stop", () => {
  const chunker = new SegmentChunker();
  const segment = chunker.observeTranscript({ text: "I need a ticket.", nowMs: 1000 });
  assert.equal(segment.reason, "punctuation");
});

test("commits stable partial after configured delay", () => {
  const chunker = new SegmentChunker({ stableMs: 700 });
  assert.equal(chunker.observeTranscript({ text: "I need a ticket", nowMs: 1000 }), null);
  const segment = chunker.observeTranscript({ text: "I need a ticket", nowMs: 1800 });
  assert.equal(segment.reason, "stable_partial");
});

test("commits on silence after speech", () => {
  const chunker = new SegmentChunker({ silenceMs: 800 });
  chunker.observeTranscript({ text: "Please come tomorrow", nowMs: 1000 });
  chunker.observeVad({ vadState: "SPEECH", nowMs: 1100 });
  const segment = chunker.observeVad({ vadState: "SILENCE", nowMs: 2000 });
  assert.equal(segment.reason, "silence");
});

test("does not repeat identical committed chunks", () => {
  const chunker = new SegmentChunker();
  const first = chunker.observeTranscript({ text: "Good morning.", nowMs: 1000 });
  const second = chunker.observeTranscript({ text: "Good morning.", nowMs: 2000 });
  assert.ok(first);
  assert.equal(second, null);
});

test("normalizes repeated whitespace before committing", () => {
  const chunker = new SegmentChunker();
  const segment = chunker.observeTranscript({ text: "  Good    evening.  ", nowMs: 1000 });
  assert.equal(segment.text, "Good evening.");
});
