# AMP MVP UI/UX Specifications (Native Android)

## Design System: "Premium Energetic"
- **Background**: Slate-950 (`#020617`) - Deep, focused night mode.
- **Activity Primary**: Electric Orange (`#FF8C00`) - Used for active movement states and "energetic" pulses.
- **Music Primary**: Neon Cyan (`#00E5FF`) - Used for playback controls and music metadata.
- **Typography**: 
  - Header: Roboto Condensed Bold
  - Body/Metadata: Roboto Regular
  - Secondary/Mono: Roboto Thin / Space Mono (for confidence/status)

---

## 1. Onboarding & Mode Selection
### Screens
1. **Splash**: Fade-in "AMP" logo with an Electric Orange pulse animation.
2. **Setup Mode**: Two large, vertically stacked cards:
   - **Local Mode**: Icon: `ic_smartphone`, Label: "Local Library", Subtext: "Play music from your device storage."
   - **Streaming Companion**: Icon: `ic_radio`, Label: "Streaming Player", Subtext: "Connect to Spotify/YouTube Music."
3. **Permissions**: Contextual dialogs for "Physical Activity" and "Media/Files".

### States
- **Card Default**: Background Slate-900, Border Slate-800.
- **Card Selected**: Border Neon Cyan (2dp), subtle outer glow.

---

## 2. Now Playing UI (Unified)
### Layout
- **Header**: Current Activity (Icon + Text) in Electric Orange.
- **Visualizer**: Central circular visualizer (based on legacy `CircleBarRenderer`) using Neon Cyan.
- **Track Info**: Large title (24sp) and artist (16sp) below the visualizer.
- **Controls**:
  - Center: Large Play/Pause toggle (72dp hit target).
  - Sides: Next/Previous (48dp hit targets).
- **Adaptation Indicator**: A small pill-shaped toggle in the top-right: "AUTO-ADAPT: ON/OFF".

### Activity Feedback
- **Pulse Animation**: When a state change is detected (e.g., Walking -> Running), the background glow should pulse Electric Orange three times to notify the user without interrupting the workout.

---

## 3. Streaming Companion Setup
### Configuration
- **Provider Selection**: List of detected music players.
- **Activity Mapping**: A list view showing:
  - Stationary -> [Select Playlist/Mix]
  - Walking -> [Select Playlist/Mix]
  - Running -> [Select Playlist/Mix]
  - Cycling -> [Select Playlist/Mix]
- **Feedback**: When a mapping is active, the button shows the playlist name in Neon Cyan.

---

## 4. Calibration & Training
### UI
- **Guided Loop**: Circular progress bar (`Neon Cyan`) with 10s countdown.
- **Status Text**: "Stabilizing Activity Recognition..."
- **Completion**: "Calibrated!" with a large Neon Cyan checkmark.

---

## 5. Unsupported Provider States
### Visuals
- **Warning Banner**: "Provider Limitation: Tempo control unavailable in Streaming Mode. Using Playlist Adaptation instead." (Slate-700 background, White text).

---

## 6. Implementation Guidelines for Jett
- **Layouts**: Use `ConstraintLayout` for the Now Playing screen to ensure responsiveness across different Android screen sizes.
- **Animations**: Use `ObjectAnimator` for the pulse effects to keep them lightweight and performant.
- **Touch Targets**: Strictly adhere to a minimum 48dp hit area for all interactive elements, with 72dp for the primary play/pause button.
- **Navigation**: Use the existing Navigation Drawer for secondary settings, but keep the core workout controls on the main Home fragment.
