#include "Game.h"

void game_fn_print_health(void);

JNIEXPORT void JNICALL Java_Game_on_1fire(JNIEnv *, jobject) {
    game_fn_print_health();
}
