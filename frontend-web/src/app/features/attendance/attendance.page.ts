import { DatePipe } from '@angular/common';
import { Component, ElementRef, OnDestroy, OnInit, ViewChild, signal } from '@angular/core';
import { BrowserQRCodeReader, IScannerControls } from '@zxing/browser';
import { ApiService } from '../../core/api.service';

interface Day {
  attendanceId: number;
  workDate: string;
  firstEntryAt: string | null;
  lastExitAt: string | null;
  workedMinutes: number;
  lateMinutes: number;
  earlyExitMinutes: number;
  status: string;
}
interface AttendanceContext {
  workDate: string;
  site: {
    name: string;
    requireGeofence: boolean;
    requireRegisteredDevice: boolean;
    requireDynamicQr: boolean;
    requireWifi: boolean;
  };
  shift: { startTime: string; endTime: string; crossesMidnight: boolean } | null;
  attendance: Day | null;
  allowedActions: ('ENTRY' | 'EXIT')[];
  requiredControls: { control: string; status: string }[];
  checkInAvailable: boolean;
  checkOutAvailable: boolean;
  blockingReason: string | null;
}

@Component({
  imports: [DatePipe],
  template: `
    <div class="page-head">
      <div>
        <h1>Mi asistencia</h1>
        <p>La hora oficial, ubicación y validaciones se comprueban en el servidor.</p>
      </div>
    </div>
    @if (message()) {
      <div class="notice" [class.error]="failed()">{{ message() }}</div>
    }
    <section class="punch-panel">
      <div class="punch-state">
        <span>{{ context()?.site?.name || 'Estado de asistencia' }}</span>
        <h2>
          {{ context()?.attendance ? status(context()!.attendance!.status) : 'Sin marcación' }}
        </h2>
        <p>
          @if (context()?.shift) {
            Horario {{ context()!.shift!.startTime }}–{{ context()!.shift!.endTime }} ·
          }
          {{ locationLabel() }}
        </p>
      </div>
      <div class="punch-tools">
        <div class="qr-tool">
          <span class="tool-label">Código de asistencia</span>
          @if (context()?.site?.requireDynamicQr) {
            <button
              type="button"
              class="scanner-button"
              (click)="openScanner()"
              [disabled]="saving()"
            >
              <span class="scanner-icon" aria-hidden="true">⌗</span
              ><span>{{ qrCode ? 'Volver a escanear' : 'Escanear QR con la cámara' }}</span>
            </button>
          }
          @if (qrCode) {
            <div class="qr-ready">
              <span aria-hidden="true">✓</span> QR leído y listo para validar
              <button type="button" (click)="clearQr()">Quitar</button>
            </div>
          }
        </div>
        @if (context()?.site?.requireRegisteredDevice) {
          <button class="secondary" (click)="registerDevice()" [disabled]="saving()">
            Registrar este navegador
          </button>
        }
      </div>
      <div class="control-list">
        @for (control of context()?.requiredControls || []; track control.control) {
          <span [class.required]="control.status !== 'NOT_REQUIRED'">
            <strong>{{ controlName(control.control) }}</strong
            >{{ controlStatus(control.status) }}
          </span>
        }
      </div>
      <div class="availability" [class.available]="!!context()?.allowedActions?.length">
        <strong>{{ actionTitle() }}</strong
        ><span>{{ actionExplanation() }}</span>
      </div>
      <div class="punch-actions">
        <button
          (click)="punch('ENTRY')"
          [disabled]="saving() || !context()?.checkInAvailable"
          [title]="buttonReason('ENTRY')"
        >
          Registrar entrada</button
        ><button
          class="secondary"
          (click)="punch('EXIT')"
          [disabled]="saving() || !context()?.checkOutAvailable"
          [title]="buttonReason('EXIT')"
        >
          Registrar salida
        </button>
      </div>
    </section>
    @if (scannerOpen()) {
      <div
        class="scanner-backdrop"
        role="dialog"
        aria-modal="true"
        aria-labelledby="scanner-title"
        (click)="closeScanner()"
      >
        <section class="scanner-dialog" (click)="$event.stopPropagation()">
          <div class="scanner-head">
            <div>
              <span>VALIDACIÓN DE ASISTENCIA</span>
              <h2 id="scanner-title">Escanear código QR</h2>
            </div>
            <button
              type="button"
              class="scanner-close"
              aria-label="Cerrar escáner"
              (click)="closeScanner()"
            >
              ×
            </button>
          </div>
          <div class="camera-frame">
            <video #qrVideo playsinline muted></video>
            <div class="scan-guide"><i></i><i></i><i></i><i></i></div>
            <div class="scan-line"></div>
          </div>
          <p>{{ scannerMessage() }}</p>
          <button type="button" class="secondary scanner-cancel" (click)="closeScanner()">
            Cancelar
          </button>
        </section>
      </div>
    }
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Fecha</th>
            <th>Entrada</th>
            <th>Salida</th>
            <th>Horas</th>
            <th>Tardanza</th>
            <th>Salida anticipada</th>
            <th>Estado</th>
          </tr>
        </thead>
        <tbody>
          @for (d of days(); track d.attendanceId) {
            <tr>
              <td>{{ d.workDate }}</td>
              <td>{{ d.firstEntryAt | date: 'shortTime' }}</td>
              <td>{{ d.lastExitAt | date: 'shortTime' }}</td>
              <td>{{ (d.workedMinutes / 60).toFixed(1) }} h</td>
              <td>{{ d.lateMinutes }} min</td>
              <td>{{ d.earlyExitMinutes }} min</td>
              <td>
                <span class="badge">{{ status(d.status) }}</span>
              </td>
            </tr>
          } @empty {
            <tr>
              <td colspan="7">Sin registros</td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `,
  styles: `
    .punch-panel {
      display: grid;
      grid-template-columns: 1.2fr 1fr;
      gap: 1rem;
      margin-bottom: 1.2rem;
      padding: 1.2rem;
      background: var(--white);
      border: 1px solid var(--line);
      border-radius: 18px;
      box-shadow: var(--shadow-sm);
    }
    .punch-state > span,
    .tool-label,
    .scanner-head span {
      color: var(--accent);
      font-size: 0.7rem;
      font-weight: 800;
      text-transform: uppercase;
      letter-spacing: 0.1em;
    }
    .punch-state h2 {
      margin: 0.35rem 0;
    }
    .punch-state p {
      margin: 0;
      color: var(--muted);
      font-size: 0.84rem;
    }
    .punch-tools {
      display: grid;
      gap: 0.7rem;
    }
    .qr-tool {
      display: grid;
      gap: 0.55rem;
    }
    .scanner-button {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 0.65rem;
      min-height: 50px;
      background: var(--blue);
      color: white;
    }
    .scanner-icon {
      font-size: 1.6rem;
      line-height: 1;
    }
    .qr-ready {
      display: flex;
      align-items: center;
      gap: 0.4rem;
      padding: 0.55rem 0.7rem;
      border-radius: 11px;
      background: rgba(38, 166, 120, 0.1);
      color: #16845f;
      font-size: 0.78rem;
      font-weight: 700;
    }
    .qr-ready > button {
      width: auto;
      min-height: 0;
      margin-left: auto;
      padding: 0.15rem 0.25rem;
      background: transparent;
      color: inherit;
      font-size: 0.74rem;
      text-decoration: underline;
    }
    .control-list {
      grid-column: 1/-1;
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      gap: 0.55rem;
    }
    .control-list span {
      display: grid;
      gap: 0.1rem;
      padding: 0.65rem 0.75rem;
      border: 1px solid var(--line);
      border-radius: 11px;
      color: var(--muted);
      font-size: 0.75rem;
    }
    .control-list span.required {
      color: var(--blue);
      background: color-mix(in srgb, var(--sky) 22%, var(--white));
    }
    .control-list strong {
      color: var(--navy);
      font-size: 0.72rem;
    }
    .availability {
      grid-column: 1/-1;
      display: flex;
      align-items: flex-start;
      gap: 0.35rem;
      flex-direction: column;
      padding: 0.8rem 0.9rem;
      border: 1px solid #ead7a3;
      border-radius: 12px;
      background: #fff8e8;
      color: #70551a;
    }
    .availability.available {
      border-color: #c7e6d8;
      background: #edf9f3;
      color: #176b4e;
    }
    .availability strong {
      font-size: 0.86rem;
    }
    .availability span {
      font-size: 0.79rem;
      line-height: 1.4;
    }
    .punch-actions {
      grid-column: 1/-1;
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 0.7rem;
    }
    .scanner-backdrop {
      position: fixed;
      inset: 0;
      z-index: 1000;
      display: grid;
      place-items: center;
      padding: 1rem;
      background: rgba(2, 17, 36, 0.78);
      backdrop-filter: blur(5px);
    }
    .scanner-dialog {
      width: min(100%, 440px);
      padding: 1rem;
      background: var(--white);
      border: 1px solid var(--line);
      border-radius: 22px;
      box-shadow: 0 24px 70px rgba(0, 0, 0, 0.35);
    }
    .scanner-head {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 1rem;
      margin-bottom: 0.8rem;
    }
    .scanner-head h2 {
      margin: 0.2rem 0 0;
    }
    .scanner-close {
      width: 42px;
      min-width: 42px;
      min-height: 42px;
      padding: 0;
      border-radius: 12px;
      background: var(--surface);
      color: var(--blue);
      font-size: 1.7rem;
      line-height: 1;
    }
    .camera-frame {
      position: relative;
      overflow: hidden;
      aspect-ratio: 1;
      border-radius: 17px;
      background: #020b17;
    }
    .camera-frame video {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    .scan-guide {
      position: absolute;
      inset: 14%;
    }
    .scan-guide i {
      position: absolute;
      width: 42px;
      height: 42px;
      border-color: #b3cfe5;
    }
    .scan-guide i:nth-child(1) {
      top: 0;
      left: 0;
      border-top: 4px solid;
      border-left: 4px solid;
    }
    .scan-guide i:nth-child(2) {
      top: 0;
      right: 0;
      border-top: 4px solid;
      border-right: 4px solid;
    }
    .scan-guide i:nth-child(3) {
      bottom: 0;
      left: 0;
      border-bottom: 4px solid;
      border-left: 4px solid;
    }
    .scan-guide i:nth-child(4) {
      right: 0;
      bottom: 0;
      border-right: 4px solid;
      border-bottom: 4px solid;
    }
    .scan-line {
      position: absolute;
      left: 15%;
      right: 15%;
      top: 50%;
      height: 2px;
      background: #71d8ff;
      box-shadow: 0 0 12px #71d8ff;
      animation: scan 2s ease-in-out infinite;
    }
    .scanner-dialog > p {
      margin: 0.8rem 0.25rem;
      color: var(--muted);
      text-align: center;
      font-size: 0.84rem;
    }
    .scanner-cancel {
      width: 100%;
    }
    @keyframes scan {
      0%,
      100% {
        transform: translateY(-110px);
      }
      50% {
        transform: translateY(110px);
      }
    }
    @media (max-width: 700px) {
      .punch-panel {
        grid-template-columns: 1fr;
      }
      .punch-actions {
        grid-column: 1;
        grid-template-columns: 1fr;
      }
      .punch-actions button {
        width: 100%;
      }
      .control-list {
        grid-template-columns: 1fr 1fr;
      }
      .scanner-backdrop {
        padding: 0.55rem;
      }
      .scanner-dialog {
        max-height: calc(100dvh - 1.1rem);
        overflow: auto;
      }
      .camera-frame {
        aspect-ratio: 4/3;
      }
    }
  `,
})
export class AttendancePage implements OnInit, OnDestroy {
  @ViewChild('qrVideo') qrVideo?: ElementRef<HTMLVideoElement>;
  readonly days = signal<Day[]>([]);
  readonly context = signal<AttendanceContext | null>(null);
  readonly saving = signal(false);
  readonly message = signal('');
  readonly failed = signal(false);
  readonly locationLabel = signal('Ubicación pendiente');
  readonly scannerOpen = signal(false);
  readonly scannerMessage = signal('Apunta la cámara al código mostrado por administración.');
  qrCode = '';
  private readonly qrReader = new BrowserQRCodeReader();
  private scannerControls?: IScannerControls;

