export const languages = [
  { id: "en-IN", label: "English (India)", sttCode: "en-IN", translationCode: "en", ttsCode: "en-IN" },
  { id: "en-US", label: "English (US)", sttCode: "en-US", translationCode: "en", ttsCode: "en-US" },
  { id: "hi-IN", label: "Hindi", sttCode: "hi-IN", translationCode: "hi", ttsCode: "hi-IN" },
  { id: "bn-IN", label: "Bengali", sttCode: "bn-IN", translationCode: "bn", ttsCode: "bn-IN" },
  { id: "ta-IN", label: "Tamil", sttCode: "ta-IN", translationCode: "ta", ttsCode: "ta-IN" },
  { id: "te-IN", label: "Telugu", sttCode: "te-IN", translationCode: "te", ttsCode: "te-IN" },
  { id: "mr-IN", label: "Marathi", sttCode: "mr-IN", translationCode: "mr", ttsCode: "mr-IN" },
  { id: "gu-IN", label: "Gujarati", sttCode: "gu-IN", translationCode: "gu", ttsCode: "gu-IN" },
  { id: "kn-IN", label: "Kannada", sttCode: "kn-IN", translationCode: "kn", ttsCode: "kn-IN" },
  { id: "ml-IN", label: "Malayalam", sttCode: "ml-IN", translationCode: "ml", ttsCode: "ml-IN" },
  { id: "pa-IN", label: "Punjabi", sttCode: "pa", translationCode: "pa", ttsCode: "pa-IN" },
  { id: "ur-IN", label: "Urdu", sttCode: "ur-IN", translationCode: "ur", ttsCode: "ur-IN" }
];

export function isSupportedTranslationCode(code) {
  return languages.some((language) => language.translationCode === code);
}
