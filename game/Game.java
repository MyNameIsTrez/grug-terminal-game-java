package game;

import java.util.ArrayList;
import java.util.Scanner;

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

    private native void callInitGlobals(long initGlobalsFn, byte[] globals, int id);

    private native void init();

    private native void fillRootGrugDir(GrugDir root);

    private native void fillGrugDir(GrugDir dir, long parentDirAddress, int dirIndex);

    private native void fillGrugFile(GrugFile file, long parentDirAddress, int fileIndex);

    private native void callDefineFn(long defineFn);

    private native void tool_onUse(long onFns);

    private ReloadData reloadData = new ReloadData();

    private Data data = new Data();

    private Scanner scanner = new Scanner(System.in);

    private static final int PLAYER_INDEX = 0;
    private static final int OPPONENT_INDEX = 1;

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
                    callInitGlobals(file.initGlobalsFn, data.humanGlobals[i], i);
                }
            }

            for (int i = 0; i < 2; i++) {
                if (reloadData.oldDll == data.toolDlls[i]) {
                    data.toolDlls[i] = file.dll;

                    data.toolGlobals[i] = new byte[file.globalsSize];
                    callInitGlobals(file.initGlobalsFn, data.toolGlobals[i], i);

                    data.tools[i].onFns = file.onFns;
                }
            }
        }
    }

    private void update() {
        switch (data.state) {
            case PICKING_PLAYER -> pickPlayer();
            case PICKING_TOOLS -> pickTools();
            case PICKING_OPPONENT -> pickOpponent();
            case FIGHTING -> fight();
        }
    }

    private void pickPlayer() {
        System.out.println("You have " + data.gold + " gold");

        ArrayList<GrugFile> filesDefiningHuman = getTypeFiles("human");

        printPlayableHumans(filesDefiningHuman);

        System.out.println("Type the number next to the human you want to play as"
                + (data.playerHasHuman ? " (type 0 to skip)" : "") + ":\n");

        int playerNumber = readSize();
        if (playerNumber == -1) {
            return;
        }

        if (playerNumber == 0) {
            if (data.playerHasHuman) {
                data.state = State.PICKING_TOOLS;
                return;
            }

            System.err.println("The minimum number you can enter is 1");
            return;
        }

        if (playerNumber > data.typeFiles.size()) {
            System.err.println("The maximum number you can enter is " + data.typeFiles.size());
            return;
        }

        int playerIndex = playerNumber - 1;

        GrugFile file = filesDefiningHuman.get(playerIndex);

        callDefineFn(file.defineFn);
        Human human = new Human(EntityDefinitions.human);

        if (human.buyGoldValue > data.gold) {
            System.err.println("You don't have enough gold to pick that human");
            return;
        }

        data.gold -= human.buyGoldValue;

        human.id = PLAYER_INDEX;
        human.opponentId = OPPONENT_INDEX;

        human.maxHealth = human.health;

        data.humans[PLAYER_INDEX] = human;
        data.humanDlls[PLAYER_INDEX] = file.dll;

        data.humanGlobals[PLAYER_INDEX] = new byte[file.globalsSize];
        callInitGlobals(file.initGlobalsFn, data.humanGlobals[PLAYER_INDEX], PLAYER_INDEX);

        data.playerHasHuman = true;

        data.state = State.PICKING_TOOLS;
    }

    private void printPlayableHumans(ArrayList<GrugFile> filesDefiningHuman) {
        for (int i = 0; i < filesDefiningHuman.size(); i++) {
            GrugFile file = filesDefiningHuman.get(i);

            callDefineFn(file.defineFn);

            Human human = EntityDefinitions.human;

            System.out.println((i + 1) + ". " + human.name + ", costing " + human.buyGoldValue + " gold");
        }
    }

    private int readSize() {
        if (!scanner.hasNextInt()) {
            System.err.println("You didn't enter a valid number");
            return -1;
        }
        int n = scanner.nextInt();
        if (n < 0) {
            System.err.println("You can't enter a negative number");
            return -1;
        }
        return n;
    }

    private void pickTools() {

    }

    private void pickOpponent() {

    }

    private void fight() {

    }

    private ArrayList<GrugFile> getTypeFiles(String defineType) {
        data.typeFiles.clear();

        GrugDir root = new GrugDir();
        fillRootGrugDir(root);

        getTypeFilesImpl(root, defineType);

        return data.typeFiles;
    }

    private void getTypeFilesImpl(GrugDir dir, String defineType) {
        for (int i = 0; i < dir.dirsSize; i++) {
            GrugDir subdir = new GrugDir();
            fillGrugDir(subdir, dir.address, i);

            getTypeFilesImpl(subdir, defineType);
        }

        for (int i = 0; i < dir.filesSize; i++) {
            GrugFile file = new GrugFile();
            fillGrugFile(file, dir.address, i);

            if (file.defineType.equals(defineType)) {
                data.typeFiles.add(file);
            }
        }
    }
}

class ReloadData {
    public String path;
    public long oldDll;
    public GrugFile file = new GrugFile();

    public ReloadData() {
    }
}

class GrugDir {
    public String name;

    public ArrayList<GrugDir> dirs = new ArrayList<GrugDir>();
    public int dirsSize;

    public ArrayList<GrugFile> files = new ArrayList<GrugFile>();
    public int filesSize;

    public long address;

    public GrugDir() {
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

    public ArrayList<GrugFile> typeFiles = new ArrayList<GrugFile>();
    public int gold = 400;

    public boolean playerHasHuman = false;
    public boolean playerHasTool = false;

    public Data() {
    }
}

enum State {
    PICKING_PLAYER,
    PICKING_TOOLS,
    PICKING_OPPONENT,
    FIGHTING,
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

    public Human(Human other) {
        this.name = other.name;
        this.health = other.health;
        this.buyGoldValue = other.buyGoldValue;
        this.killGoldValue = other.killGoldValue;

        this.id = other.id;
        this.opponentId = other.opponentId;
        this.maxHealth = other.maxHealth;
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

class EntityDefinitions {
    public static Human human = new Human();
    public static Tool tool = new Tool();
}
