import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { Smartphone, Radio, Check } from 'lucide-react';

interface SourceSetupProps {
  onComplete: (source: 'local' | 'streaming') => void;
}

export const SourceSetup: React.FC<SourceSetupProps> = ({ onComplete }) => {
  const [selected, setSelected] = useState<'local' | 'streaming' | null>(null);

  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-slate-950 p-8 text-slate-100">
      <motion.h1 
        initial={{ y: -20, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        className="text-3xl font-bold text-center mb-4"
      >
        Choose Your <span className="text-neon-cyan">Source</span>
      </motion.h1>
      <motion.p 
        initial={{ y: -10, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        transition={{ delay: 0.1 }}
        className="text-slate-400 text-center mb-12 max-w-xs"
      >
        How do you want to listen during your workout?
      </motion.p>

      <div className="grid grid-cols-1 gap-6 w-full max-w-xs">
        <motion.button
          whileTap={{ scale: 0.95 }}
          onClick={() => setSelected('local')}
          className={`relative p-6 rounded-3xl border-2 transition-all flex flex-col items-center gap-4 ${
            selected === 'local' ? 'border-neon-cyan bg-neon-cyan/5' : 'border-slate-800 bg-slate-900/50'
          }`}
        >
          <div className={`p-4 rounded-2xl ${selected === 'local' ? 'bg-neon-cyan text-slate-950' : 'bg-slate-800 text-slate-400'}`}>
            <Smartphone size={32} />
          </div>
          <div className="text-center">
            <h3 className="font-bold text-lg">Local Files</h3>
            <p className="text-xs text-slate-500 mt-1">Play music stored on your device with tempo control.</p>
          </div>
          {selected === 'local' && (
            <div className="absolute top-4 right-4 bg-neon-cyan rounded-full p-1 text-slate-950">
              <Check size={16} strokeWidth={4} />
            </div>
          )}
        </motion.button>

        <motion.button
          whileTap={{ scale: 0.95 }}
          onClick={() => setSelected('streaming')}
          className={`relative p-6 rounded-3xl border-2 transition-all flex flex-col items-center gap-4 ${
            selected === 'streaming' ? 'border-electric-orange bg-electric-orange/5' : 'border-slate-800 bg-slate-900/50'
          }`}
        >
          <div className={`p-4 rounded-2xl ${selected === 'streaming' ? 'bg-electric-orange text-slate-950' : 'bg-slate-800 text-slate-400'}`}>
            <Radio size={32} />
          </div>
          <div className="text-center">
            <h3 className="font-bold text-lg">Streaming Companion</h3>
            <p className="text-xs text-slate-500 mt-1">Connect to Spotify or other players for activity-based playlists.</p>
          </div>
          {selected === 'streaming' && (
            <div className="absolute top-4 right-4 bg-electric-orange rounded-full p-1 text-slate-950">
              <Check size={16} strokeWidth={4} />
            </div>
          )}
        </motion.button>
      </div>

      <motion.button
        disabled={!selected}
        whileTap={{ scale: 0.95 }}
        onClick={() => selected && onComplete(selected)}
        className={`mt-12 w-full max-w-xs py-4 rounded-full font-bold transition-all ${
          selected 
            ? 'bg-white text-slate-950 shadow-lg shadow-white/10' 
            : 'bg-slate-800 text-slate-600 cursor-not-allowed'
        }`}
      >
        CONTINUE
      </motion.button>
    </div>
  );
};
