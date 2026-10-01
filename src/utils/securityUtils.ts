export function generateSalt(length = 16): string {
  const chars = '0123456789abcdef';
  let salt = '';
  for (let i = 0; i < length; i++) {
    salt += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return salt;
}

export async function hashPassword(password: string, salt: string): Promise<string> {
  const encoder = new TextEncoder();
  const data = encoder.encode(`${salt}:${password}:EBL_BANK_SECURE_SALT_2026`);
  const hashBuffer = await crypto.subtle.digest('SHA-256', data);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
}

export async function verifyPassword(password: string, salt: string, expectedHash: string): Promise<boolean> {
  const hash = await hashPassword(password, salt);
  return hash.toLowerCase() === expectedHash.toLowerCase();
}

export function generateFileId(rmCode: string): string {
  const cleanRm = rmCode.slice(-4);
  const randomSuffix = Math.floor(100 + Math.random() * 900);
  return `EBL-2026-${cleanRm}-${randomSuffix}`;
}

export function generateId(): string {
  return 'id_' + Math.random().toString(36).substring(2, 9) + '_' + Date.now();
}

export function generateTemporaryPassword(): string {
  const upper = 'ABCDEFGHJKLMNPQRSTUVWXYZ';
  const lower = 'abcdefghijkmnopqrstuvwxyz';
  const numbers = '23456789';
  const special = '#$@!';
  
  const p1 = upper.charAt(Math.floor(Math.random() * upper.length));
  const p2 = lower.charAt(Math.floor(Math.random() * lower.length));
  const p3 = numbers.charAt(Math.floor(Math.random() * numbers.length));
  const p4 = special.charAt(Math.floor(Math.random() * special.length));
  
  const all = upper + lower + numbers;
  let remaining = '';
  for (let i = 0; i < 4; i++) {
    remaining += all.charAt(Math.floor(Math.random() * all.length));
  }
  return `${p1}${p2}${p3}${p4}${remaining}`;
}
