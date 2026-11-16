import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { ThemeService } from '../core/theme.service';
import { AppIcon } from '../shared/app-icon';
import { ApiService } from '../core/api.service';
import { buildShiftReminder, ShiftReminder, ShiftReminderContext } from '../core/shift-reminder';

@Component({
  imports: [RouterLink, RouterLinkActive, RouterOutlet, AppIcon],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell implements OnInit, OnDestroy {
  readonly menuOpen = signal(false);
  readonly collapsed = signal(localStorage.getItem('asistencia.sidebar') === 'compact');
  readonly reminder = signal<ShiftReminder | null>(null);
  readonly notificationMessage = signal('');
  readonly browserNotifications = signal(
    typeof Notification !== 'undefined' && Notification.permission === 'granted',
  );
  private reminderContext: ShiftReminderContext | null = null;
  private serverOffsetMs = 0;
  private refreshTimer?: ReturnType<typeof setInterval>;
  private clockTimer?: ReturnType<typeof setInterval>;
  constructor(
    readonly auth: AuthService,
    readonly theme: ThemeService,
    private api: ApiService,
  ) {}
  ngOnInit() {
    if (this.auth.hasAnyRole(['EMPLOYEE'])) {
      this.loadReminder();
      this.refreshTimer = setInterval(() => this.loadReminder(), 60_000);
      this.clockTimer = setInterval(() => this.updateReminder(), 15_000);
    }
  }
  ngOnDestroy() {
    clearInterval(this.refreshTimer);
    clearInterval(this.clockTimer);
  }
  toggle() {
    this.menuOpen.update((value) => !value);
  }
  close() {
    this.menuOpen.set(false);
  }
  toggleCollapsed() {
    this.collapsed.update((value) => {
      const next = !value;
      localStorage.setItem('asistencia.sidebar', next ? 'compact' : 'expanded');
      return next;
    });
  }
  async enableNotifications() {
    if (typeof Notification === 'undefined') {
      this.notificationMessage.set(
        'Este navegador no admite avisos del sistema. Los recordatorios dentro de la aplicación seguirán disponibles.',
      );
      return;
    }
    const permission =
      Notification.permission === 'granted' ? 'granted' : await Notification.requestPermission();
    this.browserNotifications.set(permission === 'granted');
    this.notificationMessage.set(
      permission === 'granted'
        ? 'Avisos del navegador activados.'
        : 'No se habilitaron avisos del navegador. Los recordatorios dentro de la aplicación seguirán disponibles.',
    );
    if (permission === 'granted') this.updateReminder();
  }
  private loadReminder() {
    this.api.get<ShiftReminderContext & { serverTime: string }>('/attendance/context').subscribe({
      next: (value) => {
        this.reminderContext = value;
        this.serverOffsetMs = new Date(value.serverTime).getTime() - Date.now();
        this.updateReminder();
      },
      error: () => {
        this.reminderContext = null;
        this.reminder.set(null);
      },
    });
  }
  private updateReminder() {
    const value = buildShiftReminder(
      this.reminderContext,
      new Date(Date.now() + this.serverOffsetMs),
    );
    this.reminder.set(value);
    if (value && this.browserNotifications()) this.notify(value);
  }
  private notify(value: ShiftReminder) {
    const date = this.reminderContext?.workDate;
    const key = `asistencia.reminder.${date}.${value.key}`;
    if (localStorage.getItem(key)) return;
    try {
      new Notification(value.title, { body: value.detail, tag: key });
      localStorage.setItem(key, 'sent');
    } catch {
      this.browserNotifications.set(false);
      this.notificationMessage.set(
        'Este navegador no puede mostrar avisos del sistema. Los recordatorios dentro de la aplicación siguen activos.',
      );
    }
  }
}
