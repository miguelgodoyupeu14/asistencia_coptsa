import 'package:flutter/material.dart';
import '../core/auth_controller.dart';

class RequestsScreen extends StatefulWidget {
  final AuthController auth;
  const RequestsScreen({super.key, required this.auth});
  @override
  State<RequestsScreen> createState() => _RequestsScreenState();
}

class _RequestsScreenState extends State<RequestsScreen> {
  List<dynamic> rows = [];
  final reason = TextEditingController();
  String type = 'JUSTIFICATION';
  DateTime affectedDate = DateTime.now();
  String proposedPunchType = 'EXIT';
  DateTime? proposedAt, requestedFrom, requestedTo;
  bool saving = false;

  @override
  void initState() {
    super.initState();
    load();
  }

  @override
  void dispose() {
    reason.dispose();
    super.dispose();
  }

  Future<void> load() async {
    final data = await widget.auth.api.get('/requests/me') as List;
    if (mounted) setState(() => rows = data);
  }

  Future<DateTime?> pickDateTime(BuildContext context, DateTime initial) async {
    final date = await showDatePicker(
      context: context,
      firstDate: DateTime(2020),
      lastDate: DateTime.now().add(const Duration(days: 365)),
      initialDate: initial,
    );
    if (date == null || !context.mounted) return null;
    final time = await showTimePicker(
      context: context,
      initialTime: TimeOfDay.fromDateTime(initial),
    );
    return time == null
        ? null
        : DateTime(date.year, date.month, date.day, time.hour, time.minute);
  }

  String display(DateTime? value) {
    if (value == null) return 'Seleccionar';
    final local = value.toLocal();
    String two(int n) => n.toString().padLeft(2, '0');
    return '${two(local.day)}/${two(local.month)}/${local.year} ${two(local.hour)}:${two(local.minute)}';
  }

