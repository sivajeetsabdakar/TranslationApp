import AVFoundation
import Flutter
import Speech
import UIKit

@main
@objc class AppDelegate: FlutterAppDelegate {
  private let speechStreamHandler = SpeechStreamHandler()
  private let speechRecognizerBridge = IOSSpeechRecognizer()
  private let speechPlayer = PannedSpeechPlayer()

  override func application(
    _ application: UIApplication,
    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
  ) -> Bool {
    GeneratedPluginRegistrant.register(with: self)

    guard let controller = window?.rootViewController as? FlutterViewController else {
      return super.application(application, didFinishLaunchingWithOptions: launchOptions)
    }

    let nativeChannel = FlutterMethodChannel(
      name: "translation_app/native",
      binaryMessenger: controller.binaryMessenger
    )
    nativeChannel.setMethodCallHandler { [weak self] call, result in
      self?.handleNativeCall(call, result: result)
    }

    let eventChannel = FlutterEventChannel(
      name: "translation_app/speech_events",
      binaryMessenger: controller.binaryMessenger
    )
    eventChannel.setStreamHandler(speechStreamHandler)

    return super.application(application, didFinishLaunchingWithOptions: launchOptions)
  }

  private func handleNativeCall(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    switch call.method {
    case "speechStatus":
      result([
        "apiKeyConfigured": true,
        "iosSpeechStreamingImplemented": true,
        "speechProvider": "Apple Speech"
      ])

    case "speak":
      guard
        let arguments = call.arguments as? [String: Any],
        let text = arguments["text"] as? String
      else {
        result(FlutterError(code: "BAD_ARGS", message: "Missing text.", details: nil))
        return
      }

      let localeTag = arguments["localeTag"] as? String ?? "en-US"
      let pan = arguments["pan"] as? Double ?? 0.0
      speechPlayer.speak(text: text, localeTag: localeTag, pan: Float(pan))
      result(nil)

    case "startListening":
      let arguments = call.arguments as? [String: Any]
      let languageCode = arguments?["languageCode"] as? String ?? "en-US"
      speechRecognizerBridge.start(languageCode: languageCode) { [weak self] event in
        self?.speechStreamHandler.send(event)
      } completion: { error in
        if let error {
          result(FlutterError(code: "IOS_SPEECH", message: error, details: nil))
        } else {
          result(nil)
        }
      }

    case "stopListening":
      speechRecognizerBridge.stop()
      result(nil)

    default:
      result(FlutterMethodNotImplemented)
    }
  }
}

final class SpeechStreamHandler: NSObject, FlutterStreamHandler {
  private var eventSink: FlutterEventSink?

  func onListen(withArguments arguments: Any?, eventSink events: @escaping FlutterEventSink) -> FlutterError? {
    eventSink = events
    return nil
  }

  func onCancel(withArguments arguments: Any?) -> FlutterError? {
    eventSink = nil
    return nil
  }

  func send(_ event: [String: String]) {
    DispatchQueue.main.async { [weak self] in
      self?.eventSink?(event)
    }
  }
}

final class IOSSpeechRecognizer {
  private let audioEngine = AVAudioEngine()
  private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest?
  private var recognitionTask: SFSpeechRecognitionTask?
  private var speechRecognizer: SFSpeechRecognizer?

  func start(
    languageCode: String,
    onEvent: @escaping ([String: String]) -> Void,
    completion: @escaping (String?) -> Void
  ) {
    stop()

    SFSpeechRecognizer.requestAuthorization { [weak self] speechStatus in
      guard speechStatus == .authorized else {
        completion("Speech recognition permission is required.")
        return
      }

      AVAudioSession.sharedInstance().requestRecordPermission { allowed in
        guard allowed else {
          completion("Microphone permission is required.")
          return
        }

        DispatchQueue.main.async {
          self?.startAudioRecognition(
            languageCode: languageCode,
            onEvent: onEvent,
            completion: completion
          )
        }
      }
    }
  }

  func stop() {
    recognitionTask?.cancel()
    recognitionTask = nil
    recognitionRequest?.endAudio()
    recognitionRequest = nil

    if audioEngine.isRunning {
      audioEngine.stop()
    }
    audioEngine.inputNode.removeTap(onBus: 0)

    try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
  }

