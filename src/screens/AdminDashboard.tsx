import React, { useState } from 'react';
import { User, TimeFilter, ScreenId } from '../types';
import { storageService } from '../services/storageService';
import { TimeFilterBar } from '../components/TimeFilterBar';
import { KpiCards } from '../components/KpiCards';
import { Charts } from '../components/Charts';
import {
  Users,
  Database,
  CloudSync,
  FileCheck,
  ChevronRight,
  TrendingUp,
  BarChart3
} from 'lucide-react';

interface AdminDashboardProps {
  currentUser: User;
  onNavigate: (screen: ScreenId, editFileId?: string) => void;
  onSelectRmDrilldown: (rmCode: string) => void;
}

export const AdminDashboard: React.FC<AdminDashboardProps> = ({
  currentUser,
  onNavigate,
  onSelectRmDrilldown
}) => {
  const [timeFilter, setTimeFilter] = useState<TimeFilter>('ALL_TIME');

  const files = storageService.getCustomerFiles();
  const rms = storageService.getUsers().filter(u => u.role === 'RM');
  const globalStats = storageService.calculateStats(files, timeFilter);

  // Compute RM-wise performance stats
  const rmPerformance = rms.map(rm => {
    const rmFiles = files.filter(f => f.assignedRmCode === rm.rmCode);
    const rmStats = storageService.calculateStats(rmFiles, timeFilter);
    return {
      rm,
      stats: rmStats
    };
  });

  return (
    <div className="space-y-6 text-xs">
      {/* Executive Command Banner */}
      <div className="bg-gradient-to-r from-ebl-navy-dark via-ebl-navy-primary to-slate-900 rounded-2xl p-6 text-white shadow-xl border border-ebl-navy-primary flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center space-x-2">
            <h1 className="text-xl sm:text-2xl font-black">EBL Central Administration</h1>
            <span className="bg-red-500 text-white font-bold px-2 py-0.5 rounded text-[11px]">
              ADMIN: {currentUser.rmCode}
            </span>
          </div>
          <p className="text-slate-300 mt-1">
            Global Relationship Management Oversight • All Branches & Officers
          </p>
        </div>

        {/* Quick Operations Row */}
        <div className="flex flex-wrap items-center gap-2">
          <button
            onClick={() => onNavigate('rm_mapping')}
            className="px-3.5 py-2 rounded-xl bg-white/10 hover:bg-white/20 border border-white/20 text-white font-semibold transition flex items-center space-x-1.5"
          >
            <Users className="w-3.5 h-3.5 text-ebl-gold" />
            <span>RM Mapping ({rms.length})</span>
          </button>
          <button
            onClick={() => onNavigate('global_database')}
            className="px-3.5 py-2 rounded-xl bg-white/10 hover:bg-white/20 border border-white/20 text-white font-semibold transition flex items-center space-x-1.5"
          >
            <Database className="w-3.5 h-3.5 text-blue-300" />
            <span>Global Database</span>
          </button>
          <button
            onClick={() => onNavigate('sheets_sync')}
            className="px-3.5 py-2 rounded-xl bg-ebl-gold text-ebl-navy-dark font-bold hover:bg-yellow-400 transition flex items-center space-x-1.5"
          >
            <span>Sheets Sync</span>
          </button>
        </div>
      </div>

      {/* Time Filter Bar */}
      <div className="bg-white p-2 rounded-xl border border-slate-200 shadow-sm">
        <TimeFilterBar selected={timeFilter} onChange={setTimeFilter} />
      </div>

      {/* Global KPI Cards */}
      <KpiCards stats={globalStats} onCardClick={() => onNavigate('global_database')} />

      {/* Global Distribution Charts */}
      <Charts stats={globalStats} files={files} />

      {/* RM-Wise Performance Breakdown Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="px-5 py-4 border-b border-slate-100 flex items-center justify-between">
          <div>
            <h3 className="font-bold text-sm text-slate-800">RM-Wise Performance Breakdown</h3>
            <p className="text-[11px] text-slate-400">Click on any officer row to inspect their customer records</p>
          </div>
          <button
            onClick={() => onNavigate('reports')}
            className="text-xs font-bold text-ebl-navy-primary hover:underline flex items-center space-x-1"
          >
            <span>Generate Full Report</span>
            <ChevronRight className="w-3.5 h-3.5" />
          </button>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200 text-slate-600 font-bold text-[11px] uppercase tracking-wider">
                <th className="py-3 px-4">RM Officer</th>
                <th className="py-3 px-3">Branch Location</th>
                <th className="py-3 px-3 text-center">Total</th>
                <th className="py-3 px-3 text-center text-teal-700">Collected</th>
                <th className="py-3 px-3 text-center text-blue-700">Submitted</th>
                <th className="py-3 px-3 text-center text-emerald-700">Approved</th>
                <th className="py-3 px-3 text-center text-red-700">Declined</th>
                <th className="py-3 px-3 text-center text-amber-700">Query</th>
                <th className="py-3 px-3 text-center text-rose-700">RTS</th>
                <th className="py-3 px-3 text-center text-purple-700">STC/Cond</th>
                <th className="py-3 px-3 text-center text-emerald-800">Cards (Y)</th>
                <th className="py-3 px-3 text-center text-orange-700">Pending Docs</th>
                <th className="py-3 px-4 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-xs">
              {rmPerformance.map(({ rm, stats }) => (
                <tr
                  key={rm.rmCode}
                  onClick={() => onSelectRmDrilldown(rm.rmCode)}
                  className="hover:bg-slate-50 cursor-pointer transition"
                >
                  <td className="py-3 px-4 whitespace-nowrap">
                    <div className="font-bold text-slate-800">{rm.name}</div>
                    <div className="text-[10px] text-slate-400 font-mono">RM {rm.rmCode}</div>
                  </td>
                  <td className="py-3 px-3 text-slate-600 whitespace-nowrap">
                    {rm.officeAddress}
                  </td>
                  <td className="py-3 px-3 text-center font-bold text-slate-900 bg-slate-50/50">
                    {stats.totalFiles}
                  </td>
                  <td className="py-3 px-3 text-center font-semibold text-teal-700">
                    {stats.collected}
                  </td>
                  <td className="py-3 px-3 text-center font-semibold text-blue-700">
                    {stats.submitted}
                  </td>
                  <td className="py-3 px-3 text-center font-bold text-emerald-700 bg-emerald-50/30">
                    {stats.approved}
                  </td>
                  <td className="py-3 px-3 text-center font-semibold text-red-700">
                    {stats.declined}
                  </td>
                  <td className="py-3 px-3 text-center font-semibold text-amber-700">
                    {stats.query}
                  </td>
                  <td className="py-3 px-3 text-center font-semibold text-rose-700">
                    {stats.returnToSource}
                  </td>
                  <td className="py-3 px-3 text-center font-semibold text-purple-700">
                    {stats.stc + stats.condition}
                  </td>
                  <td className="py-3 px-3 text-center font-bold text-emerald-800">
                    {stats.activeY}
                  </td>
                  <td className="py-3 px-3 text-center font-semibold text-orange-700">
                    {stats.pendingDocumentsCount}
                  </td>
                  <td className="py-3 px-4 text-right text-ebl-navy-primary font-bold">
                    <span>Inspect</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
