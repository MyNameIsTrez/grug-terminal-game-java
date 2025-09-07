package game;

import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

class Game {
    private native void loadGlobalLibraries();

    private native boolean errorHasChanged();

    private native boolean loadingErrorInGrugFile();

    private native String errorMsg();

    private native String errorPath();

    private native int errorGrugCLineNumber();

    private native boolean grugRegenerateModifiedMods();

    private native int getGrugReloadsSize();

    private native void fillReloadData(ReloadData reloadData, int i);

    private native void callInitGlobals(long initGlobalsFn, byte[] globals, long id);

    private native void initGrugAdapter();

    private native boolean grugInit(String modApiJsonPath, String modsDirPath, String dllDirPath, long onFnTimeLimitMs);

    private native void fillRootGrugDir(GrugDir root);

    private native void fillGrugDir(GrugDir dir, long parentDirAddress, int dirIndex);

    private native void fillGrugFile(GrugFile file, long parentDirAddress, int fileIndex);

    private native void toggleOnFnsMode();

    private native boolean areOnFnsInSafeMode();

    private native void gameFunctionErrorHappened(String message);

    private Scanner scanner = new Scanner(System.in);

    private native boolean Human_has_on_spawn(long onFns);
    private native void Human_on_spawn(long onFns, byte[] globals);

    private native boolean Human_has_on_despawn(long onFns);
    private native void Human_on_despawn(long onFns, byte[] globals);

    private native boolean Tool_has_on_spawn(long onFns);
    private native void Tool_on_spawn(long onFns, byte[] globals);

    private native boolean Tool_has_on_despawn(long onFns);
    private native void Tool_on_despawn(long onFns, byte[] globals);

    private native boolean Tool_has_on_use(long onFns);
    private native void Tool_on_use(long onFns, byte[] globals);

    public static Game game;

    private static final int PLAYER_INDEX = 0;
    private static final int OPPONENT_INDEX = 1;

    private ReloadData reloadData = new ReloadData();

    public static Data data = new Data();

    Random rand = new Random();

    public static boolean setHumanNameCalled;
    public static boolean setHumanHealthCalled;
    public static boolean setHumanBuyGoldValueCalled;
    public static boolean setHumanKillGoldValueCalled;

    public static boolean setToolNameCalled;
    public static boolean setToolBuyGoldValueCalled;

    public void runtimeErrorHandler(String reason, int type, String on_fn_name, String on_fn_path) {
        System.err.println("grug runtime error in " + on_fn_name + "(): " + reason + ", in " + on_fn_path);
    }

    public static void main(String[] args) {
        game = new Game();
        game.run();
    }

    public Game() {
        System.loadLibrary("global_library_loader");
        loadGlobalLibraries();

        System.loadLibrary("adapter");

        initGrugAdapter();

        if (grugInit("mod_api.json", "mods", "mod_dlls", 10)) {
            throw new RuntimeException("grugInit() error: " + errorMsg() + " (detected by grug.c:" + errorGrugCLineNumber() + ")");
        }
    }