  private func startAudioRecognition(
    languageCode: String,
    onEvent: @escaping ([String: String]) -> Void,
    completion: @escaping (String?) -> Void
  ) {
    let locale = Locale(identifier: languageCode)
    guard let recognizer = SFSpeechRecognizer(locale: locale), recognizer.isAvailable else {
      completion("Speech recognition is not available for \(languageCode).")
      return
    }

    speechRecognizer = recognizer

    let session = AVAudioSession.sharedInstance()
    do {
      try session.setCategory(.record, mode: .measurement, options: [.duckOthers])
      try session.setActive(true, options: .notifyOthersOnDeactivation)
    } catch {
      completion("Could not start microphone session: \(error.localizedDescription)")
      return
    }

    let request = SFSpeechAudioBufferRecognitionRequest()
    request.shouldReportPartialResults = true
    recognitionRequest = request

    let inputNode = audioEngine.inputNode
    let recordingFormat = inputNode.outputFormat(forBus: 0)
    inputNode.removeTap(onBus: 0)
    inputNode.installTap(onBus: 0, bufferSize: 1024, format: recordingFormat) { buffer, _ in
      request.append(buffer)
    }

    audioEngine.prepare()
    do {
      try audioEngine.start()
    } catch {
      completion("Could not start microphone: \(error.localizedDescription)")
      stop()
      return
    }

    recognitionTask = recognizer.recognitionTask(with: request) { [weak self] result, error in
      if let result {
        let text = result.bestTranscription.formattedString.trimmingCharacters(in: .whitespacesAndNewlines)
        if !text.isEmpty {
          onEvent([
            "type": result.isFinal ? "chunk" : "partial",
            "text": text
          ])
        }
      }

      if let error {
        onEvent([
          "type": "error",
          "text": "iOS speech failed: \(error.localizedDescription)"
        ])
        self?.stop()
      }
    }

    completion(nil)
  }
}

final class PannedSpeechPlayer {
  private let synthesizer = AVSpeechSynthesizer()
  private var player: AVAudioPlayer?

  func speak(text: String, localeTag: String, pan: Float) {
    guard !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
      return
    }

    player?.stop()
    player = nil

    if synthesizer.isSpeaking {
      synthesizer.stopSpeaking(at: .immediate)
    }

    let utterance = AVSpeechUtterance(string: text)
    utterance.voice = AVSpeechSynthesisVoice(language: localeTag)

    let fileURL = FileManager.default.temporaryDirectory
      .appendingPathComponent("tts-\(UUID().uuidString).caf")
    var outputFile: AVAudioFile?

    synthesizer.write(utterance) { [weak self] buffer in
      guard let self else { return }
      guard let pcmBuffer = buffer as? AVAudioPCMBuffer else { return }

      if pcmBuffer.frameLength == 0 {
        DispatchQueue.main.async {
          self.play(fileURL: fileURL, pan: pan)
        }
        return
      }

      do {
        if outputFile == nil {
          outputFile = try AVAudioFile(forWriting: fileURL, settings: pcmBuffer.format.settings)
        }
        try outputFile?.write(from: pcmBuffer)
      } catch {
        DispatchQueue.main.async {
          let fallback = AVSpeechUtterance(string: text)
          fallback.voice = AVSpeechSynthesisVoice(language: localeTag)
          self.synthesizer.speak(fallback)
        }
      }
    }
  }

  private func play(fileURL: URL, pan: Float) {
    do {
      let audioSession = AVAudioSession.sharedInstance()
      try audioSession.setCategory(.playback, mode: .spokenAudio, options: [.duckOthers])
      try audioSession.setActive(true)

      let audioPlayer = try AVAudioPlayer(contentsOf: fileURL)
      audioPlayer.pan = max(-1.0, min(1.0, pan))
      audioPlayer.prepareToPlay()
      audioPlayer.play()
      player = audioPlayer
    } catch {
      try? FileManager.default.removeItem(at: fileURL)
    }
  }
}
