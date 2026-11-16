import { Component, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';

interface Employee {
  id: number;
  code: string;
  firstNames: string;
  lastNames: string;
  photoUrl?: string | null;
  active: boolean;
}
interface Context {
  serverTime: string;
  workDate: string;
  employeeId: number;
  employeeCode: string;
  employeeName: string;
  employeePhotoUrl?: string | null;
  siteName: string;
  startTime: string;
  endTime: string;
  attendanceStatus?: string | null;
  allowedActions: ('ENTRY' | 'EXIT')[];
  blockingReason?: string | null;
}

@Component({
  imports: [FormsModule, DatePipe],
  template: `
    <div class="page-head">
      <div>
        <span class="eyebrow">Flujo excepcional</span>
        <h1>Asistencia de contingencia</h1>
        <p>
          Úsala solo cuando el empleado no puede realizar la marcación personal. Toda acción
          conserva autor, motivo y canal.
        </p>
      </div>
      <button class="secondary" type="button" (click)="loadContext()" [disabled]="!employeeId">
        Actualizar
      </button>
    </div>
    @if (message()) {
      <div class="notice" [class.error]="failed()">{{ message() }}</div>
    }
    <div class="contingency-grid">
      <section class="panel selector">
        <h2>1. Selecciona al empleado</h2>
        <label
          >Empleado
          <select [(ngModel)]="employeeId" (ngModelChange)="loadContext()">
            <option [ngValue]="0">Seleccionar…</option>
            @for (employee of employees(); track employee.id) {
              <option [ngValue]="employee.id">
                {{ employee.code }} · {{ employee.firstNames }} {{ employee.lastNames }}
              </option>
            }
          </select>
        </label>
        <p class="help">
          El backend verifica que el empleado esté dentro de tu alcance, activo, con sede y turno
          vigentes.
        </p>
      </section>
      @if (context(); as value) {
        <section class="panel employee-card">
          <div class="profile">
            @if (value.employeePhotoUrl) {
              <img [src]="value.employeePhotoUrl" alt="Foto del empleado" />
            } @else {
              <span class="avatar">{{ initials(value.employeeName) }}</span>
            }
            <div>
              <small>{{ value.employeeCode }}</small>
              <h2>{{ value.employeeName }}</h2>
              <p>{{ value.siteName }}</p>
            </div>
          </div>
          <dl>
            <div>
              <dt>Fecha laboral</dt>
              <dd>{{ value.workDate }}</dd>
            </div>
            <div>
              <dt>Horario</dt>
              <dd>{{ value.startTime }}–{{ value.endTime }}</dd>
            </div>
            <div>
              <dt>Estado</dt>
              <dd>{{ value.attendanceStatus || 'Sin marcación' }}</dd>
            </div>
            <div>
              <dt>Hora servidor</dt>
              <dd>{{ value.serverTime | date: 'shortTime' }}</dd>
            </div>
          </dl>
        </section>
        <section class="panel action-card">
          <h2>2. Registra la contingencia</h2>
          <label
            >Motivo
            <select [(ngModel)]="reason">
              @for (option of reasons; track option.value) {
                <option [value]="option.value">{{ option.label }}</option>
              }
            </select>
          </label>
          <label
            >Comentario
            @if (reason === 'OTHER') {
              <strong>(obligatorio)</strong>
            }
            <textarea
              [(ngModel)]="comment"
              rows="3"
              maxlength="500"
              placeholder="Describe brevemente la situación"
            ></textarea>
          </label>
          @if (value.blockingReason) {
            <div class="blocked">{{ reasonLabel(value.blockingReason) }}</div>
          }
          <div class="actions">
            <button
              type="button"
              (click)="punch('ENTRY')"
              [disabled]="saving() || !value.allowedActions.includes('ENTRY')"
            >
              Registrar entrada
            </button>
            <button
              type="button"
              class="secondary"
              (click)="punch('EXIT')"
              [disabled]="saving() || !value.allowedActions.includes('EXIT')"
            >
              Registrar salida
            </button>
          </div>
          <small class="audit-note"
            >Se guardará como MANUAL_SUPERVISOR o MANUAL_ADMIN; nunca como marcación móvil
            normal.</small
          >
        </section>
      }
    </div>
  `,
  styles: `
    .eyebrow {
      color: var(--accent);
      font-size: 0.74rem;
      font-weight: 800;
      letter-spacing: 0.12em;
      text-transform: uppercase;
    }
    .contingency-grid {
      display: grid;
      grid-template-columns: minmax(260px, 0.72fr) minmax(340px, 1fr);
      gap: 1rem;
    }
    .panel {
      background: var(--white);
      border: 1px solid var(--line);
      border-radius: 20px;
      padding: 1.25rem;
      box-shadow: var(--shadow-sm);
    }
    .selector {
      grid-row: span 2;
    }
    .panel h2 {
      margin: 0 0 1rem;
    }
    .panel label {
      display: grid;
      gap: 0.45rem;
      color: var(--blue);
      font-weight: 700;
    }
    .help,
    .audit-note {
      display: block;
      color: var(--muted);
      line-height: 1.5;
    }
    .profile {
      display: flex;
      align-items: center;
      gap: 1rem;
    }
    .profile img,
    .avatar {
      width: 64px;
      height: 64px;
      border-radius: 18px;
      object-fit: cover;
    }
    .avatar {
      display: grid;
      place-items: center;
      background: linear-gradient(135deg, var(--accent), var(--blue));
      color: white;
      font-weight: 800;
    }
    .profile h2,
    .profile p {
      margin: 0.15rem 0;
    }
    .profile small {
      color: var(--muted);
    }
    dl {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 0.7rem;
      margin: 1.2rem 0 0;
    }
    dl div {
      padding: 0.8rem;
      border-radius: 14px;
      background: var(--surface);
    }
    dt {
      font-size: 0.72rem;
      color: var(--muted);
      text-transform: uppercase;
      letter-spacing: 0.06em;
    }
    dd {
      margin: 0.25rem 0 0;
      font-weight: 750;
    }
    .action-card {
      display: grid;
      gap: 0.85rem;
    }
    .actions {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 0.7rem;
    }
    .blocked {
      padding: 0.8rem;
      border-radius: 12px;
      background: #fff3e8;
      color: #9a4d00;
      font-weight: 700;
    }
    .notice.error {
      background: #fff0f0;
      color: #a51d1d;
    }
    @media (max-width: 850px) {
      .contingency-grid {
        grid-template-columns: 1fr;
      }
      .selector {
        grid-row: auto;
      }
    }
    @media (max-width: 520px) {
      dl,
      .actions {
        grid-template-columns: 1fr;
      }
      .panel {
        padding: 1rem;
      }
    }
  `,
})
export class AttendanceContingencyPage implements OnInit {
  readonly employees = signal<Employee[]>([]);
  readonly context = signal<Context | null>(null);
  readonly message = signal('');
  readonly failed = signal(false);
  readonly saving = signal(false);
  employeeId = 0;
  reason = 'DEVICE_FORGOTTEN';
  comment = '';
  readonly reasons = [
    { value: 'DEVICE_FORGOTTEN', label: 'Olvidó el dispositivo' },
    { value: 'DEVICE_BATTERY', label: 'Dispositivo sin batería' },
    { value: 'DEVICE_DAMAGED', label: 'Dispositivo averiado' },
    { value: 'DEVICE_LOST', label: 'Dispositivo perdido' },
    { value: 'APP_FAILURE', label: 'Falla de la aplicación' },
    { value: 'OTHER', label: 'Otro motivo' },
  ];
  constructor(private api: ApiService) {}
  ngOnInit() {
    this.api
      .get<Employee[]>('/employees')
      .subscribe({
        next: (value) => this.employees.set(value.filter((item) => item.active)),
        error: () => this.show('No se pudieron cargar los empleados.', true),
      });
  }
  loadContext() {
    this.context.set(null);
    if (!this.employeeId) return;
    this.api
      .get<Context>('/attendance-contingencies/context', { employeeId: String(this.employeeId) })
      .subscribe({
        next: (value) => {
          this.context.set(value);
          this.message.set('');
        },
        error: (error) =>
          this.show(this.reasonLabel(error.error?.code ?? error.error?.detail), true),
      });
  }
  punch(type: 'ENTRY' | 'EXIT') {
    if (this.reason === 'OTHER' && !this.comment.trim()) {
      this.show('Escribe el comentario para el motivo “Otro”.', true);
      return;
    }
    this.saving.set(true);
    this.api
      .post('/attendance-contingencies/punch', {
        employeeId: this.employeeId,
        type,
        reason: this.reason,
        comment: this.comment.trim() || null,
      })
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.show(
            `${type === 'ENTRY' ? 'Entrada' : 'Salida'} registrada con auditoría de contingencia.`,
            false,
          );
          this.loadContext();
        },
        error: (error) => {
          this.saving.set(false);
          this.show(this.reasonLabel(error.error?.code ?? error.error?.detail), true);
        },
      });
  }
  initials(name: string) {
    return name
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map((value) => value[0])
      .join('')
      .toUpperCase();
  }
  reasonLabel(value?: string | null) {
    return (
      (
        {
          ATTENDANCE_COMPLETED: 'La jornada ya tiene entrada y salida.',
          CHECK_IN_NOT_OPEN: 'La ventana de entrada todavía no está abierta.',
          CHECK_IN_WINDOW_CLOSED:
            'La ventana de entrada terminó; utiliza una solicitud de corrección.',
          CHECK_OUT_NOT_OPEN: 'La salida todavía no está habilitada.',
          CHECK_OUT_WINDOW_CLOSED:
            'La ventana de salida terminó; utiliza una solicitud de corrección.',
          NO_ACTIVE_SCHEDULE: 'El empleado no tiene un horario vigente.',
          MULTIPLE_ACTIVE_SCHEDULES:
            'El empleado tiene más de un horario vigente; corrige la configuración.',
          NON_WORKING_DAY: 'Hoy no es un día laborable para este empleado.',
          NO_ACTIVE_SITE_ASSIGNMENT: 'El empleado no tiene una sede vigente.',
          MULTIPLE_ACTIVE_ASSIGNMENTS: 'El empleado tiene más de una sede vigente.',
          EMPLOYEE_OUT_OF_SCOPE: 'El empleado no está dentro de tu alcance de supervisión.',
          DUPLICATE_ENTRY: 'La entrada ya fue registrada.',
          DUPLICATE_EXIT: 'La salida ya fue registrada.',
          EXIT_WITHOUT_ENTRY: 'No se puede registrar una salida sin entrada.',
        } as Record<string, string>
      )[value ?? ''] ??
      value ??
      'No se pudo completar la operación.'
    );
  }
  private show(value: string, failed: boolean) {
    this.message.set(value);
    this.failed.set(failed);
  }
}
