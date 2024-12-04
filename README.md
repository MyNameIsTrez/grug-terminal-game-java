# grug-terminal-game-java

## Running the game

1. Clone this repository and open it in VS Code.
2. Run `git submodule update --init` to clone the `grug/grug.c`, `grug/grug.h`, and `grug-adapter-for-java/generate.py` files (for your own game you can just drop these files directly into your project).
3. Hit `Ctrl+Shift+B` to get a list of the available build tasks, and run the `Recompile` task.
4. Hit `F5` to run the game.

Type `f` to toggle grug between `safe` and `fast` mode.

## Development notes

- `javap -s Game` prints the method signatures in `Game.java` (which shows `runtime_error_handler()` having the signature `(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;)V`)