  constructor(private api: ApiService) {}
  ngOnInit() {
    this.load();
  }
  ngOnDestroy() {
    this.stopScanner();
  }
  load() {
    const to = new Date(),
      from = new Date();
    from.setDate(to.getDate() - 30);
    this.api.get<AttendanceContext>('/attendance/context').subscribe({
      next: (r) => {
        this.context.set(r);
        this.locationLabel.set(
          r.site.requireGeofence ? 'Ubicación requerida al marcar' : 'Ubicación no requerida',
        );
      },
      error: (e) => this.fail(e),
    });
    this.api
      .get<Day[]>('/attendance/me', { from: iso(from), to: iso(to) })
      .subscribe({ next: (r) => this.days.set(r), error: () => {} });
  }
  registerDevice() {
    this.saving.set(true);
    this.api
      .post('/attendance-security/devices', {
        identifier: this.deviceId(),
        displayName: `Navegador ${navigator.platform || 'web'}`,
        platform: 'WEB',
      })
      .subscribe({
        next: () => this.done('Dispositivo enviado. Un administrador debe aprobarlo.'),
        error: (e) => this.fail(e),
      });
  }

  openScanner() {
    this.failed.set(false);
    if (!window.isSecureContext || !navigator.mediaDevices?.getUserMedia) {
      this.message.set('La cámara requiere abrir el sistema mediante HTTPS.');
      this.failed.set(true);
      return;
    }
    this.scannerMessage.set('Solicitando permiso para usar la cámara…');
    this.scannerOpen.set(true);
    setTimeout(() => void this.startScanner());
  }
  private async startScanner() {
    const video = this.qrVideo?.nativeElement;
    if (!video) return;
    try {
      this.scannerMessage.set('Apunta la cámara al código mostrado por administración.');
      this.scannerControls = await this.qrReader.decodeFromConstraints(
        { audio: false, video: { facingMode: { ideal: 'environment' } } },
        video,
        (result, _error, controls) => {
          if (!result || !this.scannerOpen()) return;
          this.qrCode = result.getText().trim();
          controls.stop();
          this.scannerOpen.set(false);
          this.message.set(
            'Código QR leído correctamente. Ya puedes registrar tu entrada o salida.',
          );
          this.failed.set(false);
        },
      );
    } catch {
      this.stopScanner();
      this.scannerOpen.set(false);
      this.message.set(
        'No se pudo abrir la cámara. Revisa el permiso de cámara de Safari e inténtalo nuevamente.',
      );
      this.failed.set(true);
    }
  }
  closeScanner() {
    this.stopScanner();
    this.scannerOpen.set(false);
  }
  clearQr() {
    this.qrCode = '';
  }
  private stopScanner() {
    this.scannerControls?.stop();
    this.scannerControls = undefined;
  }

