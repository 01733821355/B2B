import React from 'react';
import { KpiStats } from '../types';
import {
  FileText,
  CheckCircle2,
  Send,
  Download,
  AlertCircle,
  RotateCcw,
  ShieldAlert,
  ClipboardList,
  CreditCard,
  XCircle,
  HelpCircle
} from 'lucide-react';

interface KpiCardsProps {
  stats: KpiStats;
  onCardClick?: (metric: string) => void;
}

export const KpiCards: React.FC<KpiCardsProps> = ({ stats, onCardClick }) => {
  const cards = [
    {
      title: 'Total Files',
      value: stats.totalFiles,
      icon: FileText,
      color: 'text-blue-700 bg-blue-50 border-blue-200',
      metricKey: 'Total'
    },
    {
      title: 'Approved',
      value: stats.approved,
      icon: CheckCircle2,
      color: 'text-emerald-700 bg-emerald-50 border-emerald-200',
      metricKey: 'Approved'
    },
    {
      title: 'Submitted',
      value: stats.submitted,
      icon: Send,
      color: 'text-indigo-700 bg-indigo-50 border-indigo-200',
      metricKey: 'Submitted'
    },
    {
      title: 'Collected',
      value: stats.collected,
      icon: Download,
      color: 'text-teal-700 bg-teal-50 border-teal-200',
      metricKey: 'Collected'
    },
    {
      title: 'Query',
      value: stats.query,
      icon: HelpCircle,
      color: 'text-amber-700 bg-amber-50 border-amber-200',
      metricKey: 'Query'
    },
    {
      title: 'Return to Source',
      value: stats.returnToSource,
      icon: RotateCcw,
      color: 'text-rose-700 bg-rose-50 border-rose-200',
      metricKey: 'Return to Source'
    },
    {
      title: 'Condition',
      value: stats.condition,
      icon: AlertCircle,
      color: 'text-purple-700 bg-purple-50 border-purple-200',
      metricKey: 'Condition'
    },
    {
      title: 'STC',
      value: stats.stc,
      icon: ShieldAlert,
      color: 'text-cyan-700 bg-cyan-50 border-cyan-200',
      metricKey: 'STC'
    },
    {
      title: 'Declined',
      value: stats.declined,
      icon: XCircle,
      color: 'text-red-700 bg-red-50 border-red-200',
      metricKey: 'Declined'
    },
    {
      title: 'Pending Docs',
      value: stats.pendingDocumentsCount,
      icon: ClipboardList,
      color: 'text-orange-700 bg-orange-50 border-orange-200',
      subtitle: 'Missing items'
    },
    {
      title: 'Active Cards (Y)',
      value: stats.activeY,
      icon: CreditCard,
      color: 'text-emerald-700 bg-emerald-50 border-emerald-200',
      subtitle: `Inactive: ${stats.activeN} | Closed: ${stats.activeC}`
    }
  ];

  return (
    <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
      {cards.map((c, i) => {
        const Icon = c.icon;
        return (
          <div
            key={i}
            onClick={() => c.metricKey && onCardClick && onCardClick(c.metricKey)}
            className={`p-3.5 rounded-xl bg-white border border-slate-200 shadow-sm hover:shadow transition duration-150 flex flex-col justify-between ${
              c.metricKey ? 'cursor-pointer hover:border-slate-300' : ''
            }`}
          >
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs font-semibold text-slate-500 truncate">{c.title}</span>
              <div className={`p-1.5 rounded-lg border ${c.color}`}>
                <Icon className="w-3.5 h-3.5" />
              </div>
            </div>
            <div>
              <div className="text-2xl font-bold text-slate-800 tracking-tight">{c.value}</div>
              {c.subtitle && (
                <div className="text-[10px] text-slate-400 mt-0.5 truncate">{c.subtitle}</div>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
};
