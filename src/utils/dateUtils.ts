import { TimeFilter } from '../types';

export const DHAKA_TIMEZONE = 'Asia/Dhaka';

export function formatDateTime(millis?: number): string {
  if (!millis || millis <= 0) return 'N/A';
  try {
    const d = new Date(millis);
    return new Intl.DateTimeFormat('en-GB', {
      timeZone: DHAKA_TIMEZONE,
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      hour12: true
    }).format(d);
  } catch {
    return 'Invalid Date';
  }
}

export function formatDateOnly(millis?: number): string {
  if (!millis || millis <= 0) return 'N/A';
  try {
    const d = new Date(millis);
    return new Intl.DateTimeFormat('en-GB', {
      timeZone: DHAKA_TIMEZONE,
      day: '2-digit',
      month: 'short',
      year: 'numeric'
    }).format(d);
  } catch {
    return 'Invalid Date';
  }
}

export function formatTimeOnly(date = new Date()): string {
  try {
    return new Intl.DateTimeFormat('en-US', {
      timeZone: DHAKA_TIMEZONE,
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: true
    }).format(date);
  } catch {
    return date.toLocaleTimeString();
  }
}

export function formatIsoDate(millis?: number): string {
  if (!millis || millis <= 0) return '';
  const d = new Date(millis);
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export function getTodayRange(): [number, number] {
  const now = new Date();
  const start = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0, 0);
  const end = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 23, 59, 59, 999);
  return [start.getTime(), end.getTime()];
}

/**
 * Week is Saturday through Friday by Bangladesh banking standard
 */
export function getThisWeekRange(): [number, number] {
  const now = new Date();
  const dayOfWeek = now.getDay(); // 0 is Sunday, 6 is Saturday
  // Saturday is day 6. If today is Saturday, diff is 0. If Sunday (0), diff is 1.
  const daysSinceSaturday = (dayOfWeek + 1) % 7;
  const saturday = new Date(now.getFullYear(), now.getMonth(), now.getDate() - daysSinceSaturday, 0, 0, 0, 0);
  const friday = new Date(saturday.getTime() + 6 * 24 * 60 * 60 * 1000 + (23 * 3600 + 59 * 60 + 59) * 1000 + 999);
  return [saturday.getTime(), friday.getTime()];
}

export function getLastWeekRange(): [number, number] {
  const [thisWeekStart] = getThisWeekRange();
  const lastWeekStart = thisWeekStart - 7 * 24 * 60 * 60 * 1000;
  const lastWeekEnd = thisWeekStart - 1;
  return [lastWeekStart, lastWeekEnd];
}

export function getThisMonthRange(): [number, number] {
  const now = new Date();
  const start = new Date(now.getFullYear(), now.getMonth(), 1, 0, 0, 0, 0);
  const end = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59, 999);
  return [start.getTime(), end.getTime()];
}

export function getLastMonthRange(): [number, number] {
  const now = new Date();
  const start = new Date(now.getFullYear(), now.getMonth() - 1, 1, 0, 0, 0, 0);
  const end = new Date(now.getFullYear(), now.getMonth(), 0, 23, 59, 59, 999);
  return [start.getTime(), end.getTime()];
}

export function matchesTimeFilter(timestamp: number, filter: TimeFilter): boolean {
  if (filter === 'ALL_TIME') return true;
  if (filter === 'TODAY') {
    const [s, e] = getTodayRange();
    return timestamp >= s && timestamp <= e;
  }
  if (filter === 'THIS_WEEK') {
    const [s, e] = getThisWeekRange();
    return timestamp >= s && timestamp <= e;
  }
  if (filter === 'LAST_WEEK') {
    const [s, e] = getLastWeekRange();
    return timestamp >= s && timestamp <= e;
  }
  if (filter === 'THIS_MONTH') {
    const [s, e] = getThisMonthRange();
    return timestamp >= s && timestamp <= e;
  }
  if (filter === 'LAST_MONTH') {
    const [s, e] = getLastMonthRange();
    return timestamp >= s && timestamp <= e;
  }
  return true;
}
