import React, { useState, useEffect } from 'react';
import { User, ScreenId } from './types';
import { storageService } from './services/storageService';
import { Navbar } from './components/Navbar';
import { ChangePasswordModal } from './components/ChangePasswordModal';
import { LoginScreen } from './screens/LoginScreen';
import { RmDashboard } from './screens/RmDashboard';
import { CustomerFileForm } from './screens/CustomerFileForm';
import { CustomerFileList } from './screens/CustomerFileList';
import { AdminDashboard } from './screens/AdminDashboard';
import { RmMapping } from './screens/RmMapping';
import { Reports } from './screens/Reports';
import { GoogleSheetsSync } from './screens/GoogleSheetsSync';
import { AuditLogs } from './screens/AuditLogs';
import { AppSettings } from './screens/AppSettings';
import { MentorDashboard } from './screens/MentorDashboard';
import {
  LayoutDashboard,
  FilePlus,
  FolderOpen,
  Users,
  Database,
  FileSpreadsheet,
  CloudSync,
  History,
  Settings,
  Shield,
  Trash2
} from 'lucide-react';

export function App() {
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [currentScreen, setCurrentScreen] = useState<ScreenId>('login');
  const [editFileId, setEditFileId] = useState<string | undefined>(undefined);
  const [showChangePasswordModal, setShowChangePasswordModal] = useState(false);
  const [showTrashBinOnly, setShowTrashBinOnly] = useState(false);
  const [isReady, setIsReady] = useState(false);

  useEffect(() => {
    storageService.init().then(() => {
      const user = storageService.getCurrentUser();
      if (user) {
        setCurrentUser(user);
        routeToHome(user.role);
      } else {
        setCurrentScreen('login');
      }
      setIsReady(true);
    });
  }, []);

  const routeToHome = (role: string) => {
    setShowTrashBinOnly(false);
    if (role === 'ADMIN') {
      setCurrentScreen('admin_dashboard');
    } else if (role === 'MENTOR') {
      setCurrentScreen('mentor_dashboard');
    } else {
      setCurrentScreen('rm_dashboard');
    }
  };

  const handleLoginSuccess = (user: User) => {
    setCurrentUser(user);
    routeToHome(user.role);
  };

  const handleLogout = () => {
    storageService.setCurrentUser(null);
    setCurrentUser(null);
    setCurrentScreen('login');
  };

  const handleNavigate = (screen: ScreenId, targetEditFileId?: string) => {
    setShowTrashBinOnly(false);
    setEditFileId(targetEditFileId);
    setCurrentScreen(screen);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleOpenTrashBin = () => {
    setShowTrashBinOnly(true);
    setCurrentScreen('global_database');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  if (!isReady) {
    return (
      <div className="min-h-screen bg-slate-900 flex items-center justify-center text-white">
        <div className="text-center space-y-3">
          <div className="w-12 h-12 rounded-full bg-ebl-gold text-ebl-navy-dark font-black flex items-center justify-center text-lg mx-auto animate-pulse">
            EBL
          </div>
          <p className="text-sm font-semibold text-slate-300">Loading Secure Portal...</p>
        </div>
      </div>
    );
  }

  if (!currentUser) {
    return <LoginScreen onLoginSuccess={handleLoginSuccess} />;
  }

  // Navigation Items per Role
  const navItems = currentUser.role === 'RM'
    ? [
        { id: 'rm_dashboard' as ScreenId, label: 'Dashboard', icon: LayoutDashboard },
        { id: 'customer_form' as ScreenId, label: 'New File', icon: FilePlus },
        { id: 'customer_list' as ScreenId, label: 'My Files', icon: FolderOpen },
        { id: 'reports' as ScreenId, label: 'Reports', icon: FileSpreadsheet },
      ]
    : currentUser.role === 'ADMIN'
    ? [
        { id: 'admin_dashboard' as ScreenId, label: 'Dashboard', icon: LayoutDashboard },
        { id: 'global_database' as ScreenId, label: 'Database', icon: Database },
        { id: 'rm_mapping' as ScreenId, label: 'RM Mapping', icon: Users },
        { id: 'reports' as ScreenId, label: 'Reports', icon: FileSpreadsheet },
        { id: 'sheets_sync' as ScreenId, label: 'Sheets Sync', icon: CloudSync },
        { id: 'audit_logs' as ScreenId, label: 'Audit Trail', icon: History },
        { id: 'app_settings' as ScreenId, label: 'Settings', icon: Settings },
      ]
    : [ // MENTOR
        { id: 'mentor_dashboard' as ScreenId, label: 'Console', icon: Shield },
        { id: 'global_database' as ScreenId, label: 'All Files', icon: Database },
        { id: 'rm_mapping' as ScreenId, label: 'RM Mapping', icon: Users },
        { id: 'reports' as ScreenId, label: 'Reports', icon: FileSpreadsheet },
        { id: 'sheets_sync' as ScreenId, label: 'Sheets Sync', icon: CloudSync },
        { id: 'audit_logs' as ScreenId, label: 'Audit Trail', icon: History },
        { id: 'app_settings' as ScreenId, label: 'Settings', icon: Settings },
      ];

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col text-slate-800">
      {/* Top Navbar */}
      <Navbar
        user={currentUser}
        onLogout={handleLogout}
        onChangePassword={() => setShowChangePasswordModal(true)}
        onNavigate={handleNavigate}
        currentScreen={currentScreen}
      />

      {/* Navigation Sub-bar */}
      <div className="bg-white border-b border-slate-200 shadow-sm sticky top-16 z-30 no-print">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center space-x-1 sm:space-x-2 overflow-x-auto py-2 no-scrollbar">
            {navItems.map(item => {
              const Icon = item.icon;
              const isSelected = currentScreen === item.id && (item.id !== 'customer_form' || !editFileId);
              return (
                <button
                  key={item.id}
                  onClick={() => handleNavigate(item.id)}
                  className={`flex items-center space-x-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition ${
                    isSelected
                      ? 'bg-ebl-navy-primary text-white shadow-sm'
                      : 'text-slate-600 hover:bg-slate-100'
                  }`}
                >
                  <Icon className="w-3.5 h-3.5" />
                  <span>{item.label}</span>
                </button>
              );
            })}
          </div>
        </div>
      </div>

      {/* Main Content Area */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6">
        {currentScreen === 'rm_dashboard' && (
          <RmDashboard
            user={currentUser}
            onNavigate={handleNavigate}
          />
        )}

        {currentScreen === 'customer_form' && (
          <CustomerFileForm
            currentUser={currentUser}
            editFileId={editFileId}
            onNavigate={handleNavigate}
            onSuccess={() => handleNavigate(currentUser.role === 'RM' ? 'customer_list' : 'global_database')}
          />
        )}

        {currentScreen === 'customer_list' && (
          <CustomerFileList
            currentUser={currentUser}
            onNavigate={handleNavigate}
            showDeletedOnly={false}
          />
        )}

        {currentScreen === 'admin_dashboard' && (
          <AdminDashboard
            currentUser={currentUser}
            onNavigate={handleNavigate}
            onSelectRmDrilldown={(rmCode) => {
              handleNavigate('global_database');
            }}
          />
        )}

        {currentScreen === 'mentor_dashboard' && (
          <MentorDashboard
            currentUser={currentUser}
            onNavigate={handleNavigate}
            onOpenTrashBin={handleOpenTrashBin}
          />
        )}

        {currentScreen === 'global_database' && (
          <CustomerFileList
            currentUser={currentUser}
            onNavigate={handleNavigate}
            showDeletedOnly={showTrashBinOnly}
          />
        )}

        {currentScreen === 'rm_mapping' && (
          <RmMapping
            currentUser={currentUser}
            onNavigate={handleNavigate}
          />
        )}

        {currentScreen === 'reports' && (
          <Reports
            currentUser={currentUser}
          />
        )}

        {currentScreen === 'sheets_sync' && (
          <GoogleSheetsSync />
        )}

        {currentScreen === 'audit_logs' && (
          <AuditLogs />
        )}

        {currentScreen === 'app_settings' && (
          <AppSettings />
        )}
      </main>

      {/* Forced Password Change Modal on first login */}
      {currentUser.mustChangePassword && (
        <ChangePasswordModal
          user={currentUser}
          isForced={true}
          onClose={() => {}}
          onSuccess={(updated) => {
            setCurrentUser(updated);
          }}
        />
      )}

      {/* User-Triggered Change Password Modal */}
      {showChangePasswordModal && !currentUser.mustChangePassword && (
        <ChangePasswordModal
          user={currentUser}
          isForced={false}
          onClose={() => setShowChangePasswordModal(false)}
          onSuccess={(updated) => {
            setCurrentUser(updated);
            setShowChangePasswordModal(false);
          }}
        />
      )}
    </div>
  );
}

export default App;
