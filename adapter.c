#include "headers/game_Game.h"

#include "grug/grug.h"

#include <assert.h>
#include <stdbool.h>
#include <stdio.h>

typedef char* string;
typedef int32_t i32;
typedef uint64_t id;

struct tool_on_fns {
	void (*use)(void *globals);
};

JNIEnv *global_env;
jobject global_obj;

jmethodID runtime_error_handler_id;

jobject human_definition_obj;
jfieldID human_definition_name_fid;
jfieldID human_definition_health_fid;
jfieldID human_definition_buy_gold_value_fid;
jfieldID human_definition_kill_gold_value_fid;

jobject tool_definition_obj;
jfieldID tool_definition_name_fid;
jfieldID tool_definition_buy_gold_value_fid;

jmethodID game_fn_get_human_parent_id;
jmethodID game_fn_get_opponent_id;
jmethodID game_fn_change_human_health_id;

void game_fn_define_human(string c_name, i32 c_health, i32 c_buy_gold_value, i32 c_kill_gold_value) {
    // TODO: Does this cause a memory leak?
    jstring name = (*global_env)->NewStringUTF(global_env, c_name);
    assert(name);
    (*global_env)->SetObjectField(global_env, human_definition_obj, human_definition_name_fid, name);

    (*global_env)->SetIntField(global_env, human_definition_obj, human_definition_health_fid, c_health);

    (*global_env)->SetIntField(global_env, human_definition_obj, human_definition_buy_gold_value_fid, c_buy_gold_value);

    (*global_env)->SetIntField(global_env, human_definition_obj, human_definition_kill_gold_value_fid, c_kill_gold_value);
}

void game_fn_define_tool(string c_name, i32 c_buy_gold_value) {
    // TODO: Does this cause a memory leak?
    jstring name = (*global_env)->NewStringUTF(global_env, c_name);
    assert(name);
    (*global_env)->SetObjectField(global_env, tool_definition_obj, tool_definition_name_fid, name);

    (*global_env)->SetIntField(global_env, tool_definition_obj, tool_definition_buy_gold_value_fid, c_buy_gold_value);
}

id game_fn_get_human_parent(id tool_id) {
    return (*global_env)->CallIntMethod(global_env, global_obj, game_fn_get_human_parent_id, tool_id);
}

id game_fn_get_opponent(id human_id) {
    return (*global_env)->CallIntMethod(global_env, global_obj, game_fn_get_opponent_id, human_id);
}

void game_fn_change_human_health(id human_id, i32 added_health) {
    (*global_env)->CallVoidMethod(global_env, global_obj, game_fn_change_human_health_id, human_id, added_health);
}

void runtime_error_handler(char *reason, enum grug_runtime_error_type type, char *on_fn_name, char *on_fn_path) {
    // TODO: These strings should probably be freed at the end of this function
    jstring java_reason = (*global_env)->NewStringUTF(global_env, reason);
    jint java_type = type;
    jstring java_on_fn_name = (*global_env)->NewStringUTF(global_env, on_fn_name);
    jstring java_on_fn_path = (*global_env)->NewStringUTF(global_env, on_fn_path);

    (*global_env)->CallVoidMethod(global_env, global_obj, runtime_error_handler_id, java_reason, java_type, java_on_fn_name, java_on_fn_path);
}

JNIEXPORT void JNICALL Java_game_Game_grugSetRuntimeErrorHandler(JNIEnv *env, jobject obj) {
    (void)env;
    (void)obj;

    grug_set_runtime_error_handler(runtime_error_handler);
}

JNIEXPORT jboolean JNICALL Java_game_Game_errorHasChanged(JNIEnv *env, jobject obj) {
    (void)env;
    (void)obj;

    return grug_error.has_changed;
}

JNIEXPORT jboolean JNICALL Java_game_Game_loadingErrorInGrugFile(JNIEnv *env, jobject obj) {
    (void)env;
    (void)obj;

    return grug_loading_error_in_grug_file;
}

JNIEXPORT jstring JNICALL Java_game_Game_errorMsg(JNIEnv *env, jobject obj) {
    (void)env;
    (void)obj;

    // TODO: This string should be freed at some point
    // TODO: An idea is having a global table containing the handful of possible error strings,
    // TODO: or having a single global string that gets replaced (freed) by every next error
    return (*global_env)->NewStringUTF(global_env, grug_error.msg);
}

