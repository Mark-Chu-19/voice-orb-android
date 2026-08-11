# VoiceOrb 🔮

[![Android CI](https://github.com/Mark-Chu-19/voice-orb-android/actions/workflows/android.yml/badge.svg)](https://github.com/Mark-Chu-19/voice-orb-android/actions/workflows/android.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

An animated **voice-assistant orb** for Android — the living, breathing visual heart of a voice UI.

Built with nothing but `View` + `Canvas`: **zero dependencies**, a single self-driving render
loop, and an organic look that reacts to the user's voice in real time.

## Features

- **Four states, one continuous motion** — `IDLE`, `LISTENING`, `THINKING`, `SPEAKING`. Every
  state carries its own presentation scale and ornament (orbiting comet dots while thinking,
  expanding pulse rings while speaking). Transitions ease smoothly; there are no jump cuts.
- **Voice-reactive blob** — feed normalized microphone energy into `inputLevel` and the orb's
  outline ripples to the user's speech while listening.
- **Organic rendering** — the body is a circle deformed by three sine harmonics of different
  frequency and phase, wrapped in a breathing radial aura. It feels alive, not mechanical.
- **Battery-aware** — the animation loop runs on `postInvalidateOnAnimation` and stops itself
  whenever the view is off-screen.
- **CaptionBubbleView** — a companion caption bubble with fixed characters-per-line pagination,
  designed for CJK text where word-boundary wrapping has nothing to break on.
- **Zero dependencies** — plain Android framework APIs, `minSdk 21`.

## Usage

```kotlin
val orb = VoiceOrbView(context)
orb.state = VoiceOrbView.OrbState.LISTENING   // retargets the animation, never restarts it
orb.inputLevel = normalizedMicRms             // 0f..1f, honoured while LISTENING

val caption = CaptionBubbleView(context)
caption.showCaption("今天台北天氣晴朗，氣溫二十八度。")
caption.hideCaption()
```

Or from XML with custom colors:

```xml
<com.markchu.voiceorb.VoiceOrbView
    android:layout_width="220dp"
    android:layout_height="220dp"
    app:orbCoreColor="#409CFF"
    app:orbAuraColor="#409CFF"
    app:orbAccentColor="#8CD2FF" />
```

## Demo

The `demo` module is an interactive playground: four buttons drive the orb through its states and
a slider simulates microphone energy.

```bash
./gradlew :demo:installDebug
```

## Why I built this

I built the original version of this component for an in-vehicle voice assistant, where the orb
floats above every screen as the assistant's face. This repository is a from-scratch, generalized
rewrite of that idea — the parts I kept reaching for (state-scale easing, harmonic blob
deformation, CJK-friendly captions) distilled into a reusable, dependency-free library.

## License

[MIT](LICENSE)
