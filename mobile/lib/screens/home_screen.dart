import 'package:flutter/material.dart';
import '../core/auth_controller.dart';
import 'attendance_screen.dart';
import 'history_screen.dart';
import 'requests_screen.dart';

class HomeScreen extends StatefulWidget {
  final AuthController auth;
  const HomeScreen({super.key, required this.auth});
  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int index = 0;
  late final pages = [
    Overview(auth: widget.auth),
    AttendanceScreen(auth: widget.auth),
    HistoryScreen(auth: widget.auth),
    RequestsScreen(auth: widget.auth),
  ];
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Mi Asistencia'),
        actions: [
          IconButton(
            onPressed: widget.auth.logout,
            icon: const Icon(Icons.logout),
            tooltip: 'Cerrar sesión',
          ),
        ],
      ),
      body: IndexedStack(index: index, children: pages),
      bottomNavigationBar: NavigationBar(
        selectedIndex: index,
        onDestinationSelected: (value) => setState(() => index = value),
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.home_outlined),
            selectedIcon: Icon(Icons.home),
            label: 'Inicio',
          ),
          NavigationDestination(icon: Icon(Icons.fingerprint), label: 'Marcar'),
          NavigationDestination(icon: Icon(Icons.history), label: 'Historial'),
          NavigationDestination(
            icon: Icon(Icons.description_outlined),
            label: 'Solicitudes',
          ),
        ],
      ),
    );
  }
}

class Overview extends StatefulWidget {
  final AuthController auth;
  const Overview({super.key, required this.auth});
  @override
  State<Overview> createState() => _OverviewState();
}

class _OverviewState extends State<Overview> {
  Map<String, dynamic>? profile, schedule;
  @override
  void initState() {
    super.initState();
    load();
  }

  Future<void> load() async {
    try {
      final results = await Future.wait([
        widget.auth.api.get('/profile/me'),
        widget.auth.api.get('/schedules/me'),
      ]);
      if (mounted) {
        setState(() {
          profile = results[0] as Map<String, dynamic>;
          schedule = results[1] as Map<String, dynamic>;
        });
      }
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    return RefreshIndicator(
      onRefresh: load,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Text(
            'Hola, ${profile?['firstNames'] ?? widget.auth.username ?? ''}',
            style: Theme.of(context).textTheme.headlineSmall,
          ),
          const SizedBox(height: 16),
          Card(
            child: ListTile(
              leading: const Icon(Icons.badge_outlined),
              title: Text(profile?['code'] ?? 'Perfil de empleado'),
              subtitle: Text(
                '${profile?['firstNames'] ?? ''} ${profile?['lastNames'] ?? ''}',
              ),
            ),
          ),
          Card(
            child: ListTile(
              leading: const Icon(Icons.schedule),
              title: Text(schedule?['name'] ?? 'Sin horario vigente'),
              subtitle: Text(
                '${(schedule?['shifts'] as List?)?.length ?? 0} turnos configurados',
              ),
            ),
          ),
        ],
      ),
    );
  }
}
