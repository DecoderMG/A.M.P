import React from 'react';
import { motion } from 'framer-motion';
import { useAppStore } from '../../store/useAppStore';
import { useNativeBridge } from '../../hooks/useNativeBridge';
import { Play, Pause, SkipForward, SkipBack, ListMusic } from 'lucide-react';

export const UnifiedPlayer: React.FC = () => {
  const { metadata, adaptationMode, isOverridden, toggleOverride } = useAppStore();
  const bridge = useNativeBridge();

  const progress = metadata.duration > 0 ? (metadata.position / metadata.duration) * 100 : 0;

  return (
    <div className="flex flex-col w-full max-w-md bg-slate-900/50 backdrop-blur-xl rounded-3xl border border-slate-800 p-6 shadow-2xl">
      {/* Header / Info */}
      <div className="flex justify-between items-start mb-6">
        <div>
          <h3 className="text-neon-cyan text-xs font-mono font-bold tracking-widest uppercase mb-1">
            Now Playing
          </h3>
          <h1 className="text-xl font-bold text-slate-100 truncate w-48">
            {metadata.title}
          </h1>
          <p className="text-slate-400 text-sm">
            {metadata.artist}
          </p>
        </div>
        <button 
          onClick={() => toggleOverride()}
          className={`px-3 py-1 rounded-full text-[10px] font-bold border transition-colors ${
            isOverridden 
              ? 'bg-electric-orange/20 border-electric-orange text-electric-orange' 
              : 'border-slate-700 text-slate-500'
          }`}
        >
          {isOverridden ? 'MANUAL' : 'AUTO-ADAPT'}
        </button>
      </div>

      {/* Album Art Placeholder */}
      <div className="aspect-square w-full rounded-2xl bg-gradient-to-br from-slate-800 to-slate-950 flex items-center justify-center mb-8 border border-slate-800 overflow-hidden relative">
         <ListMusic className="text-slate-700 w-32 h-32 absolute opacity-10" />
         <motion.div 
           animate={metadata.isPlaying ? { scale: [1, 1.05, 1] } : {}}
           transition={{ duration: 0.8, repeat: Infinity }}
           className="w-48 h-48 rounded-full bg-slate-800/50 border border-slate-700 flex items-center justify-center"
         >
           <div className="w-12 h-12 rounded-full bg-neon-cyan/20 blur-xl animate-pulse" />
         </motion.div>
      </div>

      {/* Progress Bar */}
      <div className="w-full mb-8">
        <div className="h-1 w-full bg-slate-800 rounded-full overflow-hidden">
          <motion.div 
            className="h-full bg-neon-cyan"
            animate={{ width: `${progress}%` }}
          />
        </div>
        <div className="flex justify-between mt-2 text-[10px] text-slate-500 font-mono">
          <span>{formatTime(metadata.position)}</span>
          <span>{formatTime(metadata.duration)}</span>
        </div>
      </div>

      {/* Controls - Large Touch Targets */}
      <div className="flex items-center justify-around">
        <button 
          onClick={() => bridge.previous()}
          className="w-16 h-16 rounded-full flex items-center justify-center text-slate-300 active:bg-slate-800 active:scale-95 transition-all"
        >
          <SkipBack size={32} />
        </button>

        <button 
          onClick={() => metadata.isPlaying ? bridge.pause() : bridge.play()}
          className="w-24 h-24 rounded-full bg-neon-cyan text-slate-950 flex items-center justify-center shadow-lg shadow-neon-cyan/20 active:scale-90 transition-all"
        >
          {metadata.isPlaying ? <Pause size={48} fill="currentColor" /> : <Play size={48} fill="currentColor" className="ml-2" />}
        </button>

        <button 
          onClick={() => bridge.next()}
          className="w-16 h-16 rounded-full flex items-center justify-center text-slate-300 active:bg-slate-800 active:scale-95 transition-all"
        >
          <SkipForward size={32} />
        </button>
      </div>
      
      {/* Adaptation Mode Indicator */}
      <div className="mt-8 flex justify-center gap-2">
        {['tempo', 'energy', 'streaming'].map((mode) => (
          <span 
            key={mode}
            className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded border ${
              adaptationMode === mode ? 'border-neon-cyan text-neon-cyan bg-neon-cyan/10' : 'border-slate-800 text-slate-600'
            }`}
          >
            {mode.toUpperCase()}
          </span>
        ))}
      </div>
    </div>
  );
};

function formatTime(ms: number) {
  const totalSeconds = Math.floor(ms / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}:${seconds.toString().padStart(2, '0')}`;
}
