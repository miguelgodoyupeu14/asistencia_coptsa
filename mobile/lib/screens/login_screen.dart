import 'package:flutter/material.dart';
import '../core/auth_controller.dart';

class LoginScreen extends StatefulWidget {
  final AuthController auth;
  const LoginScreen({super.key, required this.auth});
  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final user = TextEditingController(), password = TextEditingController();
  bool busy = false;
  String? error;
  @override
  void dispose() {
    user.dispose();
    password.dispose();
    super.dispose();
  }

  Future<void> submit() async {
    if (user.text.trim().isEmpty || password.text.isEmpty) return;
    setState(() => busy = true);
    try {
      await widget.auth.login(user.text.trim(), password.text);
    } catch (e) {
      if (mounted) setState(() => error = 'No fue posible iniciar sesión.');
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: Card(
                child: Padding(
                  padding: const EdgeInsets.all(24),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      const CircleAvatar(radius: 28, child: Text('SA')),
                      const SizedBox(height: 20),
                      Text(
                        'Mi Asistencia',
                        style: Theme.of(context).textTheme.headlineMedium,
                      ),
                      const Text('Accede con tus credenciales corporativas.'),
                      const SizedBox(height: 24),
                      TextField(
                        controller: user,
                        autofillHints: const [AutofillHints.username],
                        decoration: const InputDecoration(labelText: 'Usuario'),
                      ),
                      const SizedBox(height: 12),
                      TextField(
                        controller: password,
                        obscureText: true,
                        onSubmitted: (_) => submit(),
                        decoration: const InputDecoration(
                          labelText: 'Contraseña',
                        ),
                      ),
                      ...[
                        if (error != null)
                          Padding(
                            padding: const EdgeInsets.only(top: 12),
                            child: Text(
                              error!,
                              style: TextStyle(
                                color: Theme.of(context).colorScheme.error,
                              ),
                            ),
                          ),
                      ],
                      const SizedBox(height: 20),
                      FilledButton(
                        onPressed: busy ? null : submit,
                        child: Text(busy ? 'Ingresando…' : 'Iniciar sesión'),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
