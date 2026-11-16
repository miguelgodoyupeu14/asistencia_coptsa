import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
import 'package:network_info_plus/network_info_plus.dart';
import '../core/auth_controller.dart';
import 'qr_scanner_screen.dart';

class AttendanceScreen extends StatefulWidget {
  final AuthController auth;
  const AttendanceScreen({super.key, required this.auth});
  @override
  State<AttendanceScreen> createState() => _AttendanceScreenState();
}

class _AttendanceScreenState extends State<AttendanceScreen> {
  bool busy = false;
  String? message, qrCode, wifiName, wifiBssid;
  Map<String, dynamic>? policy;

  bool get needsGps => policy?['site']?['requireGeofence'] == true;
  bool get needsDevice => policy?['site']?['requireRegisteredDevice'] == true;
  bool get needsQr => policy?['site']?['requireDynamicQr'] == true;
  bool get needsWifi => policy?['site']?['requireWifi'] == true;
  List<dynamic> get allowed =>
      policy?['allowedActions'] as List<dynamic>? ?? const [];

  @override
  void initState() {
    super.initState();
    loadContext();
  }

  Future<void> loadContext() async {
    if (mounted) setState(() => busy = true);
    try {
      final data = await widget.auth.api.get('/attendance/context');
      if (!mounted) return;
      setState(() {
        policy = Map<String, dynamic>.from(data as Map);
        message = null;
      });
      if (needsWifi) await loadWifi();
    } catch (e) {
      if (mounted) setState(() => message = e.toString());
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  Future<void> loadWifi() async {
    try {
      final info = NetworkInfo();
      final name = await info.getWifiName();
      final bssid = await info.getWifiBSSID();
      if (mounted) {
        setState(() {
          wifiName = name;
          wifiBssid = bssid;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          wifiName = null;
          wifiBssid = null;
        });
      }
    }
  }

  Future<Position?> location() async {
    if (!await Geolocator.isLocationServiceEnabled()) return null;
    var permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
    }
    if (permission == LocationPermission.denied ||
        permission == LocationPermission.deniedForever) {
      return null;
    }
    return Geolocator.getCurrentPosition(
      locationSettings: const LocationSettings(
        accuracy: LocationAccuracy.high,
        timeLimit: Duration(seconds: 10),
      ),
    );
  }

  Future<void> registerDevice() async {
    setState(() => busy = true);
    try {
      final id = await widget.auth.deviceIdentifier();
      await widget.auth.api.post('/attendance-security/devices', {
        'identifier': id,
        'displayName': 'Dispositivo móvil',
        'platform': 'ANDROID',
      });
      if (mounted) {
        setState(
          () => message =
              'Dispositivo enviado para aprobación. Un administrador debe aprobarlo.',
        );
      }
    } catch (e) {
      if (mounted) setState(() => message = e.toString());
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  Future<void> scan() async {
    final value = await Navigator.push<String>(
      context,
      MaterialPageRoute(builder: (_) => const QrScannerScreen()),
    );
    if (value != null && mounted) setState(() => qrCode = value);
  }

  Future<void> punch(String type) async {
    setState(() {
      busy = true;
      message = 'Validando con el servidor…';
    });
    try {
      final id = needsDevice ? await widget.auth.deviceIdentifier() : null;
      final position = needsGps ? await location() : null;
      if (needsGps && position == null) {
        throw Exception(
          'Activa la ubicación y concede permiso para poder marcar.',
        );
      }
      if (needsQr && qrCode == null) {
        throw Exception('Escanea el QR vigente de tu sede antes de marcar.');
      }
      if (needsWifi) await loadWifi();
      if (needsWifi && wifiBssid == null) {
        throw Exception('No se pudo identificar la red Wi-Fi requerida.');
      }
      final data =
          await widget.auth.api.post('/attendance/punch', {
                'type': type,
                'channel': 'ANDROID',
                'deviceIdentifier': id,
                'latitude': position?.latitude,
                'longitude': position?.longitude,
                'accuracyMeters': position?.accuracy,
                'qrCode': needsQr ? qrCode : null,
                'wifiBssid': needsWifi ? wifiBssid : null,
              })
              as Map<String, dynamic>;
      if (!mounted) return;
      final late = data['lateMinutes'] as num? ?? 0;
      final early = data['earlyExitMinutes'] as num? ?? 0;
      setState(() {
        message = type == 'ENTRY'
            ? (late > 0
                  ? 'Entrada registrada con $late min de tardanza.'
                  : 'Entrada registrada a tiempo.')
            : (early > 0
                  ? 'Salida registrada $early min antes del horario.'
                  : 'Salida registrada correctamente.');
        qrCode = null;
      });
      await loadContext();
    } catch (e) {
      if (mounted) {
        setState(() => message = e.toString().replaceFirst('Exception: ', ''));
      }
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final site = policy?['site'] as Map<String, dynamic>?;
    final shift = policy?['shift'] as Map<String, dynamic>?;
    return RefreshIndicator(
      onRefresh: loadContext,
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.all(20),
        children: [
          const Icon(Icons.fingerprint, size: 72, color: Color(0xff1f6fd1)),
          const SizedBox(height: 8),
          Text(
            'Registro de asistencia',
            textAlign: TextAlign.center,
            style: Theme.of(context).textTheme.headlineSmall,
          ),
          const Text(
            'La hora oficial y todas las reglas se validan en el servidor.',
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 20),
          if (policy != null)
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      site?['name']?.toString() ?? 'Sede',
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                    const SizedBox(height: 6),
                    Text('Fecha laboral: ${policy?['workDate']}'),
                    Text(
                      shift == null
                          ? 'Día no laborable'
                          : 'Horario: ${shift['startTime']} – ${shift['endTime']}${shift['crossesMidnight'] == true ? ' (nocturno)' : ''}',
                    ),
                    Text(
                      'Estado: ${policy?['attendance']?['status'] ?? 'SIN MARCACIÓN'}',
                    ),
                  ],
                ),
              ),
            ),
          if (needsWifi)
            Card(
              child: ListTile(
                leading: const Icon(Icons.wifi),
                title: Text(wifiName ?? 'Wi-Fi no identificado'),
                subtitle: Text(wifiBssid ?? 'BSSID no disponible'),
                trailing: IconButton(
                  onPressed: busy ? null : loadWifi,
                  icon: const Icon(Icons.refresh),
                ),
              ),
            ),
          if (needsDevice)
            OutlinedButton.icon(
              onPressed: busy ? null : registerDevice,
              icon: const Icon(Icons.phone_android),
              label: const Text('Registrar este dispositivo'),
            ),
          if (needsQr) ...[
            const SizedBox(height: 10),
            OutlinedButton.icon(
              onPressed: busy ? null : scan,
              icon: const Icon(Icons.qr_code_scanner),
              label: Text(
                qrCode == null ? 'Escanear QR vigente' : 'QR escaneado',
              ),
            ),
          ],
          const SizedBox(height: 18),
          FilledButton.icon(
            onPressed: busy || !allowed.contains('ENTRY')
                ? null
                : () => punch('ENTRY'),
            icon: const Icon(Icons.login),
            label: const Text('Registrar entrada'),
          ),
          const SizedBox(height: 10),
          OutlinedButton.icon(
            onPressed: busy || !allowed.contains('EXIT')
                ? null
                : () => punch('EXIT'),
            icon: const Icon(Icons.logout),
            label: const Text('Registrar salida'),
          ),
          if (busy)
            const Padding(
              padding: EdgeInsets.only(top: 16),
              child: LinearProgressIndicator(),
            ),
          if (message != null)
            Padding(
              padding: const EdgeInsets.only(top: 18),
              child: Semantics(
                liveRegion: true,
                child: Text(message!, textAlign: TextAlign.center),
              ),
            ),
        ],
      ),
    );
  }
}
