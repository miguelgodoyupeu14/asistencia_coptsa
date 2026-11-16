# Aplicación móvil

Cliente Flutter orientado al empleado.

## Desarrollo

El emulador Android usa por defecto `http://10.0.2.2:8080/api/v1`. Para otro servidor:

```shell
flutter run --dart-define=API_URL=https://servidor.example/api/v1
```

El APK de piloto permite HTTP para probar contra un servidor de la red local. Una versión de producción debe usar HTTPS y retirar `usesCleartextTraffic`.

## Funciones del piloto

- Login JWT y token en almacenamiento seguro.
- Perfil y horario vigente.
- Identificador aleatorio persistente del dispositivo.
- Solicitud de aprobación del dispositivo.
- GPS de alta precisión y envío de coordenadas.
- Lectura del BSSID de Wi‑Fi en Android.
- Escaneo de QR dinámico.
- Entrada, salida, historial y solicitudes.

Compilación para un servidor local:

```shell
flutter build apk --debug --dart-define=API_URL=http://192.168.1.11:4200/api/v1
```

La IP queda incorporada en el APK. Si cambia la IP del servidor, hay que volver a compilar o publicar la API con un dominio estable.

## Validación

```shell
flutter analyze
flutter test
flutter build apk --debug
```

La generación Android requiere aceptar previamente las licencias mediante `flutter doctor --android-licenses`.