JNIEXPORT jstring JNICALL Java_game_Game_errorPath(JNIEnv *env, jobject obj) {
    (void)env;
    (void)obj;

    // TODO: This string should be freed at some point
    // TODO: An idea is having a global table containing the handful of possible error strings,
    // TODO: or having a single global string that gets replaced (freed) by every next error
    return (*global_env)->NewStringUTF(global_env, grug_error.path);
}

JNIEXPORT jint JNICALL Java_game_Game_errorGrugCLineNumber(JNIEnv *env, jobject obj) {
    (void)env;
    (void)obj;

    return grug_error.grug_c_line_number;
}

JNIEXPORT jboolean JNICALL Java_game_Game_grugRegenerateModifiedMods(JNIEnv *env, jobject obj) {
    (void)env;
    (void)obj;

    return grug_regenerate_modified_mods();
}

JNIEXPORT jint JNICALL Java_game_Game_getGrugReloadsSize(JNIEnv *env, jobject obj) {
    (void)env;
    (void)obj;

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

    struct grug_file c_file = c_reload_data.file;

    jfieldID name_fid = (*env)->GetFieldID(env, file_class, "name", "Ljava/lang/String;");
    // TODO: Does this cause a memory leak?
    jstring name = (*env)->NewStringUTF(env, c_file.name);
    (*env)->SetObjectField(env, file_object, name_fid, name);

    jfieldID dll_fid = (*env)->GetFieldID(env, file_class, "dll", "J");
    (*env)->SetLongField(env, file_object, dll_fid, (jlong)c_file.dll);

    jfieldID define_fn_fid = (*env)->GetFieldID(env, file_class, "defineFn", "J");
    (*env)->SetLongField(env, file_object, define_fn_fid, (jlong)c_file.define_fn);

    jfieldID globals_size_fid = (*env)->GetFieldID(env, file_class, "globalsSize", "I");
    (*env)->SetIntField(env, file_object, globals_size_fid, (jint)c_file.globals_size);

    jfieldID init_globals_fn_fid = (*env)->GetFieldID(env, file_class, "initGlobalsFn", "J");
    (*env)->SetLongField(env, file_object, init_globals_fn_fid, (jlong)c_file.init_globals_fn);

    jfieldID define_type_fid = (*env)->GetFieldID(env, file_class, "defineType", "Ljava/lang/String;");
    // TODO: Does this cause a memory leak?
    jstring define_type = (*env)->NewStringUTF(env, c_file.define_type);
    (*env)->SetObjectField(env, file_object, define_type_fid, define_type);

    jfieldID on_fns_fid = (*env)->GetFieldID(env, file_class, "onFns", "J");
    (*env)->SetLongField(env, file_object, on_fns_fid, (jlong)c_file.on_fns);

    jfieldID resource_mtimes_fid = (*env)->GetFieldID(env, file_class, "resourceMtimes", "J");
    (*env)->SetLongField(env, file_object, resource_mtimes_fid, (jlong)c_file.resource_mtimes);
}

JNIEXPORT void JNICALL Java_game_Game_callInitGlobals(JNIEnv *env, jobject obj, jlong init_globals_fn, jbyteArray globals, jint entity_id) {
    (void)obj;

    jbyte *globals_bytes = (*env)->GetByteArrayElements(env, globals, NULL);

    ((grug_init_globals_fn_t)init_globals_fn)(globals_bytes, entity_id);

    (*env)->ReleaseByteArrayElements(env, globals, globals_bytes, 0);
}

