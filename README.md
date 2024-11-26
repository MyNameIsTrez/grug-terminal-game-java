# grug-terminal-game-java

## Running the game

1. Clone this repository and open it in VS Code.
2. Run `git submodule update --init` to clone the `grug/grug.c` and `grug/grug.h` files (for your own game you can just drop these files directly into your project).
3. Hit `Ctrl+Shift+B` to get a list of the available build tasks, and run the `Compile libgrug.so` task.
4. Hit `F5` to run the game.

## Compiling libglobal_library_loader.so

- Compile `Game.java` to `Game.class` and `Game.h` with `javac -h . Game.java`

- Compile `global_library_loader.c` to `global_library_loader.o` with `gcc global_library_loader.c -c -fPIC -o global_library_loader.o -I/usr/lib/jvm/jdk-23.0.1-oracle-x64/include -I/usr/lib/jvm/jdk-23.0.1-oracle-x64/include/linux`
- Link `global_library_loader.o` to `libglobal_library_loader.so` with `gcc global_library_loader.o -o libglobal_library_loader.so -shared -fPIC -lc`

- Compile `adapter.c` to `adapter.o` with `gcc adapter.c -c -o adapter.o`
- Link `adapter.o` to `libadapter.so` with `gcc adapter.o -o libadapter.so -shared`

- Compile `foo.c` to `foo.o` with `gcc foo.c -c -fPIC -o foo.o -I/usr/lib/jvm/jdk-23.0.1-oracle-x64/include -I/usr/lib/jvm/jdk-23.0.1-oracle-x64/include/linux`
- Link `foo.o` to `libfoo.so` with `gcc foo.o -o libfoo.so -shared -fPIC -lc`
