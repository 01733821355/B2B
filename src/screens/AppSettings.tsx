import React, { useState } from 'react';
import { storageService } from '../services/storageService';
import { Settings, Save, Check } from 'lucide-react';

export const AppSettings: React.FC = () => {
  const settings = storageService.getSettings();

  const prodInitial = settings.find(s => s.settingKey === 'PRODUCT_TYPES')?.settingValue
    || 'Credit Card,B2B,Corporate Card,Split,Limit Enhancement';

  const docsInitial = settings.find(s => s.settingKey === 'PENDING_DOCS_LIST')?.settingValue
    || 'NID,TIN,Office ID,Salary Certificate,BS (Bank Statement),BIN,Trade License 2024-25,Trade License 2025-26,Trade License 2026-27,Loan Certificate,Card Statement (Month),Card Copy';

  const weekInitial = settings.find(s => s.settingKey === 'WEEK_START_DAY')?.settingValue
    || 'SATURDAY';

  const [products, setProducts] = useState(prodInitial);
  const [docs, setDocs] = useState(docsInitial);
  const [weekStart, setWeekStart] = useState(weekInitial);
  const [savedKey, setSavedKey] = useState<string | null>(null);

  const handleSave = (key: string, val: string) => {
    storageService.updateSetting(key, val);
    setSavedKey(key);
    setTimeout(() => setSavedKey(null), 2000);
  };

  return (
    <div className="space-y-6 text-xs max-w-3xl">
      <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm flex items-center justify-between">
        <div>
          <div className="flex items-center space-x-2">
            <Settings className="w-5 h-5 text-ebl-navy-primary" />
            <h1 className="text-base font-bold text-ebl-navy-dark">Application Dropdown & Workflow Configuration</h1>
          </div>
          <p className="text-slate-500 mt-0.5">
            Configure dropdown options, pending checklist items, and reporting calendars
          </p>
        </div>
      </div>

      {/* Product Types */}
      <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm space-y-3">
        <div>
          <h3 className="font-bold text-sm text-slate-800">Product Types List</h3>
          <p className="text-slate-500 text-[11px]">Comma-separated options available in the customer file entry form</p>
        </div>
        <textarea
          rows={2}
          value={products}
          onChange={e => setProducts(e.target.value)}
          className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-ebl-navy-primary focus:outline-none"
        />
        <div className="flex justify-end">
          <button
            onClick={() => handleSave('PRODUCT_TYPES', products)}
            className="flex items-center space-x-1.5 px-4 py-2 bg-ebl-navy-primary text-white rounded-lg font-bold hover:bg-ebl-navy-secondary transition"
          >
            {savedKey === 'PRODUCT_TYPES' ? <Check className="w-4 h-4 text-emerald-400" /> : <Save className="w-4 h-4" />}
            <span>{savedKey === 'PRODUCT_TYPES' ? 'Saved!' : 'Save Products'}</span>
          </button>
        </div>
      </div>

      {/* Pending Docs */}
      <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm space-y-3">
        <div>
          <h3 className="font-bold text-sm text-slate-800">Pending Documents Checklist</h3>
          <p className="text-slate-500 text-[11px]">Checklist items presented to RM officers during customer file onboarding</p>
        </div>
        <textarea
          rows={3}
          value={docs}
          onChange={e => setDocs(e.target.value)}
          className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-ebl-navy-primary focus:outline-none"
        />
        <div className="flex justify-end">
          <button
            onClick={() => handleSave('PENDING_DOCS_LIST', docs)}
            className="flex items-center space-x-1.5 px-4 py-2 bg-ebl-navy-primary text-white rounded-lg font-bold hover:bg-ebl-navy-secondary transition"
          >
            {savedKey === 'PENDING_DOCS_LIST' ? <Check className="w-4 h-4 text-emerald-400" /> : <Save className="w-4 h-4" />}
            <span>{savedKey === 'PENDING_DOCS_LIST' ? 'Saved!' : 'Save Checklist'}</span>
          </button>
        </div>
      </div>

      {/* Reporting Calendar */}
      <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm space-y-3">
        <div>
          <h3 className="font-bold text-sm text-slate-800">Reporting Calendar Rules</h3>
          <p className="text-slate-500 text-[11px]">Define week cycle (Saturday through Friday) and reporting timezone</p>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label className="block font-semibold text-slate-700 mb-1">Week Start Day</label>
            <input
              type="text"
              value={weekStart}
              onChange={e => setWeekStart(e.target.value)}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-ebl-navy-primary focus:outline-none"
            />
          </div>
          <div>
            <label className="block font-semibold text-slate-700 mb-1">Reporting Timezone</label>
            <input
              type="text"
              value="Asia/Dhaka (GMT+06:00)"
              disabled
              className="w-full px-3 py-2 border border-slate-200 rounded-lg bg-slate-100 text-slate-600 font-semibold"
            />
          </div>
        </div>
        <div className="flex justify-end pt-2">
          <button
            onClick={() => handleSave('WEEK_START_DAY', weekStart)}
            className="flex items-center space-x-1.5 px-4 py-2 bg-ebl-navy-primary text-white rounded-lg font-bold hover:bg-ebl-navy-secondary transition"
          >
            {savedKey === 'WEEK_START_DAY' ? <Check className="w-4 h-4 text-emerald-400" /> : <Save className="w-4 h-4" />}
            <span>{savedKey === 'WEEK_START_DAY' ? 'Saved!' : 'Save Calendar Rule'}</span>
          </button>
        </div>
      </div>
    </div>
  );
};
