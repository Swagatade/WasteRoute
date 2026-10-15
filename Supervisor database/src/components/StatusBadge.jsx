import React from 'react';

export default function StatusBadge({ status, size = 'md' }) {
  const getStyle = (st) => {
    switch (st?.toLowerCase()) {
      case 'resolved':
      case 'normal':
      case 'available':
      case 'approved':
        return 'badge-success';
      case 'in progress':
      case 'on route':
      case 'assigned':
        return 'badge-info';
      case 'under review':
      case 'warning':
      case 'medium':
      case 'busy':
        return 'badge-warning';
      case 'critical':
      case 'critical alert':
      case 'high':
      case 'rejected':
      case 'pending':
        return 'badge-danger';
      case 'off duty':
      case 'low':
      default:
        return 'badge-secondary';
    }
  };

  const styleClass = getStyle(status);
  const sizeStyle = size === 'sm' ? { fontSize: '11px', padding: '2px 8px' } : {};

  return (
    <span className={`badge ${styleClass}`} style={sizeStyle}>
      <span className="badge-dot" />
      {status}
    </span>
  );
}