  Future<void> create(BuildContext sheetContext) async {
    if (reason.text.trim().isEmpty ||
        (type == 'CORRECTION' && proposedAt == null) ||
        (type == 'PERMISSION' &&
            (requestedFrom == null || requestedTo == null))) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Completa todos los datos obligatorios.')),
      );
      return;
    }
    if (type == 'PERMISSION' && !requestedTo!.isAfter(requestedFrom!)) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('El fin del permiso debe ser posterior al inicio.'),
        ),
      );
      return;
    }
    setState(() => saving = true);
    try {
      await widget.auth.api.post('/requests', {
        'type': type,
        'affectedDate': affectedDate.toIso8601String().substring(0, 10),
        'reason': reason.text.trim(),
        'requestedFrom': type == 'PERMISSION'
            ? requestedFrom!.toUtc().toIso8601String()
            : null,
        'requestedTo': type == 'PERMISSION'
            ? requestedTo!.toUtc().toIso8601String()
            : null,
        'proposedPunchType': type == 'CORRECTION' ? proposedPunchType : null,
        'proposedAt': type == 'CORRECTION'
            ? proposedAt!.toUtc().toIso8601String()
            : null,
      });
      reason.clear();
      await load();
      if (sheetContext.mounted) Navigator.pop(sheetContext);
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('Solicitud enviada.')));
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(e.toString())));
      }
    } finally {
      if (mounted) setState(() => saving = false);
    }
  }

  void openForm() {
    proposedAt ??= DateTime.now();
    requestedFrom ??= DateTime.now();
    requestedTo ??= DateTime.now().add(const Duration(hours: 8));
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      builder: (sheetContext) => StatefulBuilder(
        builder: (context, sheetSetState) => SingleChildScrollView(
          padding: EdgeInsets.fromLTRB(
            20,
            20,
            20,
            MediaQuery.viewInsetsOf(context).bottom + 20,
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                'Nueva solicitud',
                style: Theme.of(context).textTheme.titleLarge,
              ),
              const SizedBox(height: 14),
              DropdownButtonFormField<String>(
                initialValue: type,
                items: const [
                  DropdownMenuItem(
                    value: 'JUSTIFICATION',
                    child: Text('Justificación'),
                  ),
                  DropdownMenuItem(value: 'PERMISSION', child: Text('Permiso')),
                  DropdownMenuItem(
                    value: 'CORRECTION',
                    child: Text('Corrección de marcación'),
                  ),
                ],
                onChanged: (v) => sheetSetState(() => type = v!),
                decoration: const InputDecoration(labelText: 'Tipo'),
              ),
              ListTile(
                contentPadding: EdgeInsets.zero,
                leading: const Icon(Icons.calendar_today),
                title: const Text('Fecha afectada'),
                subtitle: Text(affectedDate.toIso8601String().substring(0, 10)),
                onTap: () async {
                  final value = await showDatePicker(
                    context: context,
                    firstDate: DateTime(2020),
                    lastDate: DateTime.now(),
                    initialDate: affectedDate,
                  );
                  if (value != null && context.mounted) {
                    sheetSetState(() => affectedDate = value);
                  }
                },
              ),
              if (type == 'CORRECTION') ...[
                DropdownButtonFormField<String>(
                  initialValue: proposedPunchType,
                  items: const [
                    DropdownMenuItem(
                      value: 'ENTRY',
                      child: Text('Entrada faltante'),
                    ),
                    DropdownMenuItem(
                      value: 'EXIT',
                      child: Text('Salida faltante'),
                    ),
                  ],
                  onChanged: (v) => sheetSetState(() => proposedPunchType = v!),
                  decoration: const InputDecoration(
                    labelText: 'Marcación a corregir',
                  ),
                ),
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: const Icon(Icons.schedule),
                  title: const Text('Fecha y hora propuestas'),
                  subtitle: Text(display(proposedAt)),
                  onTap: () async {
                    final value = await pickDateTime(context, proposedAt!);
                    if (value != null && context.mounted) {
                      sheetSetState(() => proposedAt = value);
                    }
                  },
                ),
              ],
              if (type == 'PERMISSION') ...[
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: const Icon(Icons.play_arrow),
                  title: const Text('Inicio del permiso'),
                  subtitle: Text(display(requestedFrom)),
                  onTap: () async {
                    final value = await pickDateTime(context, requestedFrom!);
                    if (value != null && context.mounted) {
                      sheetSetState(() => requestedFrom = value);
                    }
                  },
                ),
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: const Icon(Icons.stop),
                  title: const Text('Fin del permiso'),
                  subtitle: Text(display(requestedTo)),
                  onTap: () async {
                    final value = await pickDateTime(context, requestedTo!);
                    if (value != null && context.mounted) {
                      sheetSetState(() => requestedTo = value);
                    }
                  },
                ),
              ],
              TextField(
                controller: reason,
                maxLines: 3,
                decoration: const InputDecoration(labelText: 'Motivo *'),
              ),
              const SizedBox(height: 16),
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: saving ? null : () => create(sheetContext),
                  child: const Text('Enviar solicitud'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.transparent,
      floatingActionButton: FloatingActionButton(
        onPressed: openForm,
        child: const Icon(Icons.add),
      ),
      body: RefreshIndicator(
        onRefresh: load,
        child: ListView.builder(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.all(12),
          itemCount: rows.length,
          itemBuilder: (context, i) {
            final r = rows[i] as Map<String, dynamic>;
            return Card(
              child: ListTile(
                leading: const CircleAvatar(
                  child: Icon(Icons.description_outlined),
                ),
                title: Text('${r['type']} · ${r['affectedDate'] ?? ''}'),
                subtitle: Text(
                  '${r['reason']}${r['reviewComment'] == null ? '' : '\nRespuesta: ${r['reviewComment']}'}',
                ),
                trailing: Chip(label: Text(r['status'].toString())),
              ),
            );
          },
        ),
      ),
    );
  }
}
