import 'dart:async';
import 'dart:io';
import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:google_mlkit_translation/google_mlkit_translation.dart';
import 'package:permission_handler/permission_handler.dart';

void main() {
  runApp(const TranslationApp());
}

class TranslationApp extends StatelessWidget {
  const TranslationApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'TranslationApp',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF16347F)),
        scaffoldBackgroundColor: const Color(0xFFF3F6FD),
        useMaterial3: true,
      ),
      home: const TranslatorHomePage(),
    );
  }
}

class AppLanguage {
  const AppLanguage({
    required this.name,
    required this.translateLanguage,
    required this.cloudSpeechCode,
    required this.localeTag,
  });

  final String name;
  final TranslateLanguage translateLanguage;
  final String cloudSpeechCode;
  final String localeTag;
}

const supportedLanguages = <AppLanguage>[
  AppLanguage(
    name: 'English',
    translateLanguage: TranslateLanguage.english,
    cloudSpeechCode: 'en-US',
    localeTag: 'en-US',
  ),
  AppLanguage(
    name: 'Hindi',
    translateLanguage: TranslateLanguage.hindi,
    cloudSpeechCode: 'hi-IN',
    localeTag: 'hi-IN',
  ),
  AppLanguage(
    name: 'Bengali',
    translateLanguage: TranslateLanguage.bengali,
    cloudSpeechCode: 'bn-IN',
    localeTag: 'bn-IN',
  ),
  AppLanguage(
    name: 'Gujarati',
    translateLanguage: TranslateLanguage.gujarati,
    cloudSpeechCode: 'gu-IN',
    localeTag: 'gu-IN',
  ),
  AppLanguage(
    name: 'Kannada',
    translateLanguage: TranslateLanguage.kannada,
    cloudSpeechCode: 'kn-IN',
    localeTag: 'kn-IN',
  ),
  AppLanguage(
    name: 'Marathi',
    translateLanguage: TranslateLanguage.marathi,
    cloudSpeechCode: 'mr-IN',
    localeTag: 'mr-IN',
  ),
  AppLanguage(
    name: 'Tamil',
    translateLanguage: TranslateLanguage.tamil,
    cloudSpeechCode: 'ta-IN',
    localeTag: 'ta-IN',
  ),
  AppLanguage(
    name: 'Telugu',
    translateLanguage: TranslateLanguage.telugu,
    cloudSpeechCode: 'te-IN',
    localeTag: 'te-IN',
  ),
  AppLanguage(
    name: 'Urdu',
    translateLanguage: TranslateLanguage.urdu,
    cloudSpeechCode: 'ur-IN',
    localeTag: 'ur-IN',
  ),
  AppLanguage(
    name: 'Chinese',
    translateLanguage: TranslateLanguage.chinese,
    cloudSpeechCode: 'cmn-Hans-CN',
    localeTag: 'zh-CN',
  ),
  AppLanguage(
    name: 'French',
    translateLanguage: TranslateLanguage.french,
    cloudSpeechCode: 'fr-FR',
    localeTag: 'fr-FR',
  ),
  AppLanguage(
    name: 'Korean',
    translateLanguage: TranslateLanguage.korean,
    cloudSpeechCode: 'ko-KR',
    localeTag: 'ko-KR',
  ),
  AppLanguage(
    name: 'Spanish',
    translateLanguage: TranslateLanguage.spanish,
    cloudSpeechCode: 'es-ES',
    localeTag: 'es-ES',
  ),
  AppLanguage(
    name: 'German',
    translateLanguage: TranslateLanguage.german,
    cloudSpeechCode: 'de-DE',
    localeTag: 'de-DE',
  ),
  AppLanguage(
    name: 'Japanese',
    translateLanguage: TranslateLanguage.japanese,
    cloudSpeechCode: 'ja-JP',
    localeTag: 'ja-JP',
  ),
  AppLanguage(
    name: 'Arabic',
    translateLanguage: TranslateLanguage.arabic,
    cloudSpeechCode: 'ar-XA',
    localeTag: 'ar',
  ),
  AppLanguage(
    name: 'Portuguese',
    translateLanguage: TranslateLanguage.portuguese,
    cloudSpeechCode: 'pt-BR',
    localeTag: 'pt-BR',
  ),
  AppLanguage(
    name: 'Russian',
    translateLanguage: TranslateLanguage.russian,
    cloudSpeechCode: 'ru-RU',
    localeTag: 'ru-RU',
  ),
  AppLanguage(
    name: 'Italian',
    translateLanguage: TranslateLanguage.italian,
    cloudSpeechCode: 'it-IT',
    localeTag: 'it-IT',
  ),
  AppLanguage(
    name: 'Dutch',
    translateLanguage: TranslateLanguage.dutch,
    cloudSpeechCode: 'nl-NL',
    localeTag: 'nl-NL',
  ),
  AppLanguage(
    name: 'Turkish',
    translateLanguage: TranslateLanguage.turkish,
    cloudSpeechCode: 'tr-TR',
    localeTag: 'tr-TR',
  ),
  AppLanguage(
    name: 'Vietnamese',
    translateLanguage: TranslateLanguage.vietnamese,
    cloudSpeechCode: 'vi-VN',
    localeTag: 'vi-VN',
  ),
  AppLanguage(
    name: 'Thai',
    translateLanguage: TranslateLanguage.thai,
    cloudSpeechCode: 'th-TH',
    localeTag: 'th-TH',
  ),
  AppLanguage(
    name: 'Indonesian',
    translateLanguage: TranslateLanguage.indonesian,
    cloudSpeechCode: 'id-ID',
    localeTag: 'id-ID',
  ),
  AppLanguage(
    name: 'Malay',
    translateLanguage: TranslateLanguage.malay,
    cloudSpeechCode: 'ms-MY',
    localeTag: 'ms-MY',
  ),
  AppLanguage(
    name: 'Polish',
    translateLanguage: TranslateLanguage.polish,
    cloudSpeechCode: 'pl-PL',
    localeTag: 'pl-PL',
  ),
  AppLanguage(
    name: 'Ukrainian',
    translateLanguage: TranslateLanguage.ukrainian,
    cloudSpeechCode: 'uk-UA',
    localeTag: 'uk-UA',
  ),
  AppLanguage(
    name: 'Greek',
    translateLanguage: TranslateLanguage.greek,
    cloudSpeechCode: 'el-GR',
    localeTag: 'el-GR',
  ),
  AppLanguage(
    name: 'Hebrew',
    translateLanguage: TranslateLanguage.hebrew,
    cloudSpeechCode: 'he-IL',
    localeTag: 'he-IL',
  ),
  AppLanguage(
    name: 'Persian',
    translateLanguage: TranslateLanguage.persian,
    cloudSpeechCode: 'fa-IR',
    localeTag: 'fa-IR',
  ),
  AppLanguage(
    name: 'Swahili',
    translateLanguage: TranslateLanguage.swahili,
    cloudSpeechCode: 'sw-KE',
    localeTag: 'sw-KE',
  ),
  AppLanguage(
    name: 'Swedish',
    translateLanguage: TranslateLanguage.swedish,
    cloudSpeechCode: 'sv-SE',
    localeTag: 'sv-SE',
  ),
  AppLanguage(
    name: 'Danish',
    translateLanguage: TranslateLanguage.danish,
    cloudSpeechCode: 'da-DK',
    localeTag: 'da-DK',
  ),
  AppLanguage(
    name: 'Finnish',
    translateLanguage: TranslateLanguage.finnish,
    cloudSpeechCode: 'fi-FI',
    localeTag: 'fi-FI',
  ),
  AppLanguage(
    name: 'Norwegian',
    translateLanguage: TranslateLanguage.norwegian,
    cloudSpeechCode: 'no-NO',
    localeTag: 'no-NO',
  ),
  AppLanguage(
    name: 'Czech',
    translateLanguage: TranslateLanguage.czech,
    cloudSpeechCode: 'cs-CZ',
    localeTag: 'cs-CZ',
  ),
  AppLanguage(
    name: 'Romanian',
    translateLanguage: TranslateLanguage.romanian,
    cloudSpeechCode: 'ro-RO',
    localeTag: 'ro-RO',
  ),
  AppLanguage(
    name: 'Hungarian',
    translateLanguage: TranslateLanguage.hungarian,
    cloudSpeechCode: 'hu-HU',
    localeTag: 'hu-HU',
  ),
  AppLanguage(
    name: 'Bulgarian',
    translateLanguage: TranslateLanguage.bulgarian,
    cloudSpeechCode: 'bg-BG',
    localeTag: 'bg-BG',
  ),
  AppLanguage(
    name: 'Croatian',
    translateLanguage: TranslateLanguage.croatian,
    cloudSpeechCode: 'hr-HR',
    localeTag: 'hr-HR',
  ),
  AppLanguage(
    name: 'Slovak',
    translateLanguage: TranslateLanguage.slovak,
    cloudSpeechCode: 'sk-SK',
    localeTag: 'sk-SK',
  ),
  AppLanguage(
    name: 'Slovenian',
    translateLanguage: TranslateLanguage.slovenian,
    cloudSpeechCode: 'sl-SI',
    localeTag: 'sl-SI',
  ),
  AppLanguage(
    name: 'Catalan',
    translateLanguage: TranslateLanguage.catalan,
    cloudSpeechCode: 'ca-ES',
    localeTag: 'ca-ES',
  ),
  AppLanguage(
    name: 'Lithuanian',
    translateLanguage: TranslateLanguage.lithuanian,
    cloudSpeechCode: 'lt-LT',
    localeTag: 'lt-LT',
  ),
  AppLanguage(
    name: 'Latvian',
    translateLanguage: TranslateLanguage.latvian,
    cloudSpeechCode: 'lv-LV',
    localeTag: 'lv-LV',
  ),
  AppLanguage(
    name: 'Estonian',
    translateLanguage: TranslateLanguage.estonian,
    cloudSpeechCode: 'et-EE',
    localeTag: 'et-EE',
  ),
  AppLanguage(
    name: 'Welsh',
    translateLanguage: TranslateLanguage.welsh,
    cloudSpeechCode: 'cy-GB',
    localeTag: 'cy-GB',
  ),
  AppLanguage(
    name: 'Afrikaans',
    translateLanguage: TranslateLanguage.afrikaans,
    cloudSpeechCode: 'af-ZA',
    localeTag: 'af-ZA',
  ),
  AppLanguage(
    name: 'Tagalog',
    translateLanguage: TranslateLanguage.tagalog,
    cloudSpeechCode: 'fil-PH',
    localeTag: 'fil-PH',
  ),
];

