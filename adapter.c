#include "Game.h"

#include "grug/grug.h"

#include <assert.h>
#include <stdbool.h>
#include <stdio.h>

typedef char* string;
typedef int32_t i32;
typedef uint64_t id;

JNIEnv *javaEnv;
jobject javaObject;

jmethodID runtimeErrorHandler;

void game_fn_define_human(string name, i32 health, i32 buy_gold_value, i32 kill_gold_value) {
    // TODO: REMOVE!
    (void)name;
    (void)health;
    (void)buy_gold_value;
    (void)kill_gold_value;

    assert(false);
}

void game_fn_define_tool(string name, i32 buy_gold_value) {
    (void)name;
    (void)buy_gold_value;

    assert(false);
}

id game_fn_get_human_parent(id tool_id) {
    // TODO: REMOVE!
    (void)tool_id;

    assert(false);
}

id game_fn_get_opponent(id human_id) {
    // TODO: REMOVE!
    (void)human_id;

    assert(false);
}

void game_fn_change_human_health(id human_id, i32 added_health) {
    // TODO: REMOVE!
    (void)human_id;
    (void)added_health;

    assert(false);
}

void runtime_error_handler(char *reason, enum grug_runtime_error_type type, char *onFnName, char *onFnPath) {
    // TODO: These strings should probably be freed at the end of this function
    jstring javaReason = (*javaEnv)->NewStringUTF(javaEnv, reason);
    jint javaType = type;
    jstring javaOnFnName = (*javaEnv)->NewStringUTF(javaEnv, onFnName);
    jstring javaOnFnPath = (*javaEnv)->NewStringUTF(javaEnv, onFnPath);

    (*javaEnv)->CallVoidMethod(javaEnv, javaObject, runtimeErrorHandler, javaReason, javaType, javaOnFnName, javaOnFnPath);
}

JNIEXPORT void JNICALL Java_Game_grugSetRuntimeErrorHandler(JNIEnv *localJavaEnv, jobject localJavaObject) {
    (void)localJavaEnv;
    (void)localJavaObject;

    grug_set_runtime_error_handler(runtime_error_handler);
}

JNIEXPORT jboolean JNICALL Java_Game_errorHasChanged(JNIEnv *localJavaEnv, jobject localJavaObject) {
    (void)localJavaEnv;
    (void)localJavaObject;

    return grug_error.has_changed;
}

JNIEXPORT jboolean JNICALL Java_Game_loadingErrorInGrugFile(JNIEnv *localJavaEnv, jobject localJavaObject) {
    (void)localJavaEnv;
    (void)localJavaObject;

    return grug_loading_error_in_grug_file;
}

JNIEXPORT jstring JNICALL Java_Game_errorMsg(JNIEnv *localJavaEnv, jobject localJavaObject) {
    (void)localJavaEnv;
    (void)localJavaObject;

    // TODO: This string should be freed at some point
    // TODO: An idea is having a global table containing the handful of possible error strings,
    // TODO: or having a single global string that gets replaced (freed) by every next error
    return (*javaEnv)->NewStringUTF(javaEnv, grug_error.msg);
}

JNIEXPORT jstring JNICALL Java_Game_errorPath(JNIEnv *localJavaEnv, jobject localJavaObject) {
    (void)localJavaEnv;
    (void)localJavaObject;

    // TODO: This string should be freed at some point
    // TODO: An idea is having a global table containing the handful of possible error strings,
    // TODO: or having a single global string that gets replaced (freed) by every next error
    return (*javaEnv)->NewStringUTF(javaEnv, grug_error.path);
}

JNIEXPORT jint JNICALL Java_Game_errorGrugCLineNumber(JNIEnv *localJavaEnv, jobject localJavaObject) {
    (void)localJavaEnv;
    (void)localJavaObject;

    return grug_error.grug_c_line_number;
}

JNIEXPORT jboolean JNICALL Java_Game_grugRegenerateModifiedMods(JNIEnv *localJavaEnv, jobject localJavaObject) {
    (void)localJavaEnv;
    (void)localJavaObject;

    return grug_regenerate_modified_mods();
}

JNIEXPORT void JNICALL Java_Game_init(JNIEnv *localJavaEnv, jobject localJavaObject) {
    fprintf(stderr, "localJavaEnv in Java_Game_init(): %p\n", (void *)localJavaEnv);
    javaEnv = localJavaEnv;
    javaObject = localJavaObject;

    jclass javaClass = (*localJavaEnv)->GetObjectClass(localJavaEnv, localJavaObject);
    assert(javaClass != NULL);

    runtimeErrorHandler = (*javaEnv)->GetMethodID(javaEnv, javaClass, "runtimeErrorHandler", "(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;)V");
    assert(runtimeErrorHandler != NULL);
}

JNIEXPORT void JNICALL Java_Game_tool_1onUse(JNIEnv *localJavaEnv, jobject localJavaObject, jlong onFns) {
    (void)localJavaEnv;
    (void)localJavaObject;

    printf("Java_Game_tool_1onUse\n");
    printf("onFns: %p\n", (void *)onFns);

    // TODO: Cast the onFns parameter to the tool's on_fns struct, and call on_use() from it
}
