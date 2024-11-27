class Game {
    private native void loadGlobalLibraries();

    private native void grugSetRuntimeErrorHandler();

    private native boolean errorHasChanged();

    private native boolean loadingErrorInGrugFile();

    private native String errorMsg();

    private native String errorPath();

    private native int errorGrugCLineNumber();

    private native boolean grugRegenerateModifiedMods();

    private native void init();

    private native void tool_onUse(long onFns);

    public int health = 100;

    public void runtimeErrorHandler(String reason, int type, String on_fn_name, String on_fn_path) {
        System.err.println("grug runtime error in " + on_fn_name + "(): " + reason + ", in " + on_fn_path);
    }

    public static void main(String[] args) {
        Game game = new Game();

        game.health = 50;

        System.loadLibrary("global_library_loader");
        game.loadGlobalLibraries();

        System.loadLibrary("adapter");

        game.init();

        game.grugSetRuntimeErrorHandler();

        while (true) {
            if (game.grugRegenerateModifiedMods()) {
                if (game.errorHasChanged()) {
                    if (game.loadingErrorInGrugFile()) {
                        System.err.println("grug loading error: " + game.errorMsg() + ", in " + game.errorPath()
                                + " (detected in grug.c:" + game.errorGrugCLineNumber() + ")");
                    } else {
                        System.err.println("grug loading error: " + game.errorMsg() + " (detected in grug.c:"
                                + game.errorGrugCLineNumber() + ")");
                    }
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                continue;
            }

            // TODO: Call reloadModifiedEntities()

            // long onFns = 0x42; // TODO: Unhardcode

            // game.tool_onUse(onFns);

            // TODO: Call update()

            System.out.println();

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void printHealth() {
        System.out.println(health);
    }
}
