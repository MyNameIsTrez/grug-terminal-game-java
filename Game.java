class Game {
    private native void load_global_libraries();

    private native void foo();

    static {
        System.loadLibrary("global_library_loader");
        System.loadLibrary("foo");
    }

    public static void main(String[] args) {
        Game game = new Game();

        game.load_global_libraries();

        game.foo();
    }
}
