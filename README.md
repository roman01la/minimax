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
