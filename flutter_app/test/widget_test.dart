import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_app/main.dart';

void main() {
  testWidgets('renders translator controls', (tester) async {
    await tester.pumpWidget(const TranslationApp());

    expect(find.text('Left speaker'), findsOneWidget);
    expect(find.text('Right speaker'), findsOneWidget);
    expect(find.byIcon(Icons.translate), findsNWidgets(2));
  });
}