JNIEXPORT void JNICALL Java_game_Game_init(JNIEnv *env, jobject obj) {
    global_env = env;
    global_obj = obj;

    jclass javaClass = (*env)->GetObjectClass(env, obj);
    assert(javaClass);

    runtime_error_handler_id = (*env)->GetMethodID(env, javaClass, "runtimeErrorHandler", "(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;)V");
    assert(runtime_error_handler_id);

    jclass entity_definitions_class = (*env)->FindClass(env, "game/EntityDefinitions");
    assert(entity_definitions_class);

    jfieldID human_definition_fid = (*env)->GetStaticFieldID(env, entity_definitions_class, "human", "Lgame/Human;");
    assert(human_definition_fid);

    human_definition_obj = (*env)->GetStaticObjectField(env, entity_definitions_class, human_definition_fid);
    assert(human_definition_obj);

    human_definition_obj = (*env)->NewGlobalRef(env, human_definition_obj);

    jclass human_definition_class = (*env)->GetObjectClass(env, human_definition_obj);
    assert(human_definition_class);

    human_definition_name_fid = (*env)->GetFieldID(env, human_definition_class, "name", "Ljava/lang/String;");
    assert(human_definition_name_fid);

    human_definition_health_fid = (*env)->GetFieldID(env, human_definition_class, "health", "I");
    assert(human_definition_health_fid);

    human_definition_buy_gold_value_fid = (*env)->GetFieldID(env, human_definition_class, "buyGoldValue", "I");
    assert(human_definition_buy_gold_value_fid);

    human_definition_kill_gold_value_fid = (*env)->GetFieldID(env, human_definition_class, "killGoldValue", "I");
    assert(human_definition_kill_gold_value_fid);

    jfieldID tool_definition_fid = (*env)->GetStaticFieldID(env, entity_definitions_class, "tool", "Lgame/Tool;");
    assert(tool_definition_fid);

    tool_definition_obj = (*env)->GetStaticObjectField(env, entity_definitions_class, tool_definition_fid);
    assert(tool_definition_obj);

    tool_definition_obj = (*env)->NewGlobalRef(env, tool_definition_obj);

    jclass tool_definition_class = (*env)->GetObjectClass(env, tool_definition_obj);
    assert(tool_definition_class);

    tool_definition_name_fid = (*env)->GetFieldID(env, tool_definition_class, "name", "Ljava/lang/String;");
    assert(tool_definition_name_fid);

    tool_definition_buy_gold_value_fid = (*env)->GetFieldID(env, tool_definition_class, "buyGoldValue", "I");
    assert(tool_definition_buy_gold_value_fid);

    game_fn_get_human_parent_id = (*env)->GetMethodID(env, javaClass, "gameFn_getHumanParent", "(I)I");
    assert(game_fn_get_human_parent_id);

    game_fn_get_opponent_id = (*env)->GetMethodID(env, javaClass, "gameFn_getOpponent", "(I)I");
    assert(game_fn_get_opponent_id);

    game_fn_change_human_health_id = (*env)->GetMethodID(env, javaClass, "gameFn_changeHumanHealth", "(II)V");
    assert(game_fn_change_human_health_id);
}

JNIEXPORT void JNICALL Java_game_Game_fillRootGrugDir(JNIEnv *env, jobject obj, jobject dir_object) {
    (void)obj;

    jclass dir_class = (*env)->GetObjectClass(env, dir_object);

    jfieldID name_fid = (*env)->GetFieldID(env, dir_class, "name", "Ljava/lang/String;");
    // TODO: Does this cause a memory leak?
    jstring name = (*env)->NewStringUTF(env, grug_mods.name);
    (*env)->SetObjectField(env, dir_object, name_fid, name);

    jfieldID dirs_size_fid = (*env)->GetFieldID(env, dir_class, "dirsSize", "I");
    (*env)->SetIntField(env, dir_object, dirs_size_fid, (jint)grug_mods.dirs_size);

    jfieldID files_size_fid = (*env)->GetFieldID(env, dir_class, "filesSize", "I");
    (*env)->SetIntField(env, dir_object, files_size_fid, (jint)grug_mods.files_size);

    jfieldID address_fid = (*env)->GetFieldID(env, dir_class, "address", "J");
    (*env)->SetLongField(env, dir_object, address_fid, (jlong)&grug_mods);
}