class NativeBridge {
  static const _methodChannel = MethodChannel('translation_app/native');
  static const _speechEvents = EventChannel('translation_app/speech_events');

  static Stream<Map<String, dynamic>> speechEvents() {
    return _speechEvents.receiveBroadcastStream().map((event) {
      return Map<String, dynamic>.from(event as Map);
    });
  }

  static Future<void> speak({
    required String text,
    required String localeTag,
    required double pan,
  }) {
    return _methodChannel.invokeMethod<void>('speak', {
      'text': text,
      'localeTag': localeTag,
      'pan': pan,
    });
  }

  static Future<void> startListening(String languageCode) {
    return _methodChannel.invokeMethod<void>('startListening', {
      'languageCode': languageCode,
    });
  }

  static Future<void> stopListening() {
    return _methodChannel.invokeMethod<void>('stopListening');
  }

  static Future<Map<String, dynamic>> speechStatus() async {
    final result = await _methodChannel.invokeMethod<Map>('speechStatus');
    return Map<String, dynamic>.from(result ?? const {});
  }
}

class TranslatorHomePage extends StatefulWidget {
  const TranslatorHomePage({super.key});

  @override
  State<TranslatorHomePage> createState() => _TranslatorHomePageState();
}

class _TranslatorHomePageState extends State<TranslatorHomePage> {
  final _leftController = TextEditingController();
  final _rightController = TextEditingController();
  final _modelManager = OnDeviceTranslatorModelManager();

