package game;

class GameFunctions {
    public static void set_human_name(String name) {
        if (Game.setHumanNameCalled) {
            Game.gameFunctionErrorHappened("set_human_name", "called twice by on_spawn()");
            return;
        }
        Game.setHumanNameCalled = true;

        OnSpawnData.human.name = name;
    }

    public static void set_human_health(int health) {
        if (Game.setHumanHealthCalled) {
            Game.gameFunctionErrorHappened("set_human_health", "called twice by on_spawn()");
            return;
        }
        Game.setHumanHealthCalled = true;

        OnSpawnData.human.health = health;
    }

    public static void set_human_buy_gold_value(int buyGoldValue) {
        if (Game.setHumanBuyGoldValueCalled) {
            Game.gameFunctionErrorHappened("set_human_buy_gold_value", "called twice by on_spawn()");
            return;
        }
        Game.setHumanBuyGoldValueCalled = true;

        OnSpawnData.human.buyGoldValue = buyGoldValue;
    }

    public static void set_human_kill_gold_value(int killGoldValue) {
        if (Game.setHumanKillGoldValueCalled) {
            Game.gameFunctionErrorHappened("set_human_kill_gold_value", "called twice by on_spawn()");
            return;
        }
        Game.setHumanKillGoldValueCalled = true;

        OnSpawnData.human.killGoldValue = killGoldValue;
    }

    public static void set_tool_name(String name) {
        if (Game.setToolNameCalled) {
            Game.gameFunctionErrorHappened("set_tool_name", "called twice by on_spawn()");
            return;
        }
        Game.setToolNameCalled = true;

        OnSpawnData.tool.name = name;
    }

    public static void set_tool_buy_gold_value(int buyGoldValue) {
        if (Game.setToolBuyGoldValueCalled) {
            Game.gameFunctionErrorHappened("set_tool_buy_gold_value", "called twice by on_spawn()");
            return;
        }
        Game.setToolBuyGoldValueCalled = true;

        OnSpawnData.tool.buyGoldValue = buyGoldValue;
    }

    public static long get_human_parent(long toolId) {
        if (toolId >= 2) {
            Game.gameFunctionErrorHappened(
                    "get_human_parent", "the tool_id argument was " + toolId
                            + ", while the function only expects it to be up to 2");
            return -1;
        }
        return Game.data.tools[(int)toolId].humanParentId;
    }

    public static long get_opponent(long humanId) {
        if (humanId >= 2) {
            Game.gameFunctionErrorHappened(
                    "get_opponent", "the human_id argument was " + humanId
                            + ", while the function only expects it to be up to 2");
            return -1;
        }
        return Game.data.humans[(int)humanId].opponentId;
    }

    public static void change_human_health(long humanId, int addedHealth) {
        if (humanId >= 2) {
            Game.gameFunctionErrorHappened(
                    "change_human_health", "the human_id argument was " + humanId
                            + ", while the function only expects it to be up to 2");
            return;
        }
        if (addedHealth == -42) {
            Game.gameFunctionErrorHappened(
                    "change_human_health", "the added_health argument was -42, while the function deems that number to be forbidden");
            return;
        }
        GrugHuman h = Game.data.humans[(int)humanId];
        h.health = h.health + addedHealth;
        // Clamp health
        h.health = Math.max(0, Math.min(h.maxHealth, h.health));
    }

    public static void print_string(String msg) {
        System.out.println(msg);
    }
}
