# VocaLingo / TranslationApp Technical Analysis

Last verified: 2026-08-08 against native Android package `com.vocalingo.app`, backend project `vocalingo-503812`, and production WSS endpoint `wss://vocalingo.duckdns.org/ws`.

## 1) Scope

This project is a bilingual live-translation mobile system with two running components:

- `TranslationApp/app`: Android app (Kotlin + Jetpack Compose) that captures speech, streams PCM audio + VAD state over WebSocket, and plays back translated speech.
- `TranslationApp/backend`: Node.js WebSocket gateway that handles STT → transcript chunking → translate → TTS and returns translated audio chunks.

There is also a parallel Flutter folder under `TranslationApp/flutter_app/`, but the production-grade implementation reflected here is the native Android module + Node backend.

## 2) Core Objective and UX Flow

The system provides conversational translation for two participants in one physical room by routing each participant’s translated voice to the opposite ear in a stereo audio space.

- Participant chooses which side (left/right) is currently speaking.
- App streams microphone frames to backend.
- Backend returns translated text and TTS audio chunks.
- App plays returned PCM in stereo and places that audio on the opposite channel of the current speaker.

## 3) Runtime Architecture

### 3.1 Android App Pipeline

In-app control and orchestration happen primarily in:

- `TranslationApp/app/src/main/java/com/vocalingo/app/MainActivity.kt`
- `TranslationApp/app/src/main/java/com/vocalingo/app/conversation/ConversationSession.kt`
- `TranslationApp/app/src/main/java/com/vocalingo/app/network/RealtimeGateway.kt`
- `TranslationApp/app/src/main/java/com/vocalingo/app/audio` directory

High-level runtime sequence:

1. User taps mic for a side.
2. Session connects WebSocket to backend.
3. Mic frames are captured and VAD tags are generated on-device.
4. App sends JSON events (`startSession`, `audioFrame`, `vadState`) to backend.
5. Backend sends event stream (`partialTranscript`, `segmentCommitted`, `translatedText`, `ttsAudio`, `latencyMetrics`).
6. App parses events and emits UI updates.
7. On `ttsAudio`, app enqueues converted stereo playback.

### 3.2 Backend Pipeline

Backend runtime entry and orchestration in:

- `TranslationApp/backend/src/server.js`

Per connection:

- `startSession`: initializes per-session state, language config, `SegmentChunker`, and Google Speech stream.
- Inbound message loop accepts:
  - `audioFrame` => forward PCM to STT stream + check segment boundaries by VAD
  - `vadState` => optional end-of-speech logic
  - `stopSession` / `close` => cleanup
- `processSegment()` handles STT commit chunk:
  - push segment to context window
  - translate via Google Translate
  - synthesize via Google TTS at 24 kHz PCM
  - return translated text + audioBase64

## 4) Detailed File/Component Map

### 4.1 Android audio capture and VAD

- `PcmAudioRecorder.kt`
  - Configured with:
    - sample rate `16,000 Hz`
    - mono input
    - 16-bit PCM
  - Reads chunks of `SAMPLE_RATE_HZ / 10` (~1600 samples = 100ms).
  - Runs voice activity detection on each chunk.

- `VoiceActivityDetector.kt`
  - Uses simple amplitude-based detection.
  - `speechThreshold` default: `900`
  - `hangoverFrames` default: `6`
  - Outputs `VadState.SPEECH` or `VadState.SILENCE`.

- `ConversationSession.kt`
  - Owns lifecycle for recorder + gateway + player + events flow.
  - When `start(side, sourceLang, targetLang)` is called:
    - Stops any existing session
    - Connects to backend and starts recorder
    - For every frame, sends both PCM and VAD state

### 4.2 Network gateway and event contract

- `RealtimeGateway.kt`
  - Uses OkHttp `WebSocket`
  - Adds `Authorization: Bearer <token>` if token is configured in BuildConfig.
  - Holds outgoing queue for messages before socket open.
  - Parses inbound payload into `ConversationEvent` and emits via `MutableSharedFlow`.

Inbound events supported by client:

- `partialTranscript` -> `ConversationEventType.PARTIAL_TRANSCRIPT`
- `segmentCommitted` -> `ConversationEventType.SEGMENT_COMMITTED`
- `translatedText` -> `ConversationEventType.TRANSLATED_TEXT`
- `ttsAudio` -> `ConversationEventType.TTS_AUDIO`
- `latencyMetrics` -> `ConversationEventType.LATENCY_METRICS`
- `sessionError` -> `ConversationEventType.SESSION_ERROR`

