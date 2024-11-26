class Game {
    private native void load_global_libraries();

    private native void on_fire();

    public int health = 100;

    static {
        System.loadLibrary("global_library_loader");
        System.loadLibrary("mod");
    }

    public static void main(String[] args) {
        Game game = new Game();

        game.health = 50;

        game.load_global_libraries();

        game.on_fire();
    }

    public void printHealth() {
        System.out.println(health);
    }
}
