# A.M.P Modernization Plan

## Phase 1: Build System & Project Modernization
- Upgrade Gradle wrapper to 8.11.1
- Upgrade Android Gradle Plugin to 8.8.2
- Update compileSdk to 35, targetSdk to 35, minSdk to 26
- Migrate from `compile` to `implementation` dependency configuration
- Migrate from jcenter() to mavenCentral() + google()
- Enable namespace in build.gradle (remove from manifest)
- Migrate support libraries to AndroidX
- Convert project to Kotlin DSL (build.gradle.kts) or at minimum update Groovy syntax
- Update all library modules (circleLayout, seekArc_library, slidinglib)

## Phase 2: AndroidX & Modern Architecture Migration
- Replace Activity with AppCompatActivity
- Replace Fragment with AndroidX Fragment
- Replace PreferenceFragment with PreferenceFragmentCompat
- Migrate to ViewBinding or Jetpack Compose
- Add ViewModel + LiveData/StateFlow for UI state
- Add runtime permission handling (RECORD_AUDIO, ACTIVITY_RECOGNITION, etc.)
- Migrate Navigation Drawer to Material NavigationView + DrawerLayout

## Phase 3: Activity Recognition Modernization
- Add Google Play Services dependency
- Integrate Google Activity Recognition API as primary detection
- Keep accelerometer-based DTW as fallback/enhancement
- Add ACTIVITY_RECOGNITION permission (runtime permission on API 29+)
- Implement TransitionAPI for activity changes (walking → running)
- Improve BPM detection from step cadence

## Phase 4: Music Playback Modernization
- Implement MediaSession + MediaBrowserService for proper media integration
- Add foreground service with notification media controls
- Add MediaStyle notification with playback controls
- Implement proper audio focus handling
- Add ExoPlayer (Media3) for improved playback
- Implement background playback support
- Add lock screen controls

## Phase 5: Spotify Integration
- Add Spotify Android SDK
- Implement Spotify Auth (OAuth PKCE flow)
- Browse/search Spotify catalog
- Control Spotify playback via Spotify App Remote
- Fetch BPM/tempo data from Spotify Audio Features API
- Build activity-matched playlists from Spotify library

## Phase 6: Additional Music Service Integrations
- YouTube Music integration (via Android Media Session)
- Apple Music (if Android SDK available)
- Local music library improvements (MediaStore API updates)
- Unified playback interface across all sources

## Phase 7: UI/UX Modernization
- Material Design 3 theming
- Modern activity visualization
- Improved onboarding/training flow
- Dark mode support
- Adaptive icons