  StreamSubscription<Map<String, dynamic>>? _speechSubscription;
  AppLanguage _leftLanguage = supportedLanguages[0];
  AppLanguage _rightLanguage = supportedLanguages[1];
  String _leftText = 'Left text';
  String _rightText = 'Right text';
  String _status = 'TranslationApp';
  bool _isTranslating = false;
  bool? _activeListeningSide;

  @override
  void initState() {
    super.initState();
    _speechSubscription = NativeBridge.speechEvents().listen(
      _handleSpeechEvent,
    );
    _loadSpeechStatus();
  }

  @override
  void dispose() {
    _speechSubscription?.cancel();
    NativeBridge.stopListening();
    _leftController.dispose();
    _rightController.dispose();
    super.dispose();
  }

  Future<void> _loadSpeechStatus() async {
    try {
      final status = await NativeBridge.speechStatus();
      if (!mounted) return;
      setState(() {
        _status =
            Platform.isIOS && status['iosSpeechStreamingImplemented'] == true
            ? 'iOS speech recognition ready'
            : status['apiKeyConfigured'] == true
            ? 'Google Cloud real-time speech'
            : 'Google Cloud key missing';
      });
    } catch (_) {
      if (!mounted) return;
      setState(() => _status = 'Native speech bridge unavailable');
    }
  }

