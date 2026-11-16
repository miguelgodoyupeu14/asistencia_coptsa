import 'dart:convert';
import 'package:http/http.dart' as http;

class ApiException implements Exception {
  final int status;
  final String message;
  ApiException(this.status, this.message);
  @override
  String toString() => message;
}

class ApiClient {
  static const baseUrl = String.fromEnvironment(
    'API_URL',
    defaultValue: 'http://10.0.2.2:8080/api/v1',
  );
  final String? Function() tokenProvider;
  ApiClient(this.tokenProvider);
  Map<String, String> get headers => {
    'Content-Type': 'application/json',
    if (tokenProvider() != null) 'Authorization': 'Bearer ${tokenProvider()}',
  };
  Future<dynamic> get(String path, {Map<String, String>? query}) async {
    final uri = Uri.parse('$baseUrl$path').replace(queryParameters: query);
    return _decode(await http.get(uri, headers: headers));
  }

  Future<dynamic> post(String path, Map<String, dynamic> body) async => _decode(
    await http.post(
      Uri.parse('$baseUrl$path'),
      headers: headers,
      body: jsonEncode(body),
    ),
  );
  dynamic _decode(http.Response response) {
    dynamic data;
    try {
      data = response.body.isEmpty
          ? null
          : jsonDecode(utf8.decode(response.bodyBytes));
    } catch (_) {
      data = null;
    }
    if (response.statusCode < 200 || response.statusCode >= 300) {
      final message = data is Map
          ? (data['detail'] ?? data['message'] ?? 'Error del servidor')
                .toString()
          : 'Error del servidor';
      throw ApiException(response.statusCode, message);
    }
    return data;
  }
}
