export function createMockSpeechStream({ onTranscript }) {
  let frameCount = 0;
  const examples = [
    "Namaste, I am testing the live interpreter.",
    "This sentence should be translated while I continue speaking.",
    "The listener should hear chunks without waiting for five minutes."
  ];
  return {
    write() {
      frameCount += 1;
      if (frameCount % 10 === 0) {
        const text = examples[(frameCount / 10 - 1) % examples.length];
        onTranscript({ text, isFinal: true });
      }
    },
    end() {},
    destroy() {}
  };
}

export async function mockTranslateText({ text, targetLang }) {
  return `[${targetLang}] ${text}`;
}

export async function mockSynthesizeSpeech() {
  const sampleRate = 24_000;
  const durationMs = 420;
  const sampleCount = Math.floor(sampleRate * durationMs / 1000);
  const output = Buffer.alloc(sampleCount * 2);
  for (let index = 0; index < sampleCount; index++) {
    const wave = Math.sin(2 * Math.PI * 620 * index / sampleRate);
    const sample = Math.floor(wave * 32767 * 0.25);
    output.writeInt16LE(sample, index * 2);
  }
  return output;
}
