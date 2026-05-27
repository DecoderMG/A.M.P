'use client';

import React from 'react';
import { useAppStore } from '../../store/useAppStore';
import { Settings, ChevronRight, Activity, Music, ShieldCheck } from 'lucide-react';
import Link from 'next/link';

export default function SettingsPage() {
  const { adaptationMode, setAdaptationMode } = useAppStore();

  return (
    <main className="min-h-screen bg-slate-950 text-slate-100 p-6 font-sans">
      <div className="flex items-center gap-4 mb-12 mt-6">
        <div className="bg-slate-900 p-3 rounded-2xl border border-slate-800">
           <Settings className="text-neon-cyan" size={24} />
        </div>
        <h1 className="text-2xl font-bold">Settings</h1>
      </div>

      <div className="space-y-8">
        <section>
          <h2 className="text-[10px] font-mono font-bold text-slate-500 uppercase tracking-widest mb-4 flex items-center gap-2">
            <Activity size={12} /> Playback Adaptation
          </h2>
          <div className="bg-slate-900/50 rounded-3xl border border-slate-800 overflow-hidden">
            {(['tempo', 'energy', 'streaming', 'off'] as const).map((mode) => (
              <button
                key={mode}
                onClick={() => setAdaptationMode(mode)}
                className={`w-full flex items-center justify-between p-5 border-b border-slate-800 last:border-0 transition-colors ${
                  adaptationMode === mode ? 'bg-neon-cyan/5' : 'hover:bg-slate-800/30'
                }`}
              >
                <span className={`capitalize font-medium ${adaptationMode === mode ? 'text-neon-cyan' : 'text-slate-300'}`}>
                  {mode}
                </span>
                {adaptationMode === mode && (
                  <div className="w-2 h-2 rounded-full bg-neon-cyan shadow-[0_0_8px_rgba(0,229,255,0.8)]" />
                )}
              </button>
            ))}
          </div>
        </section>

        <section>
          <h2 className="text-[10px] font-mono font-bold text-slate-500 uppercase tracking-widest mb-4 flex items-center gap-2">
            <Music size={12} /> Mappings
          </h2>
          <div className="bg-slate-900/50 rounded-3xl border border-slate-800 p-2">
             <button className="w-full flex items-center justify-between p-4 rounded-2xl hover:bg-slate-800/30 text-slate-400">
                <span className="text-sm">Activity Playlists</span>
                <ChevronRight size={16} />
             </button>
             <button className="w-full flex items-center justify-between p-4 rounded-2xl hover:bg-slate-800/30 text-slate-400">
                <span className="text-sm">Calibration Settings</span>
                <ChevronRight size={16} />
             </button>
          </div>
        </section>

        <section>
           <div className="mt-12 flex items-center gap-2 text-slate-600">
              <ShieldCheck size={14} />
              <span className="text-[10px] font-mono uppercase tracking-tight">System Status: Modern Native Bridge Active</span>
           </div>
        </section>
      </div>

      <footer className="fixed bottom-8 left-0 w-full px-6">
         <Link 
           href="/now-playing"
           className="w-full block text-center py-4 bg-white text-slate-950 rounded-full font-bold shadow-lg shadow-white/5 active:scale-95 transition-all"
         >
            BACK TO PLAYER
         </Link>
      </footer>
    </main>
  );
}
