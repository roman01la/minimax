# [minimax](https://github.com/roman01la/minimax) - Win/Linux check branch

Minimalist 3D game engine in Clojure (This fork is for Win/Linux compatibility check)

## How to run checks

```bash
# compile shader first if you need, but most of them are already compiled (included)
# nu ./scripts/shaders.nu

nu ./scripts/start.nu check.empty
nu ./scripts/start.nu check.empty2
nu ./scripts/start.nu check.triangle_2d
nu ./scripts/start.nu
```

## Status

### LWJGL Version

- [x] Upgrade to LWJGL 3.3.3 (NOTE: 3.3.4 - 3.3.5 has known compatibility issues)
- [ ] Check compatibility with LWJGL 3.3.6

### basic checks (prerequisites)

- [x] empty
- [x] triangle (2d)

### fg.core

- [x] Build passing
- [x] Run through
- [x] Sound
- [x] Model loading (-> probably working)
- [x] Rendering compatibility
  - [x] Shadow
  - [x] Background
  - [x] Cloud
  - [x] Castle
  - [x] Stones

### Platforms

- [x] Windows / DX11
- [x] macOS / Metal 
- [x] Linux / Vulkan (Ubuntu)
- [ ] Linux / OpenGL (Currently disabling for shader compatibility, such as phong shading)

### Refactoring

- [ ] code cleaning for PR

-----

(Original README)

<img src="logo.png" width="88" />

Minimalist 3D game engine in Clojure

> In game theory, minimax is a decision rule used to minimize the worst-case potential loss; in other words, a player considers all of the best opponent responses to his strategies, and selects the strategy such that the opponent's best strategy gives a payoff as large as possible.
>
> — <cite>https://brilliant.org/wiki/minimax/</cite>

<img src="screenshot.jpg" style="max-width:480px;"/>

## How to run

_Tested only on Apple Silicon system_

- Make sure to replace [native packages in deps](https://github.com/roman01la/minimax/blob/main/deps.edn#L15-L23) with the ones matching your platform
- Compile shaders `./scripts/shaders` (shaders code might need changes depending on rendering backend choosen on your platform)
- Run the sample project `./scripts/start`

## Features

- Windowing and input handling
- GLTF loader
- Renderer
  - Blinn–Phong shading
  - Shadow maps
- Scene graph
- Audio playback
- Small and incomplete UI system
  - Scroll views
  - Buttons
  - Component system with local state
  - Flexbox layout
