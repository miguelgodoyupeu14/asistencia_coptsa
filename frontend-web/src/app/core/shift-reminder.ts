export interface ShiftReminderContext {
  workDate: string;
  shift: { startTime: string } | null;
  attendance: { firstEntryAt: string | null } | null;
}

export interface ShiftReminder {
  key: 'TEN_MINUTES' | 'FIVE_MINUTES' | 'STARTED';
  tone: 'info' | 'warning' | 'urgent';
  title: string;
  detail: string;
}

export function buildShiftReminder(
  context: ShiftReminderContext | null,
  now: Date,
): ShiftReminder | null {
  if (!context?.shift || context.attendance?.firstEntryAt) return null;
  const start = new Date(`${context.workDate}T${context.shift.startTime}-05:00`);
  const difference = start.getTime() - now.getTime();
  const minutesUntil = Math.ceil(difference / 60_000);
  if (minutesUntil > 10 || minutesUntil < -120) return null;
  if (minutesUntil > 5)
    return {
      key: 'TEN_MINUTES',
      tone: 'info',
      title: `Tu turno inicia en ${minutesUntil} minutos`,
      detail: 'Prepárate para registrar tu entrada a tiempo.',
    };
  if (minutesUntil > 0)
    return {
      key: 'FIVE_MINUTES',
      tone: 'warning',
      title: `Tu turno inicia en ${minutesUntil} ${minutesUntil === 1 ? 'minuto' : 'minutos'}`,
      detail: 'Ya puedes abrir Mi asistencia y completar las validaciones requeridas.',
    };
  const elapsed = Math.floor(Math.abs(difference) / 60_000);
  return {
    key: 'STARTED',
    tone: 'urgent',
    title:
      elapsed === 0
        ? 'Tu turno inicia ahora'
        : `Tu turno inició hace ${elapsed} ${elapsed === 1 ? 'minuto' : 'minutos'}`,
    detail: 'Registra tu entrada lo antes posible para evitar una tardanza mayor.',
  };
}
