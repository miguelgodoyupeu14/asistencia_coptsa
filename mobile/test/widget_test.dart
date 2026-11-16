import 'package:asistencia_mobile/core/auth_controller.dart';
import 'package:asistencia_mobile/screens/login_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('login screen contains credential fields', (tester) async {
    final auth = AuthController();
    await tester.pumpWidget(MaterialApp(home: LoginScreen(auth: auth)));
    expect(find.text('Mi Asistencia'), findsOneWidget);
    expect(find.text('Usuario'), findsOneWidget);
    expect(find.text('Contraseña'), findsOneWidget);
    expect(find.text('Iniciar sesión'), findsOneWidget);
    auth.dispose();
  });
}
