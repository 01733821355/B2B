import React from 'react';
import { CustomerFile, KpiStats } from '../types';

interface ChartsProps {
  stats: KpiStats;
  files: CustomerFile[];
}

export const Charts: React.FC<ChartsProps> = ({ stats, files }) => {
  const total = stats.totalFiles;

  // Status items
  const statusItems = [
    { label: 'Approved', count: stats.approved, color: '#10b981' },
    { label: 'Submitted', count: stats.submitted, color: '#3b82f6' },
    { label: 'Collected', count: stats.collected, color: '#14b8a6' },
    { label: 'Query', count: stats.query, color: '#f59e0b' },
    { label: 'RTS', count: stats.returnToSource, color: '#f43f5e' },
    { label: 'Condition/STC', count: stats.condition + stats.stc, color: '#8b5cf6' },
    { label: 'Declined', count: stats.declined, color: '#ef4444' }
  ].filter(s => s.count > 0);

  // Product distribution
  const productCounts: Record<string, number> = {};
  files.forEach(f => {
    if (!f.isDeleted) {
      productCounts[f.productType] = (productCounts[f.productType] || 0) + 1;
    }
  });

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 my-4">
      {/* 1. Status Breakdown Card */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
        <h3 className="text-sm font-bold text-slate-800 mb-3 flex items-center justify-between">
          <span>Application Status Distribution</span>
          <span className="text-xs font-normal text-slate-500">{total} Total Files</span>
        </h3>

        {total === 0 ? (
          <p className="text-xs text-slate-400 py-6 text-center">No files in current period</p>
        ) : (
          <div>
            {/* Segmented bar */}
            <div className="h-4 w-full bg-slate-100 rounded-full overflow-hidden flex mb-4">
              {statusItems.map((item, idx) => {
                const pct = (item.count / total) * 100;
                return (
                  <div
                    key={idx}
                    style={{ width: `${pct}%`, backgroundColor: item.color }}
                    className="h-full hover:opacity-90 transition-all"
                    title={`${item.label}: ${item.count} (${pct.toFixed(1)}%)`}
                  />
                );
              })}
            </div>

            {/* Legend list */}
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-2 text-xs">
              {statusItems.map((item, idx) => {
                const pct = ((item.count / total) * 100).toFixed(0);
                return (
                  <div key={idx} className="flex items-center space-x-2">
                    <span className="w-2.5 h-2.5 rounded-full flex-shrink-0" style={{ backgroundColor: item.color }}></span>
                    <span className="text-slate-600 truncate">{item.label}:</span>
                    <span className="font-bold text-slate-800">{item.count} ({pct}%)</span>
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </div>

      {/* 2. Product Distribution Card */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
        <h3 className="text-sm font-bold text-slate-800 mb-3 flex items-center justify-between">
          <span>Product Type Distribution</span>
          <span className="text-xs font-normal text-slate-500">{Object.keys(productCounts).length} Types</span>
        </h3>

        {Object.keys(productCounts).length === 0 ? (
          <p className="text-xs text-slate-400 py-6 text-center">No product data available</p>
        ) : (
          <div className="space-y-2">
            {Object.entries(productCounts).map(([prod, count], idx) => {
              const pct = total > 0 ? (count / total) * 100 : 0;
              return (
                <div key={idx} className="text-xs">
                  <div className="flex justify-between text-slate-700 font-medium mb-1">
                    <span>{prod}</span>
                    <span>{count} files ({pct.toFixed(0)}%)</span>
                  </div>
                  <div className="h-2 w-full bg-slate-100 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-ebl-navy-secondary rounded-full"
                      style={{ width: `${pct}%` }}
                    />
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};