### 4.3 Backend streaming stack

- `segmentChunker.js`
  - Produces text segments from live transcript + timing + silence windows.
  - Heuristics:
    - punctuation finalization
    - stable partial text
    - max segment length timeout
    - VAD-driven silence timeout

- `googlePipeline.js`
  - `createSpeechStream` with `LINEAR16`, `16000`, `interimResults: true`
  - `translateText` via Google Translate
  - `synthesizeSpeech` via Google TTS with:
    - `audioEncoding: LINEAR16`
    - `sampleRateHertz: 24000`

- `mockPipeline.js` provides deterministic offline/mock fallback when `VOCALINGO_MOCK=1`.

## 5) Audio Stereo Routing Technique (Deep Dive)

This is the core “spatial separation” strategy used by the app.

### 5.1 Routing model

The app treats each participant as a side:

- `SpeakerSide.LEFT_USER`
- `SpeakerSide.RIGHT_USER`

This is defined in:

- `TranslationApp/app/src/main/java/com/vocalingo/app/conversation/Models.kt`

Each side has exactly one opposite listener side:

- left speaker is listened by right ear
- right speaker is listened by left ear

`AudioRouting.listenerForSpeaker(speaker)` gives the opposite side.

### 5.2 Where routing decision is made

The backend returns translated audio as 16-bit PCM **mono** bytes per segment. The app makes routing decisions before playback in `ConversationSession`:

- On `TTS_AUDIO` event, it schedules playback with:
  - `listenerSide = AudioRouting.listenerForSpeaker(event.side)`

This means:

- if left user spoke, translated output is sent to right channel path
- if right user spoke, translated output is sent to left channel path

### 5.3 Stereo conversion implementation

`StereoPcmPlayer.monoToStereo(monoPcm16, listenerSide)` performs deterministic channel gain mapping.

#### Gain mapping

From `AudioRouting.stereoGainsForListener(listener)`:

- `LEFT_USER` listener => `(leftGain=1f, rightGain=0f)`
- `RIGHT_USER` listener => `(leftGain=0f, rightGain=1f)`

So output sample-by-sample conversion:

- Decode mono sample from little-endian `Int16`
- Compute left/right sample values using per-channel gain
- Re-encode to interleaved stereo buffer `[L,R,L,R,...]`

#### Playback path

- `StereoPcmPlayer` owns a channel queue (`Channel<PlaybackItem>`).
- Worker coroutine pulls queue entries and writes stereo PCM using `AudioTrack` with `AudioFormat.CHANNEL_OUT_STEREO`.
- For each incoming translated chunk:
  - convert mono→stereo
  - create/reuse `AudioTrack`
  - write bytes and play

This yields a hard hard-channelized experience rather than mixed spatial blending.

### 5.4 Why this works for conversation

The design assumes:

- each user hears mostly the translated counterpart in one ear
- self-speech is not expected to be played in same path because playback is sent only for translated remote output

It is lightweight, deterministic, and avoids full-directional audio DSP (no phase or filter processing).

### 5.5 Calibration path

`EarCalibrationPlayer` can generate tones and play:

1. 660Hz/880Hz tone to LEFT
2. 880Hz/second tone to RIGHT

Purpose:

- verify physical stereo output path
- detect whether devices collapse channels to mono (or reorder channels)

The app does not programmatically measure ear output; Android does not expose a reliable per-ear microphone loopback for consumer earphones. Instead, calibration is user-confirmed:

- `STEREO_OK`: user confirms first tone was left and second tone was right.
- `MONO_OUTPUT_DETECTED`: user reports the tones were not separated.
- `UNKNOWN_BLUETOOTH_BEHAVIOR`: user dismisses/skips the test.

This is a practical production compromise: wired/USB-C stereo can be certified by the user test, while Bluetooth remains device-dependent.

## 6) Audio Signal and Codec Contracts

| Stage             | Format             | Sample rate | Channels | Encoding  |
|-------------------|--------------------|-------------|----------|-----------|
| Device capture     | PCM mono           | 16,000 Hz   | 1        | 16-bit LE |
| STT input          | same mono buffer   | 16,000 Hz   | 1        | 16-bit LE |
| TTS output         | PCM mono (server)  | 24,000 Hz   | 1        | LINEAR16  |
| On-device playback  | PCM stereo        | source rate | 2        | 16-bit LE |