    private void run() {
        while (true) {
            if (grugRegenerateModifiedMods()) {
                if (errorHasChanged()) {
                    if (loadingErrorInGrugFile()) {
                        System.err.println("grug loading error: " + errorMsg() + ", in " + errorPath()
                                + " (detected by grug.c:" + errorGrugCLineNumber() + ")");
                    } else {
                        System.err.println("grug loading error: " + errorMsg() + " (detected by grug.c:"
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

                    data.humans[i].onFns = file.onFns;
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
            case PICKING_PLAYER: pickPlayer();
            case PICKING_TOOLS: pickTools();
            case PICKING_OPPONENT: pickOpponent();
            case FIGHTING: fight();
        }
    }

    private void pickPlayer() {
        System.out.println("You have " + data.gold + " gold\n");

        ArrayList<GrugFile> humanFiles = getTypeFiles("Human");

        if (printPlayableHumans(humanFiles)) {
            return;
        }

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

        if (playerNumber > humanFiles.size()) {
            System.err.println("The maximum number you can enter is " + humanFiles.size());
            return;
        }

        if (data.humans[PLAYER_INDEX].onFns != 0) {
            callHumanOnDespawn(data.humans[PLAYER_INDEX].onFns, data.humanGlobals[PLAYER_INDEX]);
        }

        int playerIndex = playerNumber - 1;

        GrugFile file = humanFiles.get(playerIndex);

        data.humanGlobals[PLAYER_INDEX] = new byte[file.globalsSize];
        callInitGlobals(file.initGlobalsFn, data.humanGlobals[PLAYER_INDEX], PLAYER_INDEX);

        if (callHumanOnSpawn(file.entity, file.onFns, data.humanGlobals[PLAYER_INDEX])) {
            return;
        }

        GrugHuman human = new GrugHuman(OnSpawnData.human);

        if (human.buyGoldValue > data.gold) {
            System.err.println("You don't have enough gold to pick that human");
            return;
        }
        data.gold -= human.buyGoldValue;

        human.onFns = file.onFns;

        human.id = PLAYER_INDEX;
        human.opponentId = OPPONENT_INDEX;

        human.maxHealth = human.health;

        data.humans[PLAYER_INDEX] = human;
        data.humanDlls[PLAYER_INDEX] = file.dll;

        data.playerHasHuman = true;

        data.state = State.PICKING_TOOLS;
    }

    private boolean printPlayableHumans(ArrayList<GrugFile> humanFiles) {
        for (int i = 0; i < humanFiles.size(); i++) {
            GrugFile file = humanFiles.get(i);

            byte[] globals = new byte[file.globalsSize];
            callInitGlobals(file.initGlobalsFn, globals, 0);

            if (callHumanOnSpawn(file.entity, file.onFns, globals)) {
                return true;
            }

            GrugHuman human = OnSpawnData.human;

            System.out.println((i + 1) + ". " + human.name + ", costing " + human.buyGoldValue + " gold");

            callHumanOnDespawn(file.onFns, globals);
        }

        System.out.println();
        return false;
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

        ArrayList<GrugFile> toolFiles = getTypeFiles("Tool");

        if (printTools(toolFiles)) {
            return;
        }

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

        if (toolNumber > toolFiles.size()) {
            System.err.println("The maximum number you can enter is " + toolFiles.size());
            return;
        }

        if (data.tools[PLAYER_INDEX].onFns != 0) {
            callToolOnDespawn(data.tools[PLAYER_INDEX].onFns, data.toolGlobals[PLAYER_INDEX]);
        }

        int toolIndex = toolNumber - 1;

        GrugFile file = toolFiles.get(toolIndex);

        data.toolGlobals[PLAYER_INDEX] = new byte[file.globalsSize];
        callInitGlobals(file.initGlobalsFn, data.toolGlobals[PLAYER_INDEX], PLAYER_INDEX);

        if (callToolOnSpawn(file.entity, file.onFns, data.toolGlobals[PLAYER_INDEX])) {
            return;
        }

        GrugTool tool = new GrugTool(OnSpawnData.tool);

        if (tool.buyGoldValue > data.gold) {
            System.err.println("You don't have enough gold to buy that tool");
            return;
        }
        data.gold -= tool.buyGoldValue;

        tool.onFns = file.onFns;

        tool.humanParentId = PLAYER_INDEX;

        data.tools[PLAYER_INDEX] = tool;
        data.toolDlls[PLAYER_INDEX] = file.dll;

        data.playerHasTool = true;

        data.state = State.PICKING_OPPONENT;
    }

    private boolean printTools(ArrayList<GrugFile> toolFiles) {
        for (int i = 0; i < toolFiles.size(); i++) {
            GrugFile file = toolFiles.get(i);

            byte[] globals = new byte[file.globalsSize];
            callInitGlobals(file.initGlobalsFn, globals, 0);

            if (callToolOnSpawn(file.entity, file.onFns, globals)) {
                return true;
            }

            GrugTool tool = OnSpawnData.tool;

            System.out.println((i + 1) + ". " + tool.name + ", costing " + tool.buyGoldValue + " gold");

            callToolOnDespawn(file.onFns, globals);
        }

        System.out.println();
        return false;
    }

    private void pickOpponent() {
        System.out.println("You have " + data.gold + " gold\n");

        ArrayList<GrugFile> humanFiles = getTypeFiles("Human");

        if (printOpponentHumans(humanFiles)) {
            return;
        }

        System.out.println("Type the number next to the human you want to fight:");

        int opponentNumber = readSize();
        if (opponentNumber == -1) {
            return;
        }

        if (opponentNumber == 0) {
            System.err.println("The minimum number you can enter is 1");
            return;
        }

        if (opponentNumber > humanFiles.size()) {
            System.err.println("The maximum number you can enter is " + humanFiles.size());
            return;
        }

        int opponentIndex = opponentNumber - 1;

        GrugFile file = humanFiles.get(opponentIndex);

        data.humanGlobals[OPPONENT_INDEX] = new byte[file.globalsSize];
        callInitGlobals(file.initGlobalsFn, data.humanGlobals[OPPONENT_INDEX], OPPONENT_INDEX);

        if (callHumanOnSpawn(file.entity, file.onFns, data.humanGlobals[OPPONENT_INDEX])) {
            return;
        }

        GrugHuman human = new GrugHuman(OnSpawnData.human);

        human.onFns = file.onFns;

        human.id = OPPONENT_INDEX;
        human.opponentId = PLAYER_INDEX;

        human.maxHealth = human.health;

        data.humans[OPPONENT_INDEX] = human;
        data.humanDlls[OPPONENT_INDEX] = file.dll;

        // Give the opponent a random tool
        ArrayList<GrugFile> toolFiles = getTypeFiles("Tool");
        int toolIndex = rand.nextInt(toolFiles.size());

        file = toolFiles.get(toolIndex);

        data.toolGlobals[OPPONENT_INDEX] = new byte[file.globalsSize];
        callInitGlobals(file.initGlobalsFn, data.toolGlobals[OPPONENT_INDEX], OPPONENT_INDEX);

        if (callToolOnSpawn(file.entity, file.onFns, data.toolGlobals[OPPONENT_INDEX])) {
            return;
        }

        GrugTool tool = new GrugTool(OnSpawnData.tool);

        tool.onFns = file.onFns;

        tool.humanParentId = OPPONENT_INDEX;

        data.tools[OPPONENT_INDEX] = tool;
        data.toolDlls[OPPONENT_INDEX] = file.dll;

        data.state = State.FIGHTING;
    }

    private boolean printOpponentHumans(ArrayList<GrugFile> humanFiles) {
        for (int i = 0; i < humanFiles.size(); i++) {
            GrugFile file = humanFiles.get(i);

            byte[] globals = new byte[file.globalsSize];
            callInitGlobals(file.initGlobalsFn, globals, 0);

            if (callHumanOnSpawn(file.entity, file.onFns, globals)) {
                return true;
            }

            GrugHuman human = OnSpawnData.human;

            System.out.println((i + 1) + ". " + human.name + ", worth " + human.killGoldValue + " gold when killed");

            callHumanOnDespawn(file.onFns, globals);
        }

        System.out.println();
        return false;
    }

    private boolean callToolOnSpawn(String entity, long onFns, byte[] globals) {
        if (!Tool_has_on_spawn(onFns)) {
            System.err.println(entity + " is missing on_spawn()");
            return true;
        }

        setToolNameCalled = false;
        setToolBuyGoldValueCalled = false;

        Tool_on_spawn(onFns, globals);
    
        if (!setToolNameCalled) {
            System.err.println(entity + " its on_spawn() did not call set_tool_name()");
            return true;
        }
        if (!setToolBuyGoldValueCalled) {
            System.err.println(entity + " its on_spawn() did not call set_tool_buy_gold_value()");
            return true;
        }

        return false;
    }

    private boolean callHumanOnSpawn(String entity, long onFns, byte[] globals) {
        if (!Human_has_on_spawn(onFns)) {
            System.err.println(entity + " is missing on_spawn()");
            return true;
        }

        setHumanNameCalled = false;
        setHumanHealthCalled = false;
        setHumanBuyGoldValueCalled = false;
        setHumanKillGoldValueCalled = false;

        Human_on_spawn(onFns, globals);

        if (!setHumanNameCalled) {
            System.err.println(entity + " its on_spawn() did not call set_human_name()");
            return true;
        }
        if (!setHumanHealthCalled) {
            System.err.println(entity + " its on_spawn() did not call set_human_health()");
            return true;
        }
        if (!setHumanBuyGoldValueCalled) {
            System.err.println(entity + " its on_spawn() did not call set_human_buy_gold_value()");
            return true;
        }
        if (!setHumanKillGoldValueCalled) {
            System.err.println(entity + " its on_spawn() did not call set_human_kill_gold_value()");
            return true;
        }

        return false;
    }

    private void fight() {
        GrugHuman player = data.humans[PLAYER_INDEX];
        GrugHuman opponent = data.humans[OPPONENT_INDEX];

        GrugTool playerTool = data.tools[PLAYER_INDEX];
        GrugTool opponentTool = data.tools[OPPONENT_INDEX];

        byte[] opponentHumanGlobals = data.humanGlobals[OPPONENT_INDEX];

        byte[] playerToolGlobals = data.toolGlobals[PLAYER_INDEX];
        byte[] opponentToolGlobals = data.toolGlobals[OPPONENT_INDEX];

        System.out.println("You have " + player.health + " health");
        System.out.println("The opponent has " + opponent.health + " health");

        if (Tool_has_on_use(playerTool.onFns)) {
            System.out.println("You use your " + playerTool.name);
            Tool_on_use(playerTool.onFns, playerToolGlobals);
            sleep(1);
        } else {
            System.out.println("You don't know what to do with your " + playerTool.name);
            sleep(1);
        }

        if (opponent.health <= 0) {
            System.out.println("The opponent died!");
            callHumanOnDespawn(opponent.onFns, opponentHumanGlobals);
            sleep(1);
            data.state = State.PICKING_PLAYER;
            data.gold += opponent.killGoldValue;
            player.health = player.maxHealth;
            return;
        }

        if (Tool_has_on_use(opponentTool.onFns)) {
            System.out.println("The opponent uses their " + opponentTool.name);
            Tool_on_use(opponentTool.onFns, opponentToolGlobals);
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

    private void callToolOnDespawn(long onFns, byte[] globals) {
        if (Tool_has_on_despawn(onFns)) {
            Tool_on_despawn(onFns, globals);
        }
    }

    private void callHumanOnDespawn(long onFns, byte[] globals) {
        if (Human_has_on_despawn(onFns)) {
            Human_on_despawn(onFns, globals);
        }
    }

    private ArrayList<GrugFile> getTypeFiles(String entityType) {
        data.typeFiles.clear();

        GrugDir root = new GrugDir();
        fillRootGrugDir(root);

        getTypeFilesImpl(root, entityType);

        return data.typeFiles;
    }

    private void getTypeFilesImpl(GrugDir dir, String entityType) {
        for (int i = 0; i < dir.dirsSize; i++) {
            GrugDir subdir = new GrugDir();
            fillGrugDir(subdir, dir.address, i);

            getTypeFilesImpl(subdir, entityType);
        }

        for (int i = 0; i < dir.filesSize; i++) {
            GrugFile file = new GrugFile();
            fillGrugFile(file, dir.address, i);

            if (file.entityType.equals(entityType)) {
                data.typeFiles.add(file);
            }
        }
    }

    public static void gameFunctionErrorHappened(String gameFunctionName, String message) {
        game.gameFunctionErrorHappened(gameFunctionName + "(): " + message);
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
    public String entity;
    public String entityType;

    public long dll;

    public int globalsSize;
    public long initGlobalsFn;

    public long onFns;

    public GrugFile() {
    }
}

class Data {
    public GrugHuman[] humans = { new GrugHuman(), new GrugHuman() };
    public long[] humanDlls = new long[2];
    public byte[][] humanGlobals = new byte[2][];

    public GrugTool[] tools = { new GrugTool(), new GrugTool() };
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

class GrugHuman {
    public String name = "";
    public int health = -1;
    public int buyGoldValue = -1;
    public int killGoldValue = -1;

    // These are not initialized by mods
    public long id = -1;
    public long opponentId = -1;
    public int maxHealth = -1;
    public long onFns = 0;

    public GrugHuman() {
    }

    public GrugHuman(GrugHuman other) {
        this.name = other.name;
        this.health = other.health;
        this.buyGoldValue = other.buyGoldValue;
        this.killGoldValue = other.killGoldValue;

        this.id = other.id;
        this.opponentId = other.opponentId;
        this.maxHealth = other.maxHealth;
        this.onFns = other.onFns;
    }
}

class GrugTool {
    public String name = "";
    public int buyGoldValue = -1;

    // These are not initialized by mods
    public long humanParentId = 0;
    public long onFns = 0;

    public GrugTool() {
    }

    public GrugTool(GrugTool other) {
        this.name = other.name;
        this.buyGoldValue = other.buyGoldValue;

        this.humanParentId = other.humanParentId;
        this.onFns = other.onFns;
    }
}

class OnSpawnData {
    public static GrugHuman human = new GrugHuman();
    public static GrugTool tool = new GrugTool();
}