JNIEXPORT void JNICALL Java_game_Game_fillGrugDir(JNIEnv *env, jobject obj, jobject dir_object, jlong parent_dir_address, jint dir_index) {
    (void)obj;

    jclass dir_class = (*env)->GetObjectClass(env, dir_object);

    struct grug_mod_dir *parent_dir = (struct grug_mod_dir *)parent_dir_address;

    struct grug_mod_dir dir = parent_dir->dirs[dir_index];

    jfieldID name_fid = (*env)->GetFieldID(env, dir_class, "name", "Ljava/lang/String;");
    // TODO: Does this cause a memory leak?
    jstring name = (*env)->NewStringUTF(env, dir.name);
    (*env)->SetObjectField(env, dir_object, name_fid, name);

    jfieldID dirs_size_fid = (*env)->GetFieldID(env, dir_class, "dirsSize", "I");
    (*env)->SetIntField(env, dir_object, dirs_size_fid, (jint)dir.dirs_size);

    jfieldID files_size_fid = (*env)->GetFieldID(env, dir_class, "filesSize", "I");
    (*env)->SetIntField(env, dir_object, files_size_fid, (jint)dir.files_size);

    jfieldID address_fid = (*env)->GetFieldID(env, dir_class, "address", "J");
    (*env)->SetLongField(env, dir_object, address_fid, (jlong)&parent_dir->dirs[dir_index]);
}

JNIEXPORT void JNICALL Java_game_Game_fillGrugFile(JNIEnv *env, jobject obj, jobject file_object, jlong parent_dir_address, jint file_index) {
    (void)obj;

    jclass file_class = (*env)->GetObjectClass(env, file_object);

    struct grug_mod_dir *parent_dir = (struct grug_mod_dir *)parent_dir_address;

    struct grug_file file = parent_dir->files[file_index];

    jfieldID name_fid = (*env)->GetFieldID(env, file_class, "name", "Ljava/lang/String;");
    // TODO: Does this cause a memory leak?
    jstring name = (*env)->NewStringUTF(env, file.name);
    (*env)->SetObjectField(env, file_object, name_fid, name);

    jfieldID dll_fid = (*env)->GetFieldID(env, file_class, "dll", "J");
    (*env)->SetLongField(env, file_object, dll_fid, (jlong)file.dll);

    jfieldID define_fn_fid = (*env)->GetFieldID(env, file_class, "defineFn", "J");
    (*env)->SetLongField(env, file_object, define_fn_fid, (jlong)file.define_fn);

    jfieldID globals_size_fid = (*env)->GetFieldID(env, file_class, "globalsSize", "I");
    (*env)->SetIntField(env, file_object, globals_size_fid, (jint)file.globals_size);

    jfieldID init_globals_fn_fid = (*env)->GetFieldID(env, file_class, "initGlobalsFn", "J");
    (*env)->SetLongField(env, file_object, init_globals_fn_fid, (jlong)file.init_globals_fn);

    jfieldID define_type_fid = (*env)->GetFieldID(env, file_class, "defineType", "Ljava/lang/String;");
    // TODO: Does this cause a memory leak?
    jstring define_type = (*env)->NewStringUTF(env, file.define_type);
    (*env)->SetObjectField(env, file_object, define_type_fid, define_type);

    jfieldID on_fns_fid = (*env)->GetFieldID(env, file_class, "onFns", "J");
    (*env)->SetLongField(env, file_object, on_fns_fid, (jlong)file.on_fns);

    jfieldID resource_mtimes_fid = (*env)->GetFieldID(env, file_class, "resourceMtimes", "J");
    (*env)->SetLongField(env, file_object, resource_mtimes_fid, (jlong)file.resource_mtimes);
}

JNIEXPORT void JNICALL Java_game_Game_callDefineFn(JNIEnv *env, jobject obj, jlong define_fn) {
    (void)env;
    (void)obj;

    ((grug_define_fn_t)define_fn)();
}

JNIEXPORT jboolean JNICALL Java_game_Game_tool_1hasOnUse(JNIEnv *env, jobject obj, jlong on_fns) {
    (void)env;
    (void)obj;

    return ((struct tool_on_fns *)on_fns)->use != NULL;
}

JNIEXPORT void JNICALL Java_game_Game_tool_1onUse(JNIEnv *env, jobject obj, jlong on_fns, jbyteArray globals) {
    (void)obj;

    jbyte *globals_bytes = (*env)->GetByteArrayElements(env, globals, NULL);

    ((struct tool_on_fns *)on_fns)->use(globals_bytes);

    (*env)->ReleaseByteArrayElements(env, globals, globals_bytes, 0);
}
