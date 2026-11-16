import { describe, expect, it } from 'vitest';
import { buildShiftReminder, ShiftReminderContext } from './shift-reminder';

const context: ShiftReminderContext = {
  workDate: '2026-10-19',
  shift: { startTime: '08:00:00' },
  attendance: null,
};

describe('buildShiftReminder', () => {
  it('shows the ten-minute preparation reminder', () => {
    expect(buildShiftReminder(context, new Date('2026-10-19T07:50:00-05:00'))?.key).toBe(
      'TEN_MINUTES',
    );
  });

  it('raises the five-minute reminder near the shift', () => {
    expect(buildShiftReminder(context, new Date('2026-10-19T07:55:00-05:00'))?.key).toBe(
      'FIVE_MINUTES',
    );
  });

  it('stops reminding after an entry exists', () => {
    expect(
      buildShiftReminder(
        { ...context, attendance: { firstEntryAt: '2026-10-19T12:55:00Z' } },
        new Date('2026-10-19T07:56:00-05:00'),
      ),
    ).toBeNull();
  });
});
