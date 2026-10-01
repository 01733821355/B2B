import React, { useState } from 'react';
import { User, TimeFilter, CustomerFile, ScreenId } from '../types';
import { storageService } from '../services/storageService';
import { TimeFilterBar } from '../components/TimeFilterBar';
import { KpiCards } from '../components/KpiCards';
import { Charts } from '../components/Charts';
import { ApplicationStatusBadge, ActiveStatusBadge } from '../components/StatusBadge';
import { CustomerDetailModal } from '../components/CustomerDetailModal';
import { formatDateOnly } from '../utils/dateUtils';
import { Plus, Folder, FileText, ChevronRight, Download } from 'lucide-react';

interface RmDashboardProps {
  user: User;
  onNavigate: (screen: ScreenId, editFileId?: string) => void;
}

export const RmDashboard: React.FC<RmDashboardProps> = ({ user, onNavigate }) => {
  const [timeFilter, setTimeFilter] = useState<TimeFilter>('ALL_TIME');
  const [selectedFile, setSelectedFile] = useState<CustomerFile | null>(null);

  // Strictly enforce RM ownership: only access records where assignedRmCode equals user.rmCode
  const allFiles = storageService.getCustomerFiles();
  const rmFiles = allFiles.filter(f => f.assignedRmCode === user.rmCode && !f.isDeleted);
  const stats = storageService.calculateStats(rmFiles, timeFilter);

  return (
    <div className="space-y-6">
      {/* Banner Card */}
      <div className="bg-gradient-to-r from-ebl-navy-dark to-ebl-navy-primary rounded-2xl p-6 text-white shadow-lg border border-ebl-navy-primary flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center space-x-2">
            <h1 className="text-xl sm:text-2xl font-black">Welcome back, {user.name}</h1>
            <span className="bg-ebl-gold text-ebl-navy-dark px-2 py-0.5 rounded text-[11px] font-bold">
              RM: {user.rmCode}
            </span>
          </div>
          <p className="text-xs text-slate-300 mt-1">
            {user.officeAddress} • Relationship Officer Workspace
          </p>
        </div>

        {/* Quick Action Buttons */}
        <div className="flex items-center space-x-2">
          <button
            onClick={() => onNavigate('customer_form')}
            className="flex items-center space-x-1.5 px-4 py-2 bg-ebl-gold text-ebl-navy-dark rounded-xl font-bold text-xs hover:bg-yellow-400 transition shadow-sm"
          >
            <Plus className="w-4 h-4" />
            <span>New Customer File</span>
          </button>
          <button
            onClick={() => onNavigate('customer_list')}
            className="flex items-center space-x-1.5 px-4 py-2 bg-slate-800/80 text-white rounded-xl font-medium text-xs hover:bg-slate-700 border border-slate-600 transition"
          >
            <Folder className="w-4 h-4" />
            <span>My Files ({rmFiles.length})</span>
          </button>
          <button
            onClick={() => onNavigate('reports')}
            className="flex items-center space-x-1.5 px-3 py-2 bg-slate-800/80 text-white rounded-xl font-medium text-xs hover:bg-slate-700 border border-slate-600 transition"
            title="Download My Report"
          >
            <Download className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Time Filter Bar */}
      <div className="bg-white p-2 rounded-xl border border-slate-200 shadow-sm">
        <TimeFilterBar selected={timeFilter} onChange={setTimeFilter} />
      </div>

      {/* KPI Cards Grid */}
      <KpiCards stats={stats} onCardClick={() => onNavigate('customer_list')} />

      {/* Interactive Charts */}
      <Charts stats={stats} files={rmFiles} />

      {/* Recent Files Section */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="px-5 py-4 border-b border-slate-100 flex items-center justify-between">
          <h3 className="font-bold text-sm text-slate-800">My Recent Customer Files</h3>
          <button
            onClick={() => onNavigate('customer_list')}
            className="text-xs font-semibold text-ebl-navy-primary hover:underline flex items-center space-x-1"
          >
            <span>View All</span>
            <ChevronRight className="w-3.5 h-3.5" />
          </button>
        </div>

        {rmFiles.length === 0 ? (
          <div className="p-8 text-center text-xs text-slate-400">
            No customer files recorded yet. Tap "New Customer File" to create your first application.
          </div>
        ) : (
          <div className="divide-y divide-slate-100">
            {rmFiles.slice(0, 5).map(file => (
              <div
                key={file.fileId}
                onClick={() => setSelectedFile(file)}
                className="px-5 py-3 hover:bg-slate-50 flex items-center justify-between cursor-pointer transition text-xs"
              >
                <div>
                  <div className="flex items-center space-x-2">
                    <span className="font-bold text-slate-800">{file.customerName}</span>
                    <ApplicationStatusBadge status={file.applicationStatus} />
                    <ActiveStatusBadge status={file.activeStatus} />
                  </div>
                  <div className="text-[11px] text-slate-500 mt-0.5">
                    <span className="font-mono text-ebl-navy-primary font-medium">{file.fileId}</span> • {file.companyName} • {file.productType}
                  </div>
                </div>

                <div className="flex items-center space-x-4 text-[11px] text-slate-400">
                  <span>{formatDateOnly(file.updatedAt)}</span>
                  <ChevronRight className="w-4 h-4 text-slate-400" />
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Detail Modal */}
      {selectedFile && (
        <CustomerDetailModal
          file={selectedFile}
          currentUser={user}
          onClose={() => setSelectedFile(null)}
          onEdit={(id) => onNavigate('customer_form', id)}
        />
      )}
    </div>
  );
};
