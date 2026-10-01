import React from 'react';
import { TimeFilter } from '../types';

interface TimeFilterBarProps {
  selected: TimeFilter;
  onChange: (filter: TimeFilter) => void;
}

export const TimeFilterBar: React.FC<TimeFilterBarProps> = ({ selected, onChange }) => {
  const filters: { key: TimeFilter; label: string }[] = [
    { key: 'TODAY', label: 'Today' },
    { key: 'THIS_WEEK', label: 'This Week (Sat-Fri)' },
    { key: 'LAST_WEEK', label: 'Last Week' },
    { key: 'THIS_MONTH', label: 'This Month' },
    { key: 'LAST_MONTH', label: 'Last Month' },
    { key: 'ALL_TIME', label: 'All Time' },
  ];

  return (
    <div className="flex items-center space-x-1.5 overflow-x-auto py-2 px-1 text-xs no-scrollbar">
      <span className="text-slate-500 font-medium whitespace-nowrap mr-1">Period:</span>
      {filters.map(f => {
        const isSelected = f.key === selected;
        return (
          <button
            key={f.key}
            onClick={() => onChange(f.key)}
            className={`px-3 py-1.5 rounded-full font-medium whitespace-nowrap transition ${
              isSelected
                ? 'bg-ebl-navy-primary text-white shadow-sm'
                : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200'
            }`}
          >
            {f.label}
          </button>
        );
      })}
    </div>
  );
};
