# grug-terminal-game-java

## Running the game

1. Clone this repository and open it in VS Code.
2. Run `git submodule update --init` to clone the `grug/grug.c` and `grug/grug.h` files (for your own game you can just drop these files directly into your project).
3. Hit `Ctrl+Shift+B` to get a list of the available build tasks, and run the `Compile libgrug.so` task.
4. Hit `F5` to run the game.

## Experimenting with shared objects using Foo.java

- Compile `Foo.java` to `Foo.class` and `Foo.h` with `javac -h . Foo.java`
- Compile `foo.c` to `foo.o` with `gcc foo.c -c -fPIC -o foo.o -I/usr/lib/jvm/jdk-23.0.1-oracle-x64/include -I/usr/lib/jvm/jdk-23.0.1-oracle-x64/include/linux`
- Link `foo.o` to `libfoo.so` with `gcc foo.o -o libfoo.so -shared -fPIC -lc`
- Compile `bar.c` to `bar.o` with `gcc bar.c -c -o bar.o`
- Link `bar.o` to `libbar.so` with `gcc bar.o -o libbar.so -shared`
- Run `Foo.java` with `java -Djava.library.path=. Foo.java`
