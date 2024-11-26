#include "Game.h"

#include <dlfcn.h>
#include <stdlib.h>

JNIEXPORT void JNICALL Java_Game_load_1global_1libraries(JNIEnv *, jobject) {
    if (!dlopen("./libadapter.so", RTLD_NOW | RTLD_GLOBAL)) {
        fprintf(stderr, "dlopen: %s\n", dlerror());
        exit(EXIT_FAILURE);
    }
}
