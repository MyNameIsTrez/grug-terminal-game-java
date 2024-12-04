# grug terminal game: Java

See [my blog post](https://mynameistrez.github.io/2024/02/29/creating-the-perfect-modding-language.html) for an introduction to the grug modding language.

## Running the game

1. Clone this repository and open it in VS Code.
2. Run `git submodule update --init` to clone the `grug/grug.c`, `grug/grug.h`, and `grug-adapter-for-java/generate.py` files (for your own game you can just drop these files directly into your project).
3. Hit `Ctrl+Shift+B` to get a list of the available build tasks, and run the `Recompile` task.
4. Hit `F5` to run the game.

Type `f` to toggle grug between `safe` and `fast` mode.