  Future<void> _handleManualTranslate(bool isLeft) async {
    final input = (isLeft ? _leftController.text : _rightController.text)
        .trim();
    if (input.isEmpty) {
      _showMessage('Please enter text to translate.');
      return;
    }

    await _translateAndSpeak(input, isLeft);
  }

  Future<void> _translateAndSpeak(String input, bool isLeft) async {
    final source = isLeft ? _leftLanguage : _rightLanguage;
    final target = isLeft ? _rightLanguage : _leftLanguage;

    setState(() {
      _isTranslating = true;
      _status = 'Translating ${source.name} to ${target.name}...';
    });

    try {
      final translatedText = await _translate(input, source, target);
      if (!mounted) return;

      setState(() {
        if (isLeft) {
          _rightText = translatedText;
        } else {
          _leftText = translatedText;
        }
        _status = 'Translation ready';
      });

      await NativeBridge.speak(
        text: translatedText,
        localeTag: target.localeTag,
        pan: _panForSide(isLeft),
      );
    } catch (error) {
      if (!mounted) return;
      setState(() => _status = 'Translation failed');
      _showMessage('Translation failed: $error');
    } finally {
      if (mounted) {
        setState(() => _isTranslating = false);
      }
    }
  }

  Future<String> _translate(
    String input,
    AppLanguage source,
    AppLanguage target,
  ) async {
    if (source.translateLanguage == target.translateLanguage) {
      return input;
    }

    final sourceModel = source.translateLanguage.bcpCode;
    final targetModel = target.translateLanguage.bcpCode;

    final sourceDownloaded = await _modelManager.isModelDownloaded(sourceModel);
    final targetDownloaded = await _modelManager.isModelDownloaded(targetModel);

    if ((!sourceDownloaded || !targetDownloaded) && mounted) {
      setState(() => _status = 'Downloading translation model...');
    }

    if (!sourceDownloaded) {
      await _modelManager.downloadModel(sourceModel);
    }
    if (!targetDownloaded) {
      await _modelManager.downloadModel(targetModel);
    }

    final translator = OnDeviceTranslator(
      sourceLanguage: source.translateLanguage,
      targetLanguage: target.translateLanguage,
    );
    try {
      return await translator.translateText(input);
    } finally {
      await translator.close();
    }
  }

  Future<void> _toggleListening(bool isLeft) async {
    if (_activeListeningSide == isLeft) {
      await NativeBridge.stopListening();
      setState(() {
        _activeListeningSide = null;
        _status = 'Listening stopped';
      });
      return;
    }

    final permission = await Permission.microphone.request();
    if (!permission.isGranted) {
      _showMessage('Microphone permission is required for speech.');
      return;
    }

    await NativeBridge.stopListening();
    final source = isLeft ? _leftLanguage : _rightLanguage;

    try {
      await NativeBridge.startListening(source.cloudSpeechCode);
      setState(() {
        _activeListeningSide = isLeft;
        _status = 'Listening in ${source.name}...';
      });
    } catch (error) {
      setState(() {
        _activeListeningSide = null;
        _status = 'Speech failed to start';
      });
      _showMessage(error.toString());
    }
  }

  void _handleSpeechEvent(Map<String, dynamic> event) {
    final type = event['type'] as String?;
    final text = event['text'] as String? ?? '';
    final isLeft = _activeListeningSide;
    if (isLeft == null || text.isEmpty) return;

    if (type == 'partial') {
      setState(() {
        if (isLeft) {
          _leftText = text;
        } else {
          _rightText = text;
        }
      });
      return;
    }

    if (type == 'chunk') {
      setState(() {
        if (isLeft) {
          _leftText = text;
        } else {
          _rightText = text;
        }
      });
      unawaited(_translateAndSpeak(text, isLeft));
      return;
    }

    if (type == 'error') {
      setState(() {
        _activeListeningSide = null;
        _status = text;
      });
      _showMessage(text);
    }
  }

  double _panForSide(bool isLeft) => isLeft ? -1.0 : 1.0;

