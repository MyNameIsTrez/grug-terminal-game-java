#include "headers/game_Game.h"

#include "grug/grug.h"

#include <assert.h>
#include <stdbool.h>
#include <stdio.h>

typedef char* string;
typedef int32_t i32;
typedef uint64_t id;

JNIEnv *java_env;
jobject java_object;

jmethodID runtime_error_handler_id;

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
    jstring java_reason = (*java_env)->NewStringUTF(java_env, reason);
    jint java_type = type;
    jstring java_on_fn_name = (*java_env)->NewStringUTF(java_env, onFnName);
    jstring java_on_fn_path = (*java_env)->NewStringUTF(java_env, onFnPath);

    (*java_env)->CallVoidMethod(java_env, java_object, runtime_error_handler_id, java_reason, java_type, java_on_fn_name, java_on_fn_path);
}

JNIEXPORT void JNICALL Java_game_Game_grugSetRuntimeErrorHandler(JNIEnv *java_env_, jobject java_object_) {
    (void)java_env_;
    (void)java_object_;

    grug_set_runtime_error_handler(runtime_error_handler);
}

JNIEXPORT jboolean JNICALL Java_game_Game_errorHasChanged(JNIEnv *java_env_, jobject java_object_) {
    (void)java_env_;
    (void)java_object_;

    return grug_error.has_changed;
}

JNIEXPORT jboolean JNICALL Java_game_Game_loadingErrorInGrugFile(JNIEnv *java_env_, jobject java_object_) {
    (void)java_env_;
    (void)java_object_;

    return grug_loading_error_in_grug_file;
}

JNIEXPORT jstring JNICALL Java_game_Game_errorMsg(JNIEnv *java_env_, jobject java_object_) {
    (void)java_env_;
    (void)java_object_;

    // TODO: This string should be freed at some point
    // TODO: An idea is having a global table containing the handful of possible error strings,
    // TODO: or having a single global string that gets replaced (freed) by every next error
    return (*java_env)->NewStringUTF(java_env, grug_error.msg);
}

JNIEXPORT jstring JNICALL Java_game_Game_errorPath(JNIEnv *java_env_, jobject java_object_) {
    (void)java_env_;
    (void)java_object_;

    // TODO: This string should be freed at some point
    // TODO: An idea is having a global table containing the handful of possible error strings,
    // TODO: or having a single global string that gets replaced (freed) by every next error
    return (*java_env)->NewStringUTF(java_env, grug_error.path);
}

JNIEXPORT jint JNICALL Java_game_Game_errorGrugCLineNumber(JNIEnv *java_env_, jobject java_object_) {
    (void)java_env_;
    (void)java_object_;

    return grug_error.grug_c_line_number;
}

JNIEXPORT jboolean JNICALL Java_game_Game_grugRegenerateModifiedMods(JNIEnv *java_env_, jobject java_object_) {
    (void)java_env_;
    (void)java_object_;

    return grug_regenerate_modified_mods();
}

JNIEXPORT jint JNICALL Java_game_Game_getGrugReloadsSize(JNIEnv *java_env_, jobject java_object_) {
    (void)java_env_;
    (void)java_object_;

    return grug_reloads_size;
}

JNIEXPORT void JNICALL Java_game_Game_fillReloadData(JNIEnv *env, jobject obj, jobject reload_data_object, jint reload_index) {
    (void)obj;

    struct grug_modified c_reload_data = grug_reloads[reload_index];

    jclass reload_data_class = (*env)->GetObjectClass(env, reload_data_object);

    jfieldID path_fid = (*env)->GetFieldID(env, reload_data_class, "path", "Ljava/lang/String;");
    // TODO: Does this cause a memory leak?
    jstring path = (*env)->NewStringUTF(env, c_reload_data.path);
    (*env)->SetObjectField(env, reload_data_object, path_fid, path);

    jfieldID old_dll_fid = (*env)->GetFieldID(env, reload_data_class, "oldDll", "J");
    (*env)->SetLongField(env, reload_data_object, old_dll_fid, (jlong)c_reload_data.old_dll);

    jfieldID file_fid = (*env)->GetFieldID(env, reload_data_class, "file", "Lgame/GrugFile;");
    jobject file_object = (*env)->GetObjectField(env, reload_data_object, file_fid);

    jclass file_class = (*env)->GetObjectClass(env, file_object);

    struct grug_file *c_file = c_reload_data.file;

    jfieldID name_fid = (*env)->GetFieldID(env, file_class, "name", "Ljava/lang/String;");
    // TODO: Does this cause a memory leak?
    jstring name = (*env)->NewStringUTF(env, c_file->name);
    (*env)->SetObjectField(env, file_object, name_fid, name);

    jfieldID dll_fid = (*env)->GetFieldID(env, file_class, "dll", "J");
    (*env)->SetLongField(env, file_object, dll_fid, (jlong)c_file->dll);

    jfieldID define_fn_fid = (*env)->GetFieldID(env, file_class, "defineFn", "J");
    (*env)->SetLongField(env, file_object, define_fn_fid, (jlong)c_file->define_fn);

    jfieldID globals_size_fid = (*env)->GetFieldID(env, file_class, "globalsSize", "I");
    (*env)->SetIntField(env, file_object, globals_size_fid, (jlong)c_file->globals_size);

    jfieldID init_globals_fn_fid = (*env)->GetFieldID(env, file_class, "initGlobalsFn", "J");
    (*env)->SetLongField(env, file_object, init_globals_fn_fid, (jlong)c_file->init_globals_fn);

    jfieldID define_type_fid = (*env)->GetFieldID(env, file_class, "defineType", "Ljava/lang/String;");
    // TODO: Does this cause a memory leak?
    jstring define_type = (*env)->NewStringUTF(env, c_file->define_type);
    (*env)->SetObjectField(env, file_object, define_type_fid, define_type);

    jfieldID on_fns_fid = (*env)->GetFieldID(env, file_class, "onFns", "J");
    (*env)->SetLongField(env, file_object, on_fns_fid, (jlong)c_file->on_fns);

    jfieldID resource_mtimes_fid = (*env)->GetFieldID(env, file_class, "resourceMtimes", "J");
    (*env)->SetLongField(env, file_object, resource_mtimes_fid, (jlong)c_file->resource_mtimes);
}

JNIEXPORT void JNICALL Java_game_Game_initGlobals(JNIEnv *env, jobject obj, jlong init_globals_fn, jbyteArray globals, jint entity_id) {
    (void)obj;

    jbyte *globals_bytes = (*env)->GetByteArrayElements(env, globals, NULL);

    ((grug_init_globals_fn_t)init_globals_fn)(globals_bytes, entity_id);

    (*env)->ReleaseByteArrayElements(env, globals, globals_bytes, 0);
}

JNIEXPORT void JNICALL Java_game_Game_init(JNIEnv *java_env_, jobject java_object_) {
    java_env = java_env_;
    java_object = java_object_;

    jclass javaClass = (*java_env)->GetObjectClass(java_env, java_object);
    assert(javaClass != NULL);

    runtime_error_handler_id = (*java_env)->GetMethodID(java_env, javaClass, "runtimeErrorHandler", "(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;)V");
    assert(runtime_error_handler_id != NULL);
}

JNIEXPORT void JNICALL Java_game_Game_tool_1onUse(JNIEnv *java_env_, jobject java_object_, jlong on_fns) {
    (void)java_env_;
    (void)java_object_;

    printf("Java_game_Game_tool_1onUse\n");
    printf("on_fns: %p\n", (void *)on_fns);

    // TODO: Cast the on_fns parameter to the tool's on_fns struct, and call on_use() from it
}
