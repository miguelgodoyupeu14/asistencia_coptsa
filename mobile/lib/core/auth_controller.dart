import 'dart:convert';
import 'dart:math';
import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'api_client.dart';

class AuthController extends ChangeNotifier {
  static const _key = 'access_token';
  static const _deviceKey = 'device_identifier';
  final storage = const FlutterSecureStorage();
  late final ApiClient api = ApiClient(() => token);
  String? token;
  String? username;
  bool loading = true;
  bool get authenticated => token != null;
  Future<void> restore() async {
    token = await storage.read(key: _key);
    if (token != null && !_valid(token!)) {
      await storage.delete(key: _key);
      token = null;
    }
    if (token != null) {
      username = _claims(token!)['preferred_username']?.toString();
    }
    loading = false;
    notifyListeners();
  }

  Future<void> login(String user, String password) async {
    final data =
        await api.post('/auth/login', {'username': user, 'password': password})
            as Map<String, dynamic>;
    token = data['accessToken'] as String;
    username = user;
    await storage.write(key: _key, value: token);
    notifyListeners();
  }

  Future<void> logout() async {
    token = null;
    username = null;
    await storage.delete(key: _key);
    notifyListeners();
  }

  Future<String> deviceIdentifier() async {
    var value = await storage.read(key: _deviceKey);
    if (value != null) return value;
    final bytes = List<int>.generate(32, (_) => Random.secure().nextInt(256));
    value = base64UrlEncode(bytes);
    await storage.write(key: _deviceKey, value: value);
    return value;
  }

  bool _valid(String value) {
    final exp = _claims(value)['exp'];
    return exp is num &&
        DateTime.fromMillisecondsSinceEpoch(
          exp.toInt() * 1000,
        ).isAfter(DateTime.now());
  }

  Map<String, dynamic> _claims(String value) {
    try {
      final normalized = base64Url.normalize(value.split('.')[1]);
      return jsonDecode(utf8.decode(base64Url.decode(normalized)))
          as Map<String, dynamic>;
    } catch (_) {
      return {};
    }
  }
}
