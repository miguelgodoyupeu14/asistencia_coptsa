import { Component, OnDestroy, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import QRCode from 'qrcode';
import { ApiService } from '../../core/api.service';

interface Device {
  id: number;
  employee_code: string;
  display_name: string;
  platform: string;
  status: string;
  revoked_at?: string | null;
  revocation_reason?: string | null;
}
interface Attempt {
  id: number;
  employee_id: number;
  site_id: number | null;
  punch_type: string | null;
  accepted: boolean;
  reason_code: string;
  channel: string;
  ip_address: string;
  occurred_at: string;
  wifi_bssid?: string;
}
interface Site {
  id: number;
  name: string;
  latitude: number | null;
  longitude: number | null;
  geofenceRadiusM: number | null;
  requireGeofence: boolean;
  requireRegisteredDevice: boolean;
  requireDynamicQr: boolean;
  allowedWifiBssid: string | null;
  requireWifi: boolean;
}
interface QrResponse {
  code: string;
  expiresAt: string;
}

@Component({
  imports: [FormsModule],
  template: ` <div class="page-head">
      <div>
        <h1>Seguridad de marcación</h1>
        <p>Configura la política por sede y supervisa cada intento.</p>
      </div>
      <button class="secondary" (click)="load()">Actualizar</button>
    </div>
    @if (message()) {
      <div class="notice">{{ message() }}</div>
    }
    <section class="security-grid">
      <form class="security-card" (ngSubmit)="savePolicy()">
        <h2>Política de la sede</h2>
        <label
          >Sede<select
            [(ngModel)]="siteId"
            name="policySite"
            (ngModelChange)="selectSite()"
            required
          >
            @for (s of sites(); track s.id) {
              <option [ngValue]="s.id">{{ s.name }}</option>
            }
          </select></label
        >
        <div class="checks">
          <label
            ><input type="checkbox" [(ngModel)]="policy.requireRegisteredDevice" name="device" />
            Dispositivo aprobado</label
          ><label
            ><input type="checkbox" [(ngModel)]="policy.requireGeofence" name="geo" /> Geocerca
            GPS</label
          ><label
            ><input type="checkbox" [(ngModel)]="policy.requireDynamicQr" name="qr" /> QR
            dinámico</label
          ><label
            ><input type="checkbox" [(ngModel)]="policy.requireWifi" name="wifi" /> Wi‑Fi
            autorizado</label
          >
        </div>
        @if (policy.requireWifi) {
          <div class="warning">
            Wi‑Fi/BSSID no está disponible en Safari/iPhone. Esta política impedirá la marcación
            desde esos navegadores.
          </div>
        }
        <label
          >Radio de geocerca (m)<input
            type="number"
            [(ngModel)]="policy.geofenceRadiusM"
            name="radius"
            min="1"
            placeholder="100"
        /></label>
        <label
          >BSSID Wi‑Fi autorizado<input
            [(ngModel)]="policy.allowedWifiBssid"
            name="bssid"
            placeholder="AA:BB:CC:DD:EE:FF"
        /></label>
        <small
          >El BSSID identifica el punto de acceso. GPS requiere coordenadas y radio configurados en
          la sede.</small
        >
        <button>Guardar política</button>
      </form>
      <form class="security-card qr-card" (ngSubmit)="generateQr()">
        <h2>QR temporal</h2>
        <label
          >Sede<select [(ngModel)]="siteId" name="qrSite" required>
            @for (s of sites(); track s.id) {
              <option [ngValue]="s.id">{{ s.name }}</option>
            }
          </select></label
        >
        <label
          >Vigencia (segundos)<input
            type="number"
            [(ngModel)]="validSeconds"
            name="seconds"
            min="30"
            max="3600"
            required /></label
        ><button>Generar y reemplazar QR</button>
        @if (qrDataUrl()) {
          <img [src]="qrDataUrl()" alt="Código QR temporal para marcar asistencia" /><strong
            [class.expired]="remaining() === 0"
            >{{ remaining() === 0 ? 'QR vencido' : remaining() + ' s restantes' }}</strong
          ><small>Al generar otro QR para esta sede, este deja de ser válido.</small>
        }
      </form>
    </section>
    <h2>Dispositivos</h2>
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Empleado</th>
            <th>Dispositivo</th>
            <th>Plataforma</th>
            <th>Estado</th>
            <th>Acciones</th>
          </tr>
        </thead>
        <tbody>
          @for (d of devices(); track d.id) {
            <tr>
              <td>{{ d.employee_code }}</td>
              <td>
                {{ d.display_name }}
                @if (d.revocation_reason) {
                  <small class="device-reason">{{ d.revocation_reason }}</small>
                }
              </td>
              <td>{{ d.platform }}</td>
              <td>
                <span class="badge">{{ d.status }}</span>
              </td>
              <td class="device-actions">
                <button (click)="reviewDevice(d.id, true)" [disabled]="d.status === 'APPROVED'">
                  Aprobar
                </button>
                <button
                  class="secondary"
                  (click)="reviewDevice(d.id, false)"
                  [disabled]="d.status === 'REJECTED'"
                >
                  Rechazar
                </button>
                <button
                  class="danger"
                  (click)="revokeDevice(d.id)"
                  [disabled]="d.status === 'REVOKED'"
                >
                  Revocar
                </button>
              </td>
            </tr>
          } @empty {
            <tr>
              <td colspan="5">Sin dispositivos</td>
            </tr>
          }
        </tbody>
      </table>
    </div>
    <h2>Intentos recientes</h2>
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Empleado</th>
            <th>Sede</th>
            <th>Acción</th>
            <th>Resultado</th>
            <th>Motivo</th>
            <th>Canal</th>
            <th>Wi‑Fi</th>
            <th>Fecha</th>
          </tr>
        </thead>
        <tbody>
          @for (a of attempts(); track a.id) {
            <tr>
              <td>#{{ a.employee_id }}</td>
              <td>{{ a.site_id ? '#' + a.site_id : '—' }}</td>
              <td>{{ a.punch_type ?? '—' }}</td>
              <td>
                <span class="badge">{{ a.accepted ? 'Aceptado' : 'Rechazado' }}</span>
              </td>
              <td>{{ reason(a.reason_code) }}</td>
              <td>{{ a.channel }}</td>
              <td>{{ a.wifi_bssid || '—' }}</td>
              <td>{{ a.occurred_at }}</td>
            </tr>
          } @empty {
            <tr>
              <td colspan="8">Sin intentos</td>
            </tr>
          }
        </tbody>
      </table>
    </div>`,
  styles: `
    h2 {
      margin-top: 1.8rem;
    }
    .security-grid {
      display: grid;
      grid-template-columns: 1.2fr 0.8fr;
      gap: 1rem;
    }
    .security-card {
      display: grid;
      align-content: start;
      gap: 0.8rem;
      padding: 1.2rem;
      background: var(--white);
      border: 1px solid var(--line);
      border-radius: 17px;
      box-shadow: var(--shadow-sm);
    }
    .security-card h2 {
      margin: 0;
    }
    .security-card label {
      display: grid;
      gap: 0.4rem;
      color: var(--blue);
      font-size: 0.82rem;
      font-weight: 700;
    }
    .checks {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 0.55rem;
    }
    .checks label {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }
    .checks input {
      width: auto;
      min-height: 0;
    }
    .security-card small {
      color: var(--muted);
    }
    .warning {
      padding: 0.75rem;
      border-radius: 12px;
      background: #fff3e8;
      color: #8c4700;
      font-size: 0.82rem;
      line-height: 1.4;
    }
    .qr-card img {
      width: min(100%, 280px);
      margin: 0.4rem auto;
      border-radius: 14px;
      background: white;
      padding: 0.6rem;
    }
    .qr-card strong {
      text-align: center;
      color: var(--accent);
      font-size: 1rem;
    }
    .qr-card strong.expired {
      color: #b42318;
    }
    .device-actions {
      display: flex;
      flex-wrap: wrap;
      gap: 0.4rem;
    }
    .device-actions button {
      width: auto;
    }
    .danger {
      background: #a61b1b;
      color: white;
    }
    .device-reason {
      display: block;
      color: var(--muted);
      margin-top: 0.25rem;
    }
    @media (max-width: 800px) {
      .security-grid {
        grid-template-columns: 1fr;
      }
      .checks {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class AttendanceSecurityPage implements OnInit, OnDestroy {
  readonly devices = signal<Device[]>([]);
  readonly attempts = signal<Attempt[]>([]);
  readonly sites = signal<Site[]>([]);
  readonly qr = signal<QrResponse | null>(null);
  readonly qrDataUrl = signal('');
  readonly message = signal('');
  readonly now = signal(Date.now());
  readonly remaining = computed(() => {
    const expires = this.qr()?.expiresAt;
    if (!expires) return 0;
    return Math.max(0, Math.ceil((new Date(expires).getTime() - this.now()) / 1000));
  });
  siteId = 0;
  validSeconds = 120;
  private timer?: ReturnType<typeof setInterval>;
  policy = {
    geofenceRadiusM: null as number | null,
    requireGeofence: false,
    requireRegisteredDevice: false,
    requireDynamicQr: false,
    allowedWifiBssid: '',
    requireWifi: false,
  };
  constructor(private api: ApiService) {}
  ngOnInit() {
    this.timer = setInterval(() => this.now.set(Date.now()), 1000);
    this.load();
  }
  ngOnDestroy() {
    if (this.timer) clearInterval(this.timer);
  }
  load() {
    this.api
      .get<Device[]>('/attendance-security/devices')
      .subscribe({
        next: (r) => this.devices.set(r),
        error: () => this.message.set('No se pudieron cargar los dispositivos.'),
      });
    this.api
      .get<Attempt[]>('/attendance-security/attempts', { limit: '100' })
      .subscribe({
        next: (r) => this.attempts.set(r),
        error: () => this.message.set('No se pudieron cargar los intentos.'),
      });
    this.api.get<Site[]>('/organization/sites').subscribe((r) => {
      this.sites.set(r);
      if (!this.siteId && r[0]) {
        this.siteId = r[0].id;
        this.selectSite();
      }
    });
  }
  selectSite() {
    const site = this.sites().find((s) => s.id === Number(this.siteId));
    if (site)
      this.policy = {
        geofenceRadiusM: site.geofenceRadiusM,
        requireGeofence: site.requireGeofence,
        requireRegisteredDevice: site.requireRegisteredDevice,
        requireDynamicQr: site.requireDynamicQr,
        allowedWifiBssid: site.allowedWifiBssid ?? '',
        requireWifi: site.requireWifi,
      };
  }
  savePolicy() {
    this.api
      .patch(`/organization/sites/${this.siteId}/attendance-security`, {
        ...this.policy,
        allowedWifiBssid: this.policy.allowedWifiBssid || null,
      })
      .subscribe({
        next: () => {
          this.message.set('Política de seguridad actualizada.');
          this.load();
        },
        error: (e) => this.message.set(e.error?.detail ?? 'No se pudo guardar la política.'),
      });
  }
  reviewDevice(id: number, approved: boolean) {
    this.api.patch(`/attendance-security/devices/${id}`, { approved }).subscribe({
      next: () => {
        this.message.set('Estado del dispositivo actualizado.');
        this.load();
      },
      error: () => this.message.set('No se pudo actualizar el dispositivo.'),
    });
  }
  revokeDevice(id: number) {
    const reason = window
      .prompt('Motivo de revocación (por ejemplo: equipo robado o perdido):')
      ?.trim();
    if (!reason) return;
    this.api.patch(`/attendance-security/devices/${id}/revoke`, { reason }).subscribe({
      next: () => {
        this.message.set('Dispositivo revocado. Ya no podrá registrar asistencia.');
        this.load();
      },
      error: (e) => this.message.set(e.error?.detail ?? 'No se pudo revocar el dispositivo.'),
    });
  }
  generateQr() {
    this.api
      .post<QrResponse>('/attendance-security/qr', {
        siteId: Number(this.siteId),
        validSeconds: Number(this.validSeconds),
      })
      .subscribe({
        next: async (r) => {
          this.qr.set(r);
          this.now.set(Date.now());
          this.qrDataUrl.set(
            await QRCode.toDataURL(r.code, { width: 360, margin: 2, errorCorrectionLevel: 'M' }),
          );
        },
        error: (e) => this.message.set(e.error?.detail ?? 'No se pudo generar el QR.'),
      });
  }
  reason(value: string) {
    return (
      (
        {
          VALIDATED: 'Validado',
          NO_ACTIVE_SITE_ASSIGNMENT: 'Sin sede vigente',
          SITE_ASSIGNMENT_OVERLAP: 'Sedes superpuestas',
          NO_ACTIVE_SCHEDULE: 'Sin horario vigente',
          SCHEDULE_OVERLAP: 'Horarios superpuestos',
          NON_WORKING_DAY: 'Día no laborable',
          OUTSIDE_ENTRY_WINDOW: 'Entrada fuera de horario',
          OUTSIDE_EXIT_WINDOW: 'Salida fuera de horario',
          DUPLICATE_ENTRY: 'Entrada duplicada',
          EXIT_WITHOUT_ENTRY: 'Salida sin entrada',
          DUPLICATE_EXIT: 'Salida duplicada',
          EXIT_BEFORE_ENTRY: 'Salida anterior a entrada',
          DEVICE_REQUIRED: 'Dispositivo no enviado',
          DEVICE_NOT_APPROVED: 'Dispositivo no aprobado',
          GPS_REQUIRED: 'GPS no proporcionado',
          GEOFENCE_NOT_CONFIGURED: 'Geocerca sin configurar',
          OUTSIDE_GEOFENCE: 'Fuera de geocerca',
          QR_REQUIRED: 'QR requerido',
          QR_INVALID: 'QR desconocido',
          QR_WRONG_SITE: 'QR de otra sede',
          QR_REPLACED: 'QR reemplazado',
          QR_EXPIRED: 'QR vencido',
          WIFI_REQUIRED: 'Wi‑Fi requerido',
          INVALID_WIFI: 'Wi‑Fi no autorizado',
        } as Record<string, string>
      )[value] ?? value
    );
  }
}
