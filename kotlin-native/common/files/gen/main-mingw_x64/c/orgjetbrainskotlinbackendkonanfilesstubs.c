#include <stdint.h>
#include <string.h>
#include <jni.h>
#include <Files.h>
#define __DATE__ "__DATE__"
#define __TIME__ "__TIME__"
#define __TIMESTAMP__ "__TIMESTAMP__"
#define __FILE__ "__FILE__"
#define __FILE_NAME__ "__FILE_NAME__"
#define __BASE_FILE__ "__BASE_FILE__"
#define __LINE__ "__LINE__"

// NOTE THIS FILE IS AUTO-GENERATED

JNIEXPORT jbyte JNICALL Java_org_jetbrains_kotlin_backend_konan_files_files_kniBridge0 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jbyte p2) {
    return (jbyte)renameAtomic((char*)p0, (char*)p1, (p2) ? 1 : 0);
}
