import React from 'react';
import { ApplicationStatus, ActiveStatus, CpvStatus, AccountStatus } from '../types';

export const ApplicationStatusBadge: React.FC<{ status: ApplicationStatus | string }> = ({ status }) => {
  const getColors = () => {
    switch (status.toLowerCase()) {
      case 'approved':
        return 'bg-emerald-100 text-emerald-800 border-emerald-300';
      case 'submitted':
        return 'bg-blue-100 text-blue-800 border-blue-300';
      case 'collected':
        return 'bg-teal-100 text-teal-800 border-teal-300';
      case 'query':
        return 'bg-amber-100 text-amber-800 border-amber-300';
      case 'return to source':
        return 'bg-rose-100 text-rose-800 border-rose-300';
      case 'condition':
        return 'bg-purple-100 text-purple-800 border-purple-300';
      case 'stc':
        return 'bg-cyan-100 text-cyan-800 border-cyan-300';
      case 'declined':
        return 'bg-red-100 text-red-800 border-red-300';
      default:
        return 'bg-slate-100 text-slate-800 border-slate-300';
    }
  };

  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold border ${getColors()}`}>
      {status}
    </span>
  );
};

export const ActiveStatusBadge: React.FC<{ status: ActiveStatus | string }> = ({ status }) => {
  let label = status;
  let colors = 'bg-slate-100 text-slate-700 border-slate-300';

  if (status === 'Y') {
    label = 'Active (Y)';
    colors = 'bg-emerald-50 text-emerald-700 border-emerald-200';
  } else if (status === 'N') {
    label = 'Inactive (N)';
    colors = 'bg-slate-100 text-slate-600 border-slate-200';
  } else if (status === 'C') {
    label = 'Cancelled (C)';
    colors = 'bg-rose-50 text-rose-700 border-rose-200';
  }

  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded text-[11px] font-medium border ${colors}`}>
      {label}
    </span>
  );
};

export const CpvStatusBadge: React.FC<{ status: CpvStatus | string }> = ({ status }) => {
  let colors = 'bg-slate-100 text-slate-700';
  switch (status.toLowerCase()) {
    case 'completed':
      colors = 'bg-emerald-100 text-emerald-800';
      break;
    case 'pending':
      colors = 'bg-amber-100 text-amber-800';
      break;
    case 'failed':
      colors = 'bg-rose-100 text-rose-800';
      break;
    case 'not required':
      colors = 'bg-slate-100 text-slate-500';
      break;
  }

  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded text-[11px] font-medium ${colors}`}>
      CPV: {status}
    </span>
  );
};

export const AccountStatusBadge: React.FC<{ status: AccountStatus | string }> = ({ status }) => {
  let colors = 'bg-slate-100 text-slate-700';
  if (status === 'ACTIVE') colors = 'bg-emerald-100 text-emerald-800';
  else if (status === 'SUSPENDED') colors = 'bg-rose-100 text-rose-800';
  else if (status === 'INACTIVE') colors = 'bg-slate-100 text-slate-600';

  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold ${colors}`}>
      {status}
    </span>
  );
};
