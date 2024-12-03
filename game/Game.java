package game;

import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import java.lang.Math;

class Game {
    private native void loadGlobalLibraries();

    private native void grugSetRuntimeErrorHandler();

    private native boolean errorHasChanged();

    private native boolean loadingErrorInGrugFile();

    private native String errorMsg();

    private native String errorPath();

    private native String onFnName();

    private native String onFnPath();

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

    private native void toggleOnFnsMode();

    private native boolean areOnFnsInSafeMode();

    private ReloadData reloadData = new ReloadData();

    private Data data = new Data();

    private Scanner scanner = new Scanner(System.in);

    private static final int PLAYER_INDEX = 0;
    private static final int OPPONENT_INDEX = 1;

    Random rand = new Random();

    private native boolean tool_hasOnUse(long onFns);

    private native void tool_onUse(long onFns, byte[] globals);

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

                sleep(1);

                continue;
            }

            reloadModifiedEntities();

            // Since this is a simple terminal game, there are no PNGs/MP3s/etc.
            // reloadModifiedResources();

            update();

            System.out.println();

            sleep(1);
        }
    }

    private void sleep(long seconds) {
        try {
            Thread.sleep(seconds * 1000);
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
        System.out.println("You have " + data.gold + " gold\n");

        ArrayList<GrugFile> filesDefiningHuman = getTypeFiles("human");

        printPlayableHumans(filesDefiningHuman);

        System.out.println("Type the number next to the human you want to play as"
                + (data.playerHasHuman ? " (type 0 to skip)" : "") + ":");

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

        if (playerNumber > filesDefiningHuman.size()) {
            System.err.println("The maximum number you can enter is " + filesDefiningHuman.size());
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

        System.out.println();
    }

    private int readSize() {
        if (scanner.hasNext("f")) {
            scanner.next();
            toggleOnFnsMode();
            System.out.println("Toggled grug to " + (areOnFnsInSafeMode() ? "safe" : "fast") + " mode");
            return -1;
        }

        if (!scanner.hasNextInt()) {
            scanner.next();
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
        System.out.println("You have " + data.gold + " gold\n");

        ArrayList<GrugFile> filesDefiningTool = getTypeFiles("tool");

        printTools(filesDefiningTool);

        System.out.println("Type the number next to the tool you want to buy"
                + (data.playerHasTool ? " (type 0 to skip)" : "") + ":");

        int toolNumber = readSize();
        if (toolNumber == -1) {
            return;
        }

        if (toolNumber == 0) {
            if (data.playerHasTool) {
                data.state = State.PICKING_OPPONENT;
                return;
            }

            System.err.println("The minimum number you can enter is 1");
            return;
        }

        if (toolNumber > filesDefiningTool.size()) {
            System.err.println("The maximum number you can enter is " + filesDefiningTool.size());
            return;
        }

        int toolIndex = toolNumber - 1;

        GrugFile file = filesDefiningTool.get(toolIndex);

        callDefineFn(file.defineFn);
        Tool tool = new Tool(EntityDefinitions.tool);

        tool.onFns = file.onFns;

        if (tool.buyGoldValue > data.gold) {
            System.err.println("You don't have enough gold to buy that tool");
            return;
        }

        data.gold -= tool.buyGoldValue;

        tool.humanParentId = PLAYER_INDEX;

        data.tools[PLAYER_INDEX] = tool;
        data.toolDlls[PLAYER_INDEX] = file.dll;

        data.toolGlobals[PLAYER_INDEX] = new byte[file.globalsSize];
        callInitGlobals(file.initGlobalsFn, data.toolGlobals[PLAYER_INDEX], PLAYER_INDEX);

        data.playerHasTool = true;
    }

    private void printTools(ArrayList<GrugFile> filesDefiningTool) {
        for (int i = 0; i < filesDefiningTool.size(); i++) {
            GrugFile file = filesDefiningTool.get(i);

            callDefineFn(file.defineFn);

            Tool tool = EntityDefinitions.tool;

            System.out.println((i + 1) + ". " + tool.name + ", costing " + tool.buyGoldValue + " gold");
        }

        System.out.println();
    }

    private void pickOpponent() {
        System.out.println("You have " + data.gold + " gold\n");

        ArrayList<GrugFile> filesDefiningHuman = getTypeFiles("human");

        printOpponentHumans(filesDefiningHuman);

        System.out.println("Type the number next to the human you want to fight:");

        int opponentNumber = readSize();
        if (opponentNumber == -1) {
            return;
        }

        if (opponentNumber == 0) {
            System.err.println("The minimum number you can enter is 1");
            return;
        }

        if (opponentNumber > filesDefiningHuman.size()) {
            System.err.println("The maximum number you can enter is " + filesDefiningHuman.size());
            return;
        }

        int opponentIndex = opponentNumber - 1;

        GrugFile file = filesDefiningHuman.get(opponentIndex);

        callDefineFn(file.defineFn);
        Human human = new Human(EntityDefinitions.human);

        human.id = OPPONENT_INDEX;
        human.opponentId = PLAYER_INDEX;

        human.maxHealth = human.health;

        data.humans[OPPONENT_INDEX] = human;
        data.humanDlls[OPPONENT_INDEX] = file.dll;

        data.humanGlobals[OPPONENT_INDEX] = new byte[file.globalsSize];
        callInitGlobals(file.initGlobalsFn, data.humanGlobals[OPPONENT_INDEX], OPPONENT_INDEX);

        // Give the opponent a random tool
        ArrayList<GrugFile> filesDefiningTool = getTypeFiles("tool");
        int toolIndex = rand.nextInt(filesDefiningTool.size());

        file = filesDefiningTool.get(toolIndex);

        callDefineFn(file.defineFn);
        Tool tool = new Tool(EntityDefinitions.tool);

        tool.onFns = file.onFns;

        tool.humanParentId = OPPONENT_INDEX;

        data.tools[OPPONENT_INDEX] = tool;
        data.toolDlls[OPPONENT_INDEX] = file.dll;

        data.toolGlobals[OPPONENT_INDEX] = new byte[file.globalsSize];
        callInitGlobals(file.initGlobalsFn, data.toolGlobals[OPPONENT_INDEX], OPPONENT_INDEX);

        data.state = State.FIGHTING;
    }

    private void printOpponentHumans(ArrayList<GrugFile> filesDefiningHuman) {
        for (int i = 0; i < filesDefiningHuman.size(); i++) {
            GrugFile file = filesDefiningHuman.get(i);

            callDefineFn(file.defineFn);

            Human human = EntityDefinitions.human;

            System.out.println((i + 1) + ". " + human.name + ", worth " + human.killGoldValue + " gold when killed");
        }

        System.out.println();
    }

    private void fight() {
        Human player = data.humans[PLAYER_INDEX];
        Human opponent = data.humans[OPPONENT_INDEX];

        byte[] playerToolGlobals = data.toolGlobals[PLAYER_INDEX];
        byte[] opponentToolGlobals = data.toolGlobals[OPPONENT_INDEX];

        Tool playerTool = data.tools[PLAYER_INDEX];
        Tool opponentTool = data.tools[OPPONENT_INDEX];

        System.out.println("You have " + player.health + " health");
        System.out.println("The opponent has " + opponent.health + " health");

        if (tool_hasOnUse(playerTool.onFns)) {
            System.out.println("You use your " + playerTool.name);
            tool_onUse(playerTool.onFns, playerToolGlobals);
            sleep(1);
        } else {
            System.out.println("You don't know what to do with your " + playerTool.name);
            sleep(1);
        }

        if (opponent.health <= 0) {
            System.out.println("The opponent died!");
            sleep(1);
            data.state = State.PICKING_PLAYER;
            data.gold += opponent.killGoldValue;
            player.health = player.maxHealth;
            return;
        }

        if (tool_hasOnUse(opponentTool.onFns)) {
            System.out.println("The opponent uses their " + opponentTool.name);
            tool_onUse(opponentTool.onFns, opponentToolGlobals);
            sleep(1);
        } else {
            System.out.println("The opponent doesn't know what to do with their " + opponentTool.name);
            sleep(1);
        }

        if (player.health <= 0) {
            System.out.println("You died!");
            sleep(1);
            data.state = State.PICKING_PLAYER;
            player.health = player.maxHealth;
        }
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

    private int gameFn_getHumanParent(int toolId) {
        if (toolId >= 2) {
            System.err.println(
                    "grug runtime error in " + onFnName()
                            + "(): the tool_id argument of get_human_parent() was " + toolId
                            + ", while the function only expects it to be up to 2, in " + onFnPath());
            return -1;
        }
        return data.tools[toolId].humanParentId;
    }

    private int gameFn_getOpponent(int humanId) {
        if (humanId >= 2) {
            System.err.println(
                    "grug runtime error in " + onFnName()
                            + "(): the human_id argument of get_opponent() was " + humanId
                            + ", while the function only expects it to be up to 2, in " + onFnPath());
            return -1;
        }
        return data.humans[humanId].opponentId;
    }

    private void gameFn_changeHumanHealth(int humanId, int addedHealth) {
        if (humanId >= 2) {
            System.err.println(
                    "grug runtime error in " + onFnName()
                            + "(): the human_id argument of change_human_health() was " + humanId
                            + ", while the function only expects it to be up to 2, in " + onFnPath());
            return;
        }
        if (addedHealth == -42) {
            System.err.println(
                    "grug runtime error in " + onFnName()
                            + "(): the added_health argument of change_human_health() was -42, while the function deems that number to be forbidden, in "
                            + onFnPath());
            return;
        }
        Human h = data.humans[humanId];
        h.health = Math.clamp(h.health + addedHealth, 0, h.maxHealth);
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

    public Tool(Tool other) {
        this.name = other.name;
        this.buyGoldValue = other.buyGoldValue;

        this.humanParentId = other.humanParentId;
        this.onFns = other.onFns;
    }
}

class EntityDefinitions {
    public static Human human = new Human();
    public static Tool tool = new Tool();
}