Important mismatch:
- input 16 kHz, output 24 kHz
- output conversion is straightforwardly handled by `AudioTrack` with declared sample rate for playback item.

## 7) Concurrency and State Semantics

### 7.1 App-side state model

- `ConversationSession` holds `MutableSharedFlow` event stream for UI and playback.
- `start()` sets new `sessionId`, connects gateway and starts recorder.
- `stop()` closes playback cancellation + ws close + recorder stop.
- `shutdown()` additionally stops player queue.

### 7.2 Backend session state

Each websocket connection state contains:

- `speakerSide`, language pair
- `context` (rolling last segments)
- `chunker`
- `speechStream` handle
- session timeout handle

There is no durable session persistence; all state is ephemeral per websocket connection.

Backend runtime guardrails are enforced in `backend/src/server.js`:

- `VOCALINGO_MAX_CONNECTIONS_PER_IP` default `4`
- `VOCALINGO_MAX_SESSION_MS` default `600000` (10 minutes)
- `VOCALINGO_MAX_AUDIO_FRAME_BYTES` default `65536`
- `VOCALINGO_MAX_MESSAGES_PER_MINUTE` default `900`

These limits are abuse controls, not identity controls. They reduce accidental runaway sessions and low-effort abuse, but do not replace App Check or user-scoped authentication.

## 8) Message Protocol (JSON)

### 8.1 Client → Server

- `startSession`
  - `sessionId`
  - `speakerSide`
  - `sourceLang`
  - `targetLang`
  - `targetTtsLang`

- `audioFrame`
  - `pcmFrame` (base64)
  - `vadState`
  - `sentAtMs`

- `vadState`
  - same payload, periodic boundary checks

- `stopSession`, `cancelPlayback`
  - `stopSession` cleans up backend STT stream and returns `sessionStopped`.
  - `cancelPlayback` is acknowledged by backend with `playbackCancelled`; actual queued-audio cancellation is performed on-device by `ConversationSession`/`StereoPcmPlayer`.

### 8.2 Server → Client

- `partialTranscript`
- `segmentCommitted`
- `translatedText`
- `ttsAudio` (`audioBase64`)
- `latencyMetrics`
- `sessionError`

## 9) Security and Auth

- Production WebSocket path: `wss://vocalingo.duckdns.org/ws`
- Optional bearer protection using `VOCALINGO_API_TOKEN` (Node env) and app-side `BACKEND_API_TOKEN`.
- In local setup, mock mode can be enabled using `VOCALINGO_MOCK=1`.
- Production Google Cloud project: `vocalingo-503812`
- Firebase Android package: `com.vocalingo.app`

Configuration points:

- `TranslationApp/app/build.gradle.kts`
  - `BACKEND_WS_URL`
  - `BACKEND_API_TOKEN`
- `backend/src/server.js` and environment variables in `backend/README.md`

Important security note:

- The current app still embeds a static backend token in `BuildConfig.BACKEND_API_TOKEN`.
- This is acceptable for controlled testing/MVP distribution, but not strong enough for public launch.
- Production launch should add Firebase App Check / Play Integrity verification on the backend and reject requests without valid attestation.

## 10) Build and Runtime Settings

### Android

- App ID: `com.vocalingo.app`
- Android namespace/source package: `com.vocalingo.app`
- Jetpack Compose
- Min SDK 24, Target 34
- Recording permissions required:
  - `RECORD_AUDIO`
  - `INTERNET`
- Release disables Android backup and device transfer for app data.
- Debug builds permit cleartext access to `10.0.2.2`; release builds use WSS/HTTPS only.

### Backend

- `npm start` -> `node src/server.js`
- Production environment requires Google credentials for Speech, Translate, TTS.
- OCI deployment is Docker Compose-managed from `/home/ubuntu/vocalingo-backend`.
- Caddy path: `wss://vocalingo.duckdns.org/ws` proxies backend port 8080 on Docker network `whatsnew_default`.
- The backend does not publish a public app port; Caddy is the public ingress on 80/443.
- Production budget alert exists in Google Cloud for `vocalingo-503812` at INR 1000/month with 50%, 90%, and 100% thresholds.

## 11) Observability and Quality Signals

- `latencyMetrics` is emitted both at session open and after each segment processing.
- Transcripts and segment commit reasons are available via segmenter event states.
- Firebase Crashlytics and Analytics are wired into the Android app when `app/google-services.json` is present.
- Backend diagnostics currently rely on container logs, health checks, websocket errors, and client status events.
- No distributed tracing currently.

