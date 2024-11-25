class Game {
    private native boolean grug_regenerate_modified_mods();

    static {
        System.loadLibrary("grug");
    }

    public static void main(String[] args) {
        Game game = new Game();

        System.out.println("foo");
        game.grug_regenerate_modified_mods();
        System.out.println("bar");
    }
}
