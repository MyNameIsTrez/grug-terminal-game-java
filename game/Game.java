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
        this.loadGlobalLibraries();

        System.loadLibrary("adapter");

        this.init();

        this.grugSetRuntimeErrorHandler();

        while (true) {
            if (this.grugRegenerateModifiedMods()) {
                if (this.errorHasChanged()) {
                    if (this.loadingErrorInGrugFile()) {
                        System.err.println("grug loading error: " + this.errorMsg() + ", in " + this.errorPath()
                                + " (detected in grug.c:" + this.errorGrugCLineNumber() + ")");
                    } else {
                        System.err.println("grug loading error: " + this.errorMsg() + " (detected in grug.c:"
                                + this.errorGrugCLineNumber() + ")");
                    }
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                continue;
            }

            this.reloadModifiedEntities();

            // Since this is a simple terminal game, there are no PNGs/MP3s/etc.
            // reloadModifiedResources();

            // long onFns = 0x42; // TODO: Unhardcode

            // this.tool_onUse(onFns);

            update();

            System.out.println();

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void reloadModifiedEntities() {
        int reloadsSize = this.getGrugReloadsSize();

        for (int reloadIndex = 0; reloadIndex < reloadsSize; reloadIndex++) {
            this.fillReloadData(reloadData, reloadIndex);

            GrugFile file = reloadData.file;

            for (int i = 0; i < 2; i++) {
                if (reloadData.oldDll == data.humanDlls[i]) {
                    data.humanDlls[i] = file.dll;

                    data.humanGlobals[i] = new byte[file.globalsSize];
                    this.initGlobals(file.initGlobalsFn, data.humanGlobals[i], i);
                }
            }

            // TODO: Copy-paste the loop, to reload the tools
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
    public Human[] humans;
    public long[] humanDlls = new long[2];
    public byte[][] humanGlobals = new byte[2][];

    public Tool[] tools;
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
    public Human() {
    }
}

class Tool {
    public Tool() {
    }
}