## 12) Testing coverage

Current automated coverage includes:

- `app/src/test/java/com/vocalingo/app/ConversationUnitTest.kt`
- `app/src/androidTest/java/com/vocalingo/app/ExampleInstrumentedTest.kt`
- `backend/test/segmentChunker.test.js`

Current Android unit checks include:

- speaker/listener inversion
- stereo gain mapping
- VAD threshold behavior
- mono->stereo sample placement

Current Android instrumentation checks include:

- split-screen UI renders on emulator
- ear-test dialog/confirmation flow

Current backend unit checks include:

- STT-final chunk commits
- punctuation chunk commits
- stable partial chunk commits
- silence-driven chunk commits
- duplicate chunk suppression
- whitespace normalization

Operational smoke testing has also verified the live WSS path through `vocalingo.duckdns.org`:

- Google STT transcript
- Google Translate output
- Google TTS audio return

## 13) Known Risks and Limitations

1. No true adaptive room calibration:
   - calibration is user-confirmed and not persisted.
   - no measured channel imbalance compensation.

2. Fixed device assumptions:
   - simple amplitude VAD (not robust to noisy environments).
   - simple hard-left/right routing may fail on devices forcing mono mixdown.

3. No duplex overlap support:
   - one active speaker side at a time in UI.
   - no mixing/ducking strategy for simultaneous speech.

4. Playback lifecycle simplicity:
   - `AudioTrack` is created per playback item currently.
   - this is easy to reason about but can increase overhead on high-frequency chunks.

5. Security:
   - backend token is static and shipped via app config; suitable for controlled MVP distribution, not high-risk public production without App Check or user-level auth.

6. Reconnect and offline behavior:
   - internet is required.
   - explicit reconnect/backoff is limited; users may need to restart a mic session after transport interruption.

## 14) Recommended Hardening (next iteration)

1. Measure channel quality
   - add optional calibration test that compares amplitude per ear and writes calibration profile.

2. Improve media pipeline
   - add sample-rate-resampling guard before/after backend TTS bytes.
   - reuse one long-lived `AudioTrack` for lower latency.

3. Improve VAD quality
   - replace amplitude-only detector with WebRTC VAD or RNNoise-style energy model.

4. Support full duplex and overlap
   - allow near-simultaneous turns with queue-level arbitration and UI indicators.

5. Strengthen auth
   - add Firebase App Check / Play Integrity verification.
   - optionally add short-lived session JWTs and revocation semantics.

## 15) Failure Modes and Recovery

- Mic permission missing:
  - recorder emits `SESSION_ERROR` and prompts via UI.

- Unauthorized websocket connect:
  - backend rejects handshake when token mismatch configured.

- Google backend fault:
  - STT/translate/TTS errors bubble as `sessionError` or pipeline errors and propagate to app events.

- Transport interruptions:
  - queueing and reconnect logic is currently limited (explicit reconnect not yet implemented).

## 16) Quick references

- [ConversationSession](D:/Codes/Projects/FORRESUME/TranslationApp/app/src/main/java/com/vocalingo/app/conversation/ConversationSession.kt)
- [RealtimeGateway](D:/Codes/Projects/FORRESUME/TranslationApp/app/src/main/java/com/vocalingo/app/network/RealtimeGateway.kt)
- [StereoPcmPlayer](D:/Codes/Projects/FORRESUME/TranslationApp/app/src/main/java/com/vocalingo/app/audio/StereoPcmPlayer.kt)
- [AudioRouting](D:/Codes/Projects/FORRESUME/TranslationApp/app/src/main/java/com/vocalingo/app/conversation/AudioRouting.kt)
- [VoiceActivityDetector](D:/Codes/Projects/FORRESUME/TranslationApp/app/src/main/java/com/vocalingo/app/audio/VoiceActivityDetector.kt)
- [Segmenter](D:/Codes/Projects/FORRESUME/TranslationApp/backend/src/segmentChunker.js)
- [Server pipeline](D:/Codes/Projects/FORRESUME/TranslationApp/backend/src/server.js)
- [Google pipeline](D:/Codes/Projects/FORRESUME/TranslationApp/backend/src/googlePipeline.js)
- [Backend README](D:/Codes/Projects/FORRESUME/TranslationApp/backend/README.md)
