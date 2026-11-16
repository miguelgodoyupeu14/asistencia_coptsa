import 'package:flutter/material.dart';
import '../core/auth_controller.dart';

class HistoryScreen extends StatefulWidget {
  final AuthController auth;
  const HistoryScreen({super.key, required this.auth});
  @override
  State<HistoryScreen> createState() => _HistoryScreenState();
}

class _HistoryScreenState extends State<HistoryScreen> {
  List<dynamic> rows = [];
  bool loading = true;
  @override
  void initState() {
    super.initState();
    load();
  }

  Future<void> load() async {
    final now = DateTime.now(), from = now.subtract(const Duration(days: 30));
    try {
      final data =
          await widget.auth.api.get(
                '/attendance/me',
                query: {'from': date(from), 'to': date(now)},
              )
              as List;
      if (mounted) setState(() => rows = data);
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  String date(DateTime d) => d.toIso8601String().substring(0, 10);
  @override
  Widget build(BuildContext context) {
    if (loading) return const Center(child: CircularProgressIndicator());
    return RefreshIndicator(
      onRefresh: load,
      child: ListView.builder(
        padding: const EdgeInsets.all(12),
        itemCount: rows.length,
        itemBuilder: (context, i) {
          final r = rows[i] as Map<String, dynamic>;
          return Card(
            child: ListTile(
              title: Text(r['workDate'].toString()),
              subtitle: Text(
                '${r['workedMinutes'] ?? 0} min · Tardanza ${r['lateMinutes'] ?? 0} min',
              ),
              trailing: Chip(label: Text(r['status'].toString())),
            ),
          );
        },
      ),
    );
  }
}
