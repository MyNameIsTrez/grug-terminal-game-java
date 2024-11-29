package game;

class Game {
    private native void loadGlobalLibraries();

    private native void grugSetRuntimeErrorHandler();

    private native boolean errorHasChanged();

    private native boolean loadingErrorInGrugFile();

    private native String errorMsg();

    private native String errorPath();

    private native int errorGrugCLineNumber();

    private native boolean grugRegenerateModifiedMods();

    private native int getGrugReloadsSize();

    private native void fillReloadData(ReloadData reloadData, int i);

    private native void initGlobals(long initGlobalsFn, byte[] globals, int id);

    private native void init();

    private native void tool_onUse(long onFns);

    private ReloadData reloadData = new ReloadData();

    private Data data = new Data();

    public void runtimeErrorHandler(String reason, int type, String on_fn_name, String on_fn_path) {
        System.err.println("grug runtime error in " + on_fn_name + "(): " + reason + ", in " + on_fn_path);
    }

    public static void main(String[] args) {
        new Game();
    }

    public Game() {
        System.loadLibrary("global_library_loader");
        loadGlobalLibraries();

        System.loadLibrary("adapter");

        init();

        grugSetRuntimeErrorHandler();

        while (true) {
            if (grugRegenerateModifiedMods()) {
                if (errorHasChanged()) {
                    if (loadingErrorInGrugFile()) {
                        System.err.println("grug loading error: " + errorMsg() + ", in " + errorPath()
                                + " (detected in grug.c:" + errorGrugCLineNumber() + ")");
                    } else {
                        System.err.println("grug loading error: " + errorMsg() + " (detected in grug.c:"
                                + errorGrugCLineNumber() + ")");
                    }
                }

                sleep(1000);

                continue;
            }

            reloadModifiedEntities();

            // Since this is a simple terminal game, there are no PNGs/MP3s/etc.
            // reloadModifiedResources();

            update();

            System.out.println();

            sleep(1000);
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void reloadModifiedEntities() {
        int reloadsSize = getGrugReloadsSize();

        for (int reloadIndex = 0; reloadIndex < reloadsSize; reloadIndex++) {
            fillReloadData(reloadData, reloadIndex);

            GrugFile file = reloadData.file;

            for (int i = 0; i < 2; i++) {
                if (reloadData.oldDll == data.humanDlls[i]) {
                    data.humanDlls[i] = file.dll;

                    data.humanGlobals[i] = new byte[file.globalsSize];
                    initGlobals(file.initGlobalsFn, data.humanGlobals[i], i);
                }
            }

            for (int i = 0; i < 2; i++) {
                if (reloadData.oldDll == data.toolDlls[i]) {
                    data.toolDlls[i] = file.dll;

                    data.toolGlobals[i] = new byte[file.globalsSize];
                    initGlobals(file.initGlobalsFn, data.toolGlobals[i], i);

                    data.tools[i].onFns = file.onFns;
                }
            }
        }
    }

    private void update() {

    }
}

class ReloadData {
    public String path;
    public long oldDll;
    public GrugFile file = new GrugFile();

    public ReloadData() {
    }
}

class GrugFile {
    public String name;
    public long dll;
    public long defineFn;
    public int globalsSize;
    public long initGlobalsFn;
    public String defineType;
    public long onFns;
    public long resourceMtimes;

    public GrugFile() {
    }
}

class Data {
    public Human[] humans = { new Human(), new Human() };
    public long[] humanDlls = new long[2];
    public byte[][] humanGlobals = new byte[2][];

    public Tool[] tools = { new Tool(), new Tool() };
    public long[] toolDlls = new long[2];
    public byte[][] toolGlobals = new byte[2][];

    public State state = State.PICKING_PLAYER;

    public enum State {
        PICKING_PLAYER,
        PICKING_TOOLS,
        PICKING_OPPONENT,
        FIGHTING,
    }

    public GrugFile[] typeFiles;
    public int gold = 400;

    public boolean playerHasHuman = false;
    public boolean playerHasTool = false;

    public Data() {
    }
}

class Human {
    public String name = "";
    public int health = -1;
    public int buyGoldValue = -1;
    public int killGoldValue = -1;

    // These are not initialized by mods
    public int id = -1;
    public int opponentId = -1;
    public int maxHealth = -1;

    public Human() {
    }
}

class Tool {
    public String name = "";
    public int buyGoldValue = -1;

    // These are not initialized by mods
    public int humanParentId = 0;
    public long onFns = 0;

    public Tool() {
    }
}
