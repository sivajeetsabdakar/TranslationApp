import speech from "@google-cloud/speech";
import textToSpeech from "@google-cloud/text-to-speech";
import { v2 as translateV2 } from "@google-cloud/translate";

const speechClient = new speech.SpeechClient();
const translateClient = new translateV2.Translate();
const ttsClient = new textToSpeech.TextToSpeechClient();

export function createSpeechStream({ sourceLang, onTranscript, onError }) {
  const request = {
    config: {
      encoding: "LINEAR16",
      sampleRateHertz: 16000,
      languageCode: sourceLang,
      enableAutomaticPunctuation: true,
      model: "latest_long"
    },
    interimResults: true,
    singleUtterance: false
  };

  return speechClient
    .streamingRecognize(request)
    .on("error", onError)
    .on("data", (data) => {
      for (const result of data.results || []) {
        const alternative = result.alternatives?.[0];
        const text = alternative?.transcript?.trim();
        if (text) onTranscript({ text, isFinal: Boolean(result.isFinal) });
      }
    });
}

export async function translateText({ text, targetLang }) {
  const [translation] = await translateClient.translate(text, targetLang);
  return Array.isArray(translation) ? translation[0] : translation;
}

export async function synthesizeSpeech({ text, targetTtsLang }) {
  const [response] = await ttsClient.synthesizeSpeech({
    input: { text },
    voice: { languageCode: targetTtsLang },
    audioConfig: {
      audioEncoding: "LINEAR16",
      sampleRateHertz: 24000,
      speakingRate: 1.04
    }
  });
  return response.audioContent;
}
