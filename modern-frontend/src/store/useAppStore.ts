import { create } from 'zustand';

export type ActivityState = 'stationary' | 'walking' | 'running' | 'cycling' | 'workout' | 'vehicle' | 'unknown';

interface PlayerMetadata {
  title: string;
  artist: string;
  album: string;
  duration: number;
  position: number;
  isPlaying: boolean;
}

interface AppState {
  currentActivity: ActivityState;
  previousActivity: ActivityState | null;
  activityConfidence: number;
  metadata: PlayerMetadata;
  adaptationMode: 'tempo' | 'energy' | 'streaming' | 'off';
  isOverridden: boolean;
  
  // Actions
  setActivity: (activity: ActivityState, confidence: number) => void;
  updateMetadata: (metadata: Partial<PlayerMetadata>) => void;
  setAdaptationMode: (mode: AppState['adaptationMode']) => void;
  toggleOverride: (override?: boolean) => void;
}

export const useAppStore = create<AppState>((set) => ({
  currentActivity: 'unknown',
  previousActivity: null,
  activityConfidence: 0,
  metadata: {
    title: 'No Track',
    artist: 'Unknown Artist',
    album: 'Unknown Album',
    duration: 0,
    position: 0,
    isPlaying: false,
  },
  adaptationMode: 'off',
  isOverridden: false,

  setActivity: (activity, confidence) => set((state) => ({
    previousActivity: state.currentActivity !== activity ? state.currentActivity : state.previousActivity,
    currentActivity: activity,
    activityConfidence: confidence,
  })),

  updateMetadata: (newMetadata) => set((state) => ({
    metadata: { ...state.metadata, ...newMetadata },
  })),

  setAdaptationMode: (mode) => set({ adaptationMode: mode }),

  toggleOverride: (override) => set((state) => ({
    isOverridden: override !== undefined ? override : !state.isOverridden,
  })),
}));
