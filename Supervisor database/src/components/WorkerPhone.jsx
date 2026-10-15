import React from 'react';
import { Phone } from 'lucide-react';

/** Worker mobile number as a tap-to-call line. Pass `phone` already resolved with workerPhone(). */
export default function WorkerPhone({ phone, style = {}, size = 12 }) {
  const base = { display: 'inline-flex', alignItems: 'center', gap: 5, fontSize: 12, fontWeight: 600, ...style };
  if (!phone) {
    return (
      <span style={{ ...base, color: 'var(--text-muted)', fontStyle: 'italic', fontWeight: 400 }}>
        <Phone size={size} /> No mobile on file
      </span>
    );
  }
  return (
    <a href={`tel:${phone.replace(/[^\d+]/g, '')}`} style={{ ...base, color: 'var(--cyan-400)', textDecoration: 'none' }}>
      <Phone size={size} /> {phone}
    </a>
  );
}
