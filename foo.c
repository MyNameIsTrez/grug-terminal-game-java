#include "Foo.h"

void bar(void);

JNIEXPORT void JNICALL Java_Foo_foo(JNIEnv *, jobject) {
    bar();
}
