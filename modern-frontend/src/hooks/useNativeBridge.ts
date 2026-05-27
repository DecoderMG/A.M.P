import { useEffect } from 'react';
import { useAppStore, ActivityState } from '../store/useAppStore';

declare global {
  interface Window {
    AndroidBridge?: {
      play: () => void;
      pause: () => void;
      next: () => void;
      previous: () => void;
      seek: (position: number) => void;
      setAdaptationEnabled: (enabled: boolean) => void;
    };
  }
}

export const useNativeBridge = () => {
  const { setActivity, updateMetadata } = useAppStore();

  useEffect(() => {
    // Expose methods for Android to call
    (window as any).onActivityUpdate = (state: string, confidence: number) => {
      setActivity(state as ActivityState, confidence);
    };

    (window as any).onMetadataUpdate = (json: string) => {
      try {
        const data = JSON.parse(json);
        updateMetadata(data);
      } catch (e) {
        console.error('Failed to parse metadata', e);
      }
    };

    return () => {
      delete (window as any).onActivityUpdate;
      delete (window as any).onMetadataUpdate;
    };
  }, [setActivity, updateMetadata]);

  const bridge = {
    play: () => window.AndroidBridge?.play(),
    pause: () => window.AndroidBridge?.pause(),
    next: () => window.AndroidBridge?.next(),
    previous: () => window.AndroidBridge?.previous(),
    seek: (pos: number) => window.AndroidBridge?.seek(pos),
  };

  return bridge;
};