  void _showMessage(String message) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(message)));
  }

  void _swapLanguages() {
    setState(() {
      final oldLeftLanguage = _leftLanguage;
      final oldLeftText = _leftText;
      final oldLeftInput = _leftController.text;

      _leftLanguage = _rightLanguage;
      _rightLanguage = oldLeftLanguage;
      _leftText = _rightText;
      _rightText = oldLeftText;
      _leftController.text = _rightController.text;
      _rightController.text = oldLeftInput;
    });
  }

  Future<void> _openSettings() async {
    await Navigator.of(
      context,
    ).push<void>(MaterialPageRoute(builder: (_) => const SpeechSettingsPage()));
    await _loadSpeechStatus();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(10),
          child: Column(
            children: [
              _TopBar(
                status: _status,
                isBusy: _isTranslating,
                onSettings: _openSettings,
                onSwap: _swapLanguages,
              ),
              Expanded(
                child: Transform.rotate(
                  angle: math.pi,
                  child: ConversationPanel(
                    speakerLabel: 'Right speaker',
                    language: _rightLanguage,
                    languages: supportedLanguages,
                    displayText: _rightText,
                    controller: _rightController,
                    hintText: 'Enter right text',
                    isListening: _activeListeningSide == false,
                    onLanguageChanged: (value) {
                      if (value == null) return;
                      setState(() => _rightLanguage = value);
                    },
                    onSpeak: () => _toggleListening(false),
                    onTranslate: () => _handleManualTranslate(false),
                  ),
                ),
              ),
              const SizedBox(height: 6),
              Expanded(
                child: ConversationPanel(
                  speakerLabel: 'Left speaker',
                  language: _leftLanguage,
                  languages: supportedLanguages,
                  displayText: _leftText,
                  controller: _leftController,
                  hintText: 'Enter left text',
                  isListening: _activeListeningSide == true,
                  onLanguageChanged: (value) {
                    if (value == null) return;
                    setState(() => _leftLanguage = value);
                  },
                  onSpeak: () => _toggleListening(true),
                  onTranslate: () => _handleManualTranslate(true),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _TopBar extends StatelessWidget {
  const _TopBar({
    required this.status,
    required this.isBusy,
    required this.onSettings,
    required this.onSwap,
  });

  final String status;
  final bool isBusy;
  final VoidCallback onSettings;
  final VoidCallback onSwap;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 44,
      child: Row(
        children: [
          Expanded(
            child: Text(
              status,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(
                color: Color(0xFF5D6A80),
                fontSize: 12,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
          if (isBusy)
            const SizedBox(
              height: 18,
              width: 18,
              child: CircularProgressIndicator(strokeWidth: 2),
            ),
          const SizedBox(width: 8),
          IconButton.filledTonal(
            tooltip: 'Swap languages',
            onPressed: onSwap,
            icon: const Icon(Icons.swap_vert),
          ),
          const SizedBox(width: 8),
          IconButton.filledTonal(
            tooltip: 'Speech settings',
            onPressed: onSettings,
            icon: const Icon(Icons.menu),
          ),
        ],
      ),
    );
  }
}

class ConversationPanel extends StatelessWidget {
  const ConversationPanel({
    super.key,
    required this.speakerLabel,
    required this.language,
    required this.languages,
    required this.displayText,
    required this.controller,
    required this.hintText,
    required this.isListening,
    required this.onLanguageChanged,
    required this.onSpeak,
    required this.onTranslate,
  });

  final String speakerLabel;
  final AppLanguage language;
  final List<AppLanguage> languages;
  final String displayText;
  final TextEditingController controller;
  final String hintText;
  final bool isListening;
  final ValueChanged<AppLanguage?> onLanguageChanged;
  final VoidCallback onSpeak;
  final VoidCallback onTranslate;

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(8),
        boxShadow: const [
          BoxShadow(
            color: Color(0x33000000),
            blurRadius: 8,
            offset: Offset(0, 3),
          ),
        ],
      ),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    speakerLabel,
                    style: const TextStyle(
                      color: Color(0xFF172033),
                      fontSize: 15,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                ),
                SizedBox(
                  width: 148,
                  height: 42,
                  child: DropdownButtonFormField<AppLanguage>(
                    initialValue: language,
                    isExpanded: true,
                    decoration: InputDecoration(
                      contentPadding: const EdgeInsets.symmetric(
                        horizontal: 10,
                        vertical: 8,
                      ),
                      filled: true,
                      fillColor: const Color(0xFFEFF3FA),
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(8),
                        borderSide: BorderSide.none,
                      ),
                    ),
                    items: languages
                        .map(
                          (item) => DropdownMenuItem<AppLanguage>(
                            value: item,
                            child: Text(item.name),
                          ),
                        )
                        .toList(),
                    onChanged: onLanguageChanged,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Expanded(
              child: Container(
                width: double.infinity,
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: const Color(0xFFF9FBFE),
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: const Color(0xFFD8DFEA)),
                ),
                child: SingleChildScrollView(
                  child: Text(
                    displayText,
                    style: const TextStyle(
                      color: Color(0xFF172033),
                      fontSize: 16,
                    ),
                  ),
                ),
              ),
            ),
            const SizedBox(height: 8),
            SizedBox(
              height: 48,
              child: TextField(
                controller: controller,
                textCapitalization: TextCapitalization.sentences,
                decoration: InputDecoration(
                  hintText: hintText,
                  filled: true,
                  fillColor: const Color(0xFFF9FBFE),
                  contentPadding: const EdgeInsets.symmetric(horizontal: 12),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(8),
                    borderSide: const BorderSide(color: Color(0xFFD8DFEA)),
                  ),
                ),
              ),
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: onSpeak,
                    icon: Icon(isListening ? Icons.stop : Icons.mic),
                    label: Text(isListening ? 'Stop' : 'Speak'),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: FilledButton.icon(
                    onPressed: onTranslate,
                    icon: const Icon(Icons.translate),
                    label: const Text('Translate'),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class SpeechSettingsPage extends StatefulWidget {
  const SpeechSettingsPage({super.key});

  @override
  State<SpeechSettingsPage> createState() => _SpeechSettingsPageState();
}

class _SpeechSettingsPageState extends State<SpeechSettingsPage> {
  Map<String, dynamic> _status = const {};

  @override
  void initState() {
    super.initState();
    _loadStatus();
  }

  Future<void> _loadStatus() async {
    final status = await NativeBridge.speechStatus();
    if (!mounted) return;
    setState(() => _status = status);
  }

  @override
  Widget build(BuildContext context) {
    final isIosSpeechReady =
        Platform.isIOS && _status['iosSpeechStreamingImplemented'] == true;
    final hasKey = _status['apiKeyConfigured'] == true;
    final isSpeechReady = isIosSpeechReady || hasKey;
    return Scaffold(
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(20),
          children: [
            Align(
              alignment: Alignment.centerLeft,
              child: OutlinedButton.icon(
                onPressed: () => Navigator.of(context).pop(),
                icon: const Icon(Icons.arrow_back),
                label: const Text('Back'),
              ),
            ),
            const SizedBox(height: 18),
            const Text(
              'Speech',
              style: TextStyle(fontSize: 28, fontWeight: FontWeight.w800),
            ),
            const SizedBox(height: 8),
            Text(
              isIosSpeechReady
                  ? 'Apple Speech configured'
                  : hasKey
                  ? 'API key configured'
                  : 'API key missing',
              style: const TextStyle(color: Color(0xFF5D6A80), fontSize: 15),
            ),
            const SizedBox(height: 18),
            _SettingsCard(
              title: 'Setup',
              body: isIosSpeechReady
                  ? 'Speech recognition uses the native iOS Speech framework.'
                  : isSpeechReady
                  ? 'Speech streaming is enabled for this debug build.'
                  : 'Add googleCloudSpeechApiKey=YOUR_KEY to local.properties and rebuild the debug app.',
            ),
            const SizedBox(height: 12),
            _SettingsCard(
              title: 'Languages',
              body: supportedLanguages.map((item) => item.name).join(', '),
            ),
            const SizedBox(height: 12),
            _SettingsCard(
              title: 'Platform',
              body: Platform.isIOS
                  ? 'iOS bridge handles native speech recognition and panned TTS playback.'
                  : 'Android bridge handles speech streaming and left/right TTS playback.',
            ),
          ],
        ),
      ),
    );
  }
}

class _SettingsCard extends StatelessWidget {
  const _SettingsCard({required this.title, required this.body});

  final String title;
  final String body;

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(8),
        boxShadow: const [
          BoxShadow(
            color: Color(0x22000000),
            blurRadius: 8,
            offset: Offset(0, 3),
          ),
        ],
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              title,
              style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w800),
            ),
            const SizedBox(height: 8),
            Text(
              body,
              style: const TextStyle(color: Color(0xFF5D6A80), fontSize: 14),
            ),
          ],
        ),
      ),
    );
  }
}
