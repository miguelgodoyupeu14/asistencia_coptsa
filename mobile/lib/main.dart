import 'package:flutter/material.dart';
import 'core/auth_controller.dart';
import 'screens/home_screen.dart';
import 'screens/login_screen.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const AttendanceApp());
}

class AttendanceApp extends StatefulWidget {
  const AttendanceApp({super.key});
  @override
  State<AttendanceApp> createState() => _AttendanceAppState();
}

class _AttendanceAppState extends State<AttendanceApp> {
  final auth = AuthController();
  @override
  void initState() {
    super.initState();
    auth.restore();
  }

  @override
  void dispose() {
    auth.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: auth,
      builder: (context, _) {
        return MaterialApp(
          debugShowCheckedModeBanner: false,
          title: 'Mi Asistencia',
          theme: ThemeData(
            colorScheme: ColorScheme.fromSeed(
              seedColor: const Color(0xff1f6fd1),
            ),
            useMaterial3: true,
            scaffoldBackgroundColor: const Color(0xfff4f6f9),
            inputDecorationTheme: const InputDecorationTheme(
              border: OutlineInputBorder(),
            ),
          ),
          home: auth.loading
              ? const Scaffold(body: Center(child: CircularProgressIndicator()))
              : auth.authenticated
              ? HomeScreen(auth: auth)
              : LoginScreen(auth: auth),
        );
      },
    );
  }
}
