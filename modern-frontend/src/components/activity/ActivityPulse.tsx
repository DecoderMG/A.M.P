import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useAppStore } from '../../store/useAppStore';
import { Activity, Footprints, Zap, Bike, Dumbbell, Car, HelpCircle } from 'lucide-react';

const activityConfig = {
  stationary: { icon: Activity, color: 'text-slate-400', label: 'Stationary' },
  walking: { icon: Footprints, color: 'text-green-400', label: 'Walking' },
  running: { icon: Zap, color: 'text-electric-orange', label: 'Running' },
  cycling: { icon: Bike, color: 'text-neon-cyan', label: 'Cycling' },
  workout: { icon: Dumbbell, color: 'text-purple-400', label: 'Workout' },
  vehicle: { icon: Car, color: 'text-blue-400', label: 'In Vehicle' },
  unknown: { icon: HelpCircle, color: 'text-slate-500', label: 'Unknown' },
};

export const ActivityPulse: React.FC = () => {
  const { currentActivity, activityConfidence } = useAppStore();
  const config = activityConfig[currentActivity] || activityConfig.unknown;
  const Icon = config.icon;

  return (
    <div className="flex flex-col items-center justify-center p-8">
      <div className="relative">
        <AnimatePresence mode="wait">
          <motion.div
            key={currentActivity}
            initial={{ scale: 0.8, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            exit={{ scale: 1.2, opacity: 0 }}
            className={`relative z-10 w-32 h-32 rounded-full bg-slate-900 border-4 border-slate-800 flex items-center justify-center ${config.color}`}
          >
            <Icon size={64} />
          </motion.div>
        </AnimatePresence>
        
        {/* Pulse Rings */}
        <motion.div
          animate={{ scale: [1, 1.5, 2], opacity: [0.5, 0.2, 0] }}
          transition={{ duration: 2, repeat: Infinity, ease: "easeOut" }}
          className={`absolute inset-0 rounded-full border-2 ${config.color} opacity-20`}
        />
        <motion.div
          animate={{ scale: [1, 1.3, 1.6], opacity: [0.3, 0.1, 0] }}
          transition={{ duration: 2, repeat: Infinity, ease: "easeOut", delay: 0.5 }}
          className={`absolute inset-0 rounded-full border-2 ${config.color} opacity-20`}
        />
      </div>

      <motion.div 
        initial={{ y: 10, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        className="mt-6 text-center"
      >
        <h2 className="text-2xl font-bold tracking-wider uppercase text-slate-100">
          {config.label}
        </h2>
        <div className="mt-2 h-1.5 w-48 bg-slate-800 rounded-full overflow-hidden">
          <motion.div 
            initial={{ width: 0 }}
            animate={{ width: `${activityConfidence * 100}%` }}
            className={`h-full bg-gradient-to-r from-slate-700 to-neon-cyan`}
          />
        </div>
        <p className="mt-2 text-xs text-slate-500 font-mono">
          CONFIDENCE: {Math.round(activityConfidence * 100)}%
        </p>
      </motion.div>
    </div>
  );
};