  async punch(type: 'ENTRY' | 'EXIT') {
    this.saving.set(true);
    this.failed.set(false);
    const policy = this.context()?.site;
    if (policy?.requireWifi) {
      this.message.set(
        'Esta sede requiere validación Wi‑Fi disponible únicamente desde la aplicación Android.',
      );
      this.failed.set(true);
      this.saving.set(false);
      return;
    }
    let location: {
      latitude: number | null;
      longitude: number | null;
      accuracyMeters: number | null;
    } = {
      latitude: null,
      longitude: null,
      accuracyMeters: null,
    };
    if (policy?.requireGeofence) {
      try {
        location = await this.position();
      } catch {
        this.locationLabel.set('GPS no disponible en este navegador');
      }
    }
    this.api
      .post<any>('/attendance/punch', {
        type,
        channel: 'WEB',
        deviceIdentifier: policy?.requireRegisteredDevice ? this.deviceId() : null,
        latitude: location.latitude,
        longitude: location.longitude,
        accuracyMeters: location.accuracyMeters,
        qrCode: this.qrCode.trim() || null,
        wifiBssid: null,
      })
      .subscribe({
        next: (r) => {
          this.qrCode = '';
          const time = new Date(r.registeredAt).toLocaleTimeString([], {
            hour: '2-digit',
            minute: '2-digit',
          });
          const result =
            type === 'ENTRY'
              ? r.lateMinutes
                ? `Entrada ${time} · Tardanza de ${r.lateMinutes} min`
                : `Entrada ${time} · A tiempo`
              : `Salida ${time} · ${r.workedMinutes} min trabajados`;
          this.done(result);
          this.load();
        },
        error: (e) => this.fail(e),
      });
  }
  private position() {
    return new Promise<{ latitude: number; longitude: number; accuracyMeters: number }>(
      (resolve, reject) => {
        if (!navigator.geolocation) return reject();
        navigator.geolocation.getCurrentPosition(
          (p) => {
            this.locationLabel.set(`GPS listo · precisión ${Math.round(p.coords.accuracy)} m`);
            resolve({
              latitude: p.coords.latitude,
              longitude: p.coords.longitude,
              accuracyMeters: p.coords.accuracy,
            });
          },
          reject,
          { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 },
        );
      },
    );
  }
  private deviceId() {
    const key = 'asistencia.web.device';
    let id = localStorage.getItem(key);
    if (!id) {
      id = crypto.randomUUID?.() ?? `${Date.now()}-${Math.random()}`;
      localStorage.setItem(key, id);
    }
    return id;
  }
  private done(text: string) {
    this.message.set(text);
    this.failed.set(false);
    this.saving.set(false);
  }
  private fail(error: any) {
    const reasons: Record<string, string> = {
      EMPLOYEE_NOT_LINKED: 'Tu cuenta no está vinculada a un empleado.',
      EMPLOYEE_INACTIVE: 'El empleado está inactivo.',
      NO_ACTIVE_SITE_ASSIGNMENT: 'No tienes una sede vigente asignada.',
      SITE_ASSIGNMENT_OVERLAP: 'Tienes más de una sede vigente; administración debe corregirlo.',
      NO_ACTIVE_SCHEDULE: 'No tienes un horario vigente asignado.',
      SCHEDULE_OVERLAP: 'Tienes más de un horario vigente; administración debe corregirlo.',
      NON_WORKING_DAY: 'Hoy no es un día laborable en tu horario.',
      OUTSIDE_ENTRY_WINDOW: 'La entrada está fuera de la ventana permitida.',
      OUTSIDE_EXIT_WINDOW: 'La salida está fuera de la ventana permitida.',
      CHECK_IN_NOT_OPEN: 'La ventana para registrar la entrada todavía no ha comenzado.',
      CHECK_IN_WINDOW_CLOSED:
        'La ventana de entrada ya cerró. Presenta una solicitud de corrección.',
      CHECK_OUT_NOT_OPEN: 'La salida todavía no está disponible.',
      CHECK_OUT_WINDOW_CLOSED:
        'La ventana de salida ya cerró. Presenta una solicitud de corrección.',
      DUPLICATE_ENTRY: 'La entrada de hoy ya fue registrada.',
      EXIT_WITHOUT_ENTRY: 'Debes registrar una entrada antes de la salida.',
      DUPLICATE_EXIT: 'La salida de hoy ya fue registrada.',
      EXIT_BEFORE_ENTRY: 'La salida no puede ser anterior a la entrada.',
      DEVICE_NOT_APPROVED: 'El dispositivo todavía no fue aprobado.',
      GPS_REQUIRED:
        'Safari no proporcionó tu ubicación. Habilita Localización y Ubicación precisa para este sitio.',
      LOCATION_ACCURACY_REQUIRED:
        'No fue posible conocer la precisión del GPS. Obtén una nueva ubicación.',
      LOCATION_LOW_ACCURACY:
        'La ubicación es demasiado imprecisa. Muévete a un lugar abierto e inténtalo nuevamente.',
      GEOFENCE_NOT_CONFIGURED: 'La sede no tiene una geocerca configurada correctamente.',
      OUTSIDE_GEOFENCE: 'Estás fuera de la zona permitida para marcar asistencia.',
      INVALID_QR: 'El QR es inválido o ya venció.',
      WIFI_NOT_ALLOWED: 'Debes usar la APK y conectarte al Wi‑Fi autorizado.',
    };
    Object.assign(reasons, {
      NO_ACTIVE_ASSIGNMENT: 'No tienes una sede vigente asignada.',
      MULTIPLE_ACTIVE_ASSIGNMENTS:
        'Tienes más de una sede vigente; administración debe corregirlo.',
      MULTIPLE_ACTIVE_SCHEDULES:
        'Tienes más de un horario vigente; administración debe corregirlo.',
      DEVICE_REQUIRED: 'Esta sede requiere registrar un dispositivo.',
      QR_REQUIRED: 'Debes escanear el QR vigente.',
      NO_ACTIVE_QR: 'La sede no tiene un QR activo. Solicita que administración genere uno.',
      QR_INVALID: 'El QR no es válido.',
      QR_EXPIRED: 'El QR ya venció.',
      QR_REPLACED: 'El QR fue reemplazado; escanea el nuevo.',
      QR_WRONG_SITE: 'El QR pertenece a otra sede.',
      QR_SITE_MISMATCH: 'El QR pertenece a otra sede.',
      QR_INACTIVE: 'El QR fue reemplazado; escanea el nuevo.',
      WIFI_REQUIRED: 'Debes usar la aplicación Android conectada al Wi‑Fi autorizado.',
      INVALID_WIFI: 'La red Wi‑Fi no está autorizada.',
      WIFI_UNAVAILABLE_ON_WEB: 'Esta sede exige Wi‑Fi y debe utilizarse la aplicación Android.',
      DEVICE_NOT_REGISTERED: 'Este dispositivo todavía no fue registrado.',
      DEVICE_PENDING: 'Tu dispositivo todavía está esperando aprobación.',
      DEVICE_REJECTED: 'El registro de este dispositivo fue rechazado.',
      DEVICE_REVOKED: 'Este dispositivo fue revocado.',
      DEVICE_OWNERSHIP_MISMATCH: 'Este dispositivo está asociado a otro empleado.',
      ALREADY_CHECKED_IN: 'La entrada de esta jornada ya fue registrada.',
      ALREADY_CHECKED_OUT: 'La salida de esta jornada ya fue registrada.',
    });
    const detail = error.error?.detail ?? '';
    this.message.set(reasons[detail] ?? detail ?? 'No fue posible registrar.');
    this.failed.set(true);
    this.saving.set(false);
  }
  actionTitle() {
    const value = this.context();
    if (!value) return 'Consultando disponibilidad';
    if (!value.shift) return 'No puedes marcar hoy';
    if (value.allowedActions.includes('ENTRY')) return 'Entrada disponible';
    if (value.allowedActions.includes('EXIT')) return 'Salida disponible';
    if (value.attendance?.status === 'COMPLETED') return 'Jornada finalizada';
    return 'Marcación no disponible';
  }
  actionExplanation() {
    const value = this.context();
    if (!value) return 'Estamos consultando tu sede, horario y jornada.';
    if (!value.shift) return 'No tienes un turno asignado para hoy.';
    if (value.checkInAvailable)
      return 'Puedes registrar tu entrada. La ubicación se comprobará al pulsar el botón.';
    if (value.checkOutAvailable)
      return 'Tu entrada ya está registrada; ahora corresponde registrar la salida.';
    if (value.attendance?.status === 'COMPLETED')
      return 'Ya registraste la entrada y la salida de esta jornada.';
    return 'La jornada actual no admite otra marcación.';
  }
  buttonReason(type: 'ENTRY' | 'EXIT') {
    if (this.context()?.allowedActions.includes(type))
      return type === 'ENTRY' ? 'Registrar entrada' : 'Registrar salida';
    return this.actionExplanation();
  }
  controlName(value: string) {
    return (
      (
        { GPS: 'Ubicación', QR: 'Código QR', DEVICE: 'Dispositivo', WIFI: 'Wi-Fi' } as Record<
          string,
          string
        >
      )[value] ?? value
    );
  }
  controlStatus(value: string) {
    return (
      (
        {
          NOT_REQUIRED: 'No requerido',
          PENDING: 'Pendiente',
          REQUIRED: 'Requerido',
          UNAVAILABLE: 'No disponible',
        } as Record<string, string>
      )[value] ?? value
    );
  }
  status(value: string) {
    return (
      (
        {
          OPEN: 'Jornada abierta',
          COMPLETED: 'Completada',
          PRESENT: 'Presente',
          ABSENT: 'Ausente',
          LATE: 'Tardanza',
          INCOMPLETE: 'Incompleta',
        } as Record<string, string>
      )[value] ?? value
    );
  }
}

function iso(d: Date) {
  return d.toISOString().slice(0, 10);
}
