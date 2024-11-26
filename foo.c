#include "Game.h"

void bar(void);

JNIEXPORT void JNICALL Java_Game_foo(JNIEnv *, jobject) {
    bar();
}
