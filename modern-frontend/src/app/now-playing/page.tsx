'use client';

import React from 'react';
import { ActivityPulse } from '../../components/activity/ActivityPulse';
import { UnifiedPlayer } from '../../components/player/UnifiedPlayer';

export default function NowPlayingPage() {
  return (
    <main className="min-h-screen bg-slate-950 flex flex-col items-center justify-between py-12 px-6 overflow-hidden">
      {/* Dynamic Background Glow */}
      <div className="fixed top-0 left-0 w-full h-full pointer-events-none overflow-hidden -z-10">
        <div className="absolute top-[-10%] left-[-10%] w-[50%] h-[50%] bg-electric-orange/10 blur-[120px] rounded-full animate-pulse" />
        <div className="absolute bottom-[-10%] right-[-10%] w-[50%] h-[50%] bg-neon-cyan/10 blur-[120px] rounded-full animate-pulse" style={{ animationDelay: '1s' }} />
      </div>

      <ActivityPulse />
      
      <UnifiedPlayer />

      <footer className="mt-8 text-center">
        <p className="text-[10px] text-slate-700 font-mono tracking-widest uppercase">
          AMP MVP // PREMIUM ENERGETIC UI
        </p>
      </footer>
    </main>
  );
}
