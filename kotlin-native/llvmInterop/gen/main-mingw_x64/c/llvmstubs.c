#include <stdint.h>
#include <string.h>
#include <jni.h>
#include <llvm-c/Core.h>
#include <llvm-c/Target.h>
#include <llvm-c/Analysis.h>
#include <llvm-c/BitWriter.h>
#include <llvm-c/BitReader.h>
#include <llvm-c/Transforms/PassBuilder.h>
#include <llvm-c/TargetMachine.h>
#include <llvm-c/Target.h>
#include <llvm-c/Linker.h>
#include <llvm-c/DebugInfo.h>
#include <DebugInfoC.h>
#include <CAPIExtensions.h>
#include <RemoveRedundantSafepoints.h>
#include <OpaquePointerAPI.h>
#define __DATE__ "__DATE__"
#define __TIME__ "__TIME__"
#define __TIMESTAMP__ "__TIMESTAMP__"
#define __FILE__ "__FILE__"
#define __FILE_NAME__ "__FILE_NAME__"
#define __BASE_FILE__ "__BASE_FILE__"
#define __LINE__ "__LINE__"

// NOTE THIS FILE IS AUTO-GENERATED

JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge0 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    LLVMInstallFatalErrorHandler((void*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge1 (JNIEnv* jniEnv, jclass jclss) {
    LLVMResetFatalErrorHandler();
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge2 (JNIEnv* jniEnv, jclass jclss) {
    LLVMEnablePrettyStackTrace();
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge3 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    LLVMInitializeCore((struct LLVMOpaquePassRegistry*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge4 (JNIEnv* jniEnv, jclass jclss) {
    LLVMShutdown();
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge5 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    LLVMGetVersion((unsigned int*)p0, (unsigned int*)p1, (unsigned int*)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge6 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMCreateMessage((char*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge7 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    LLVMDisposeMessage((char*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge8 (JNIEnv* jniEnv, jclass jclss) {
    return (jlong)LLVMContextCreate();
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge9 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    LLVMContextSetDiagnosticHandler((struct LLVMOpaqueContext*)p0, (void*)p1, (void*)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge10 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMContextGetDiagnosticHandler((struct LLVMOpaqueContext*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge11 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMContextGetDiagnosticContext((struct LLVMOpaqueContext*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge12 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    LLVMContextSetYieldCallback((struct LLVMOpaqueContext*)p0, (void*)p1, (void*)p2);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge13 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMContextShouldDiscardValueNames((struct LLVMOpaqueContext*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge14 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMContextSetDiscardValueNames((struct LLVMOpaqueContext*)p0, p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge15 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMContextSetOpaquePointers((struct LLVMOpaqueContext*)p0, p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge16 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    LLVMContextDispose((struct LLVMOpaqueContext*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge17 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetDiagInfoDescription((struct LLVMOpaqueDiagnosticInfo*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge18 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetDiagInfoSeverity((struct LLVMOpaqueDiagnosticInfo*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge19 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jint p2) {
    return (jint)LLVMGetMDKindIDInContext((struct LLVMOpaqueContext*)p0, (char*)p1, p2);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge20 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jint)LLVMGetEnumAttributeKindForName((char*)p0, p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge21 (JNIEnv* jniEnv, jclass jclss) {
    return (jint)LLVMGetLastEnumAttributeKind();
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge22 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2) {
    return (jlong)LLVMCreateEnumAttribute((struct LLVMOpaqueContext*)p0, p1, p2);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge23 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetEnumAttributeKind((struct LLVMOpaqueAttributeRef*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge24 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetEnumAttributeValue((struct LLVMOpaqueAttributeRef*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge25 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2) {
    return (jlong)LLVMCreateTypeAttribute((struct LLVMOpaqueContext*)p0, p1, (struct LLVMOpaqueType*)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge26 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetTypeAttributeValue((struct LLVMOpaqueAttributeRef*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge27 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jint p2, jlong p3, jint p4) {
    return (jlong)LLVMCreateStringAttribute((struct LLVMOpaqueContext*)p0, (char*)p1, p2, (char*)p3, p4);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge28 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMGetStringAttributeKind((struct LLVMOpaqueAttributeRef*)p0, (unsigned int*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge29 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMGetStringAttributeValue((struct LLVMOpaqueAttributeRef*)p0, (unsigned int*)p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge30 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMIsEnumAttribute((struct LLVMOpaqueAttributeRef*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge31 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMIsStringAttribute((struct LLVMOpaqueAttributeRef*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge32 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMIsTypeAttribute((struct LLVMOpaqueAttributeRef*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge33 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMGetTypeByName2((struct LLVMOpaqueContext*)p0, (char*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge34 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMModuleCreateWithNameInContext((char*)p0, (struct LLVMOpaqueContext*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge35 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMCloneModule((struct LLVMOpaqueModule*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge36 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    LLVMDisposeModule((struct LLVMOpaqueModule*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge37 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMGetModuleIdentifier((struct LLVMOpaqueModule*)p0, (unsigned long long*)p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge38 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    LLVMSetModuleIdentifier((struct LLVMOpaqueModule*)p0, (char*)p1, p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge39 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMGetSourceFileName((struct LLVMOpaqueModule*)p0, (unsigned long long*)p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge40 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    LLVMSetSourceFileName((struct LLVMOpaqueModule*)p0, (char*)p1, p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge41 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetDataLayoutStr((struct LLVMOpaqueModule*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge42 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetDataLayout((struct LLVMOpaqueModule*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge43 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMSetDataLayout((struct LLVMOpaqueModule*)p0, (char*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge44 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetTarget((struct LLVMOpaqueModule*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge45 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMSetTarget((struct LLVMOpaqueModule*)p0, (char*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge46 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMCopyModuleFlagsMetadata((struct LLVMOpaqueModule*)p0, (unsigned long long*)p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge47 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    LLVMDisposeModuleFlagsMetadata((struct LLVMOpaqueModuleFlagEntry*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge48 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    return (jint)LLVMModuleFlagEntriesGetFlagBehavior((struct LLVMOpaqueModuleFlagEntry*)p0, p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge49 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2) {
    return (jlong)LLVMModuleFlagEntriesGetKey((struct LLVMOpaqueModuleFlagEntry*)p0, p1, (unsigned long long*)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge50 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    return (jlong)LLVMModuleFlagEntriesGetMetadata((struct LLVMOpaqueModuleFlagEntry*)p0, p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge51 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)LLVMGetModuleFlag((struct LLVMOpaqueModule*)p0, (char*)p1, p2);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge52 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2, jlong p3, jlong p4) {
    LLVMAddModuleFlag((struct LLVMOpaqueModule*)p0, p1, (char*)p2, p3, (struct LLVMOpaqueMetadata*)p4);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge53 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    LLVMDumpModule((struct LLVMOpaqueModule*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge54 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jint)LLVMPrintModuleToFile((struct LLVMOpaqueModule*)p0, (char*)p1, (char**)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge55 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMPrintModuleToString((struct LLVMOpaqueModule*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge56 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMGetModuleInlineAsm((struct LLVMOpaqueModule*)p0, (unsigned long long*)p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge57 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    LLVMSetModuleInlineAsm2((struct LLVMOpaqueModule*)p0, (char*)p1, p2);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge58 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    LLVMAppendModuleInlineAsm((struct LLVMOpaqueModule*)p0, (char*)p1, p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge59 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jlong p4, jint p5, jint p6, jint p7, jint p8) {
    return (jlong)LLVMGetInlineAsm((struct LLVMOpaqueType*)p0, (char*)p1, p2, (char*)p3, p4, p5, p6, p7, p8);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge60 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetModuleContext((struct LLVMOpaqueModule*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge61 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMGetTypeByName((struct LLVMOpaqueModule*)p0, (char*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge62 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetFirstNamedMetadata((struct LLVMOpaqueModule*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge63 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetLastNamedMetadata((struct LLVMOpaqueModule*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge64 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetNextNamedMetadata((struct LLVMOpaqueNamedMDNode*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge65 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetPreviousNamedMetadata((struct LLVMOpaqueNamedMDNode*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge66 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)LLVMGetNamedMetadata((struct LLVMOpaqueModule*)p0, (char*)p1, p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge67 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)LLVMGetOrInsertNamedMetadata((struct LLVMOpaqueModule*)p0, (char*)p1, p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge68 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMGetNamedMetadataName((struct LLVMOpaqueNamedMDNode*)p0, (unsigned long long*)p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge69 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jint)LLVMGetNamedMetadataNumOperands((struct LLVMOpaqueModule*)p0, (char*)p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge70 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    LLVMGetNamedMetadataOperands((struct LLVMOpaqueModule*)p0, (char*)p1, (struct LLVMOpaqueValue**)p2);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge71 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    LLVMAddNamedMetadataOperand((struct LLVMOpaqueModule*)p0, (char*)p1, (struct LLVMOpaqueValue*)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge72 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMGetDebugLocDirectory((struct LLVMOpaqueValue*)p0, (unsigned int*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge73 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMGetDebugLocFilename((struct LLVMOpaqueValue*)p0, (unsigned int*)p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge74 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetDebugLocLine((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge75 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetDebugLocColumn((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge76 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)LLVMAddFunction((struct LLVMOpaqueModule*)p0, (char*)p1, (struct LLVMOpaqueType*)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge77 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMGetNamedFunction((struct LLVMOpaqueModule*)p0, (char*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge78 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetFirstFunction((struct LLVMOpaqueModule*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge79 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetLastFunction((struct LLVMOpaqueModule*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge80 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetNextFunction((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge81 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetPreviousFunction((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge82 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMSetModuleInlineAsm((struct LLVMOpaqueModule*)p0, (char*)p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge83 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetTypeKind((struct LLVMOpaqueType*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge84 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMTypeIsSized((struct LLVMOpaqueType*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge85 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetTypeContext((struct LLVMOpaqueType*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge86 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMPrintTypeToString((struct LLVMOpaqueType*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge87 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMInt1TypeInContext((struct LLVMOpaqueContext*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge88 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMInt8TypeInContext((struct LLVMOpaqueContext*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge89 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMInt16TypeInContext((struct LLVMOpaqueContext*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge90 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMInt32TypeInContext((struct LLVMOpaqueContext*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge91 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMInt64TypeInContext((struct LLVMOpaqueContext*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge92 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMInt128TypeInContext((struct LLVMOpaqueContext*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge93 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    return (jlong)LLVMIntTypeInContext((struct LLVMOpaqueContext*)p0, p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge94 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetIntTypeWidth((struct LLVMOpaqueType*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge95 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMHalfTypeInContext((struct LLVMOpaqueContext*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge96 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMBFloatTypeInContext((struct LLVMOpaqueContext*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge97 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMFloatTypeInContext((struct LLVMOpaqueContext*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge98 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMDoubleTypeInContext((struct LLVMOpaqueContext*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge99 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMX86FP80TypeInContext((struct LLVMOpaqueContext*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge100 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMFP128TypeInContext((struct LLVMOpaqueContext*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge101 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMPPCFP128TypeInContext((struct LLVMOpaqueContext*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge102 (JNIEnv* jniEnv, jclass jclss) {
    return (jlong)LLVMBFloatType();
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge103 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jint p2, jint p3) {
    return (jlong)LLVMFunctionType((struct LLVMOpaqueType*)p0, (struct LLVMOpaqueType**)p1, p2, p3);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge104 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMIsFunctionVarArg((struct LLVMOpaqueType*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge105 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetReturnType((struct LLVMOpaqueType*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge106 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMCountParamTypes((struct LLVMOpaqueType*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge107 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMGetParamTypes((struct LLVMOpaqueType*)p0, (struct LLVMOpaqueType**)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge108 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jint p2, jint p3) {
    return (jlong)LLVMStructTypeInContext((struct LLVMOpaqueContext*)p0, (struct LLVMOpaqueType**)p1, p2, p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge109 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMStructCreateNamed((struct LLVMOpaqueContext*)p0, (char*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge110 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetStructName((struct LLVMOpaqueType*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge111 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jint p2, jint p3) {
    LLVMStructSetBody((struct LLVMOpaqueType*)p0, (struct LLVMOpaqueType**)p1, p2, p3);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge112 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMCountStructElementTypes((struct LLVMOpaqueType*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge113 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMGetStructElementTypes((struct LLVMOpaqueType*)p0, (struct LLVMOpaqueType**)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge114 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    return (jlong)LLVMStructGetTypeAtIndex((struct LLVMOpaqueType*)p0, p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge115 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMIsPackedStruct((struct LLVMOpaqueType*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge116 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMIsOpaqueStruct((struct LLVMOpaqueType*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge117 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMIsLiteralStruct((struct LLVMOpaqueType*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge118 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetElementType((struct LLVMOpaqueType*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge119 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMGetSubtypes((struct LLVMOpaqueType*)p0, (struct LLVMOpaqueType**)p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge120 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetNumContainedTypes((struct LLVMOpaqueType*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge121 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    return (jlong)LLVMArrayType((struct LLVMOpaqueType*)p0, p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge122 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetArrayLength((struct LLVMOpaqueType*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge123 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    return (jlong)LLVMPointerType((struct LLVMOpaqueType*)p0, p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge124 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMPointerTypeIsOpaque((struct LLVMOpaqueType*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge125 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    return (jlong)LLVMPointerTypeInContext((struct LLVMOpaqueContext*)p0, p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge126 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetPointerAddressSpace((struct LLVMOpaqueType*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge127 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    return (jlong)LLVMVectorType((struct LLVMOpaqueType*)p0, p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge128 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    return (jlong)LLVMScalableVectorType((struct LLVMOpaqueType*)p0, p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge129 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetVectorSize((struct LLVMOpaqueType*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge130 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMVoidTypeInContext((struct LLVMOpaqueContext*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge131 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMLabelTypeInContext((struct LLVMOpaqueContext*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge132 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMX86MMXTypeInContext((struct LLVMOpaqueContext*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge133 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMX86AMXTypeInContext((struct LLVMOpaqueContext*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge134 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMTokenTypeInContext((struct LLVMOpaqueContext*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge135 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMMetadataTypeInContext((struct LLVMOpaqueContext*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge136 (JNIEnv* jniEnv, jclass jclss) {
    return (jlong)LLVMX86AMXType();
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge137 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jint p3, jlong p4, jint p5) {
    return (jlong)LLVMTargetExtTypeInContext((struct LLVMOpaqueContext*)p0, (char*)p1, (struct LLVMOpaqueType**)p2, p3, (unsigned int*)p4, p5);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge138 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMTypeOf((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge139 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetValueKind((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge140 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMGetValueName2((struct LLVMOpaqueValue*)p0, (unsigned long long*)p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge141 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    LLVMSetValueName2((struct LLVMOpaqueValue*)p0, (char*)p1, p2);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge142 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    LLVMDumpValue((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge143 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMPrintValueToString((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge144 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMReplaceAllUsesWith((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueValue*)p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge145 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMIsConstant((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge146 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMIsUndef((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge147 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMIsPoison((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge148 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAArgument((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge149 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsABasicBlock((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge150 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAInlineAsm((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge151 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAUser((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge152 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAConstant((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge153 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsABlockAddress((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge154 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAConstantAggregateZero((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge155 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAConstantArray((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge156 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAConstantDataSequential((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge157 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAConstantDataArray((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge158 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAConstantDataVector((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge159 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAConstantExpr((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge160 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAConstantFP((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge161 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAConstantInt((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge162 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAConstantPointerNull((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge163 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAConstantStruct((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge164 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAConstantTokenNone((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge165 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAConstantVector((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge166 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAGlobalValue((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge167 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAGlobalAlias((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge168 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAGlobalObject((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge169 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAFunction((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge170 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAGlobalVariable((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge171 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAGlobalIFunc((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge172 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAUndefValue((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge173 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAPoisonValue((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge174 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAInstruction((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge175 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAUnaryOperator((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge176 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsABinaryOperator((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge177 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsACallInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge178 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAIntrinsicInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge179 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsADbgInfoIntrinsic((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge180 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsADbgVariableIntrinsic((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge181 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsADbgDeclareInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge182 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsADbgLabelInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge183 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAMemIntrinsic((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge184 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAMemCpyInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge185 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAMemMoveInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge186 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAMemSetInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge187 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsACmpInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge188 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAFCmpInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge189 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAICmpInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge190 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAExtractElementInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge191 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAGetElementPtrInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge192 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAInsertElementInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge193 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAInsertValueInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge194 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsALandingPadInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge195 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAPHINode((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge196 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsASelectInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge197 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAShuffleVectorInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge198 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAStoreInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge199 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsABranchInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge200 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAIndirectBrInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge201 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAInvokeInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge202 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAReturnInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge203 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsASwitchInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge204 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAUnreachableInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge205 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAResumeInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge206 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsACleanupReturnInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge207 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsACatchReturnInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge208 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsACatchSwitchInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge209 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsACallBrInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge210 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAFuncletPadInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge211 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsACatchPadInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge212 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsACleanupPadInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge213 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAUnaryInstruction((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge214 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAAllocaInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge215 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsACastInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge216 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAAddrSpaceCastInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge217 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsABitCastInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge218 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAFPExtInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge219 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAFPToSIInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge220 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAFPToUIInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge221 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAFPTruncInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge222 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAIntToPtrInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge223 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAPtrToIntInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge224 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsASExtInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge225 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsASIToFPInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge226 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsATruncInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge227 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAUIToFPInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge228 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAZExtInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge229 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAExtractValueInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge230 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsALoadInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge231 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAVAArgInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge232 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAFreezeInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge233 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAAtomicCmpXchgInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge234 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAAtomicRMWInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge235 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAFenceInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge236 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAMDNode((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge237 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsAMDString((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge238 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetValueName((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge239 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMSetValueName((struct LLVMOpaqueValue*)p0, (char*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge240 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetFirstUse((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge241 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetNextUse((struct LLVMOpaqueUse*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge242 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetUser((struct LLVMOpaqueUse*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge243 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetUsedValue((struct LLVMOpaqueUse*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge244 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    return (jlong)LLVMGetOperand((struct LLVMOpaqueValue*)p0, p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge245 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    return (jlong)LLVMGetOperandUse((struct LLVMOpaqueValue*)p0, p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge246 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2) {
    LLVMSetOperand((struct LLVMOpaqueValue*)p0, p1, (struct LLVMOpaqueValue*)p2);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge247 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetNumOperands((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge248 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMConstNull((struct LLVMOpaqueType*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge249 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMConstAllOnes((struct LLVMOpaqueType*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge250 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetUndef((struct LLVMOpaqueType*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge251 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetPoison((struct LLVMOpaqueType*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge252 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMIsNull((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge253 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMConstPointerNull((struct LLVMOpaqueType*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge254 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jint p2) {
    return (jlong)LLVMConstInt((struct LLVMOpaqueType*)p0, p1, p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge255 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2) {
    return (jlong)LLVMConstIntOfArbitraryPrecision((struct LLVMOpaqueType*)p0, p1, (unsigned long long*)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge256 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jbyte p2) {
    return (jlong)LLVMConstIntOfString((struct LLVMOpaqueType*)p0, (char*)p1, p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge257 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jint p2, jbyte p3) {
    return (jlong)LLVMConstIntOfStringAndSize((struct LLVMOpaqueType*)p0, (char*)p1, p2, p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge258 (JNIEnv* jniEnv, jclass jclss, jlong p0, jdouble p1) {
    return (jlong)LLVMConstReal((struct LLVMOpaqueType*)p0, p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge259 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMConstRealOfString((struct LLVMOpaqueType*)p0, (char*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge260 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jint p2) {
    return (jlong)LLVMConstRealOfStringAndSize((struct LLVMOpaqueType*)p0, (char*)p1, p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge261 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMConstIntGetZExtValue((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge262 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMConstIntGetSExtValue((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jdouble JNICALL Java_llvm_llvm_kniBridge263 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jdouble)LLVMConstRealGetDouble((struct LLVMOpaqueValue*)p0, (int*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge264 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jint p2, jint p3) {
    return (jlong)LLVMConstStringInContext((struct LLVMOpaqueContext*)p0, (char*)p1, p2, p3);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge265 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMIsConstantString((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge266 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMGetAsString((struct LLVMOpaqueValue*)p0, (unsigned long long*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge267 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jint p2, jint p3) {
    return (jlong)LLVMConstStructInContext((struct LLVMOpaqueContext*)p0, (struct LLVMOpaqueValue**)p1, p2, p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge268 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jint p2) {
    return (jlong)LLVMConstArray((struct LLVMOpaqueType*)p0, (struct LLVMOpaqueValue**)p1, p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge269 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jint p2) {
    return (jlong)LLVMConstNamedStruct((struct LLVMOpaqueType*)p0, (struct LLVMOpaqueValue**)p1, p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge270 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    return (jlong)LLVMGetAggregateElement((struct LLVMOpaqueValue*)p0, p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge271 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    return (jlong)LLVMGetElementAsConstant((struct LLVMOpaqueValue*)p0, p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge272 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    return (jlong)LLVMConstVector((struct LLVMOpaqueValue**)p0, p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge273 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetConstOpcode((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge274 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMAlignOf((struct LLVMOpaqueType*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge275 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMSizeOf((struct LLVMOpaqueType*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge276 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMConstNeg((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge277 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMConstNSWNeg((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge278 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMConstNUWNeg((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge279 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMConstNot((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge280 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMConstAdd((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueValue*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge281 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMConstNSWAdd((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueValue*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge282 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMConstNUWAdd((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueValue*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge283 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMConstSub((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueValue*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge284 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMConstNSWSub((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueValue*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge285 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMConstNUWSub((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueValue*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge286 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMConstMul((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueValue*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge287 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMConstNSWMul((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueValue*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge288 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMConstNUWMul((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueValue*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge289 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMConstAnd((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueValue*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge290 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMConstOr((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueValue*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge291 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMConstXor((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueValue*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge292 (JNIEnv* jniEnv, jclass jclss, jint p0, jlong p1, jlong p2) {
    return (jlong)LLVMConstICmp(p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue*)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge293 (JNIEnv* jniEnv, jclass jclss, jint p0, jlong p1, jlong p2) {
    return (jlong)LLVMConstFCmp(p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue*)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge294 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMConstShl((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueValue*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge295 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMConstLShr((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueValue*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge296 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMConstAShr((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueValue*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge297 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jint p3) {
    return (jlong)LLVMConstGEP2((struct LLVMOpaqueType*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue**)p2, p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge298 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jint p3) {
    return (jlong)LLVMConstInBoundsGEP2((struct LLVMOpaqueType*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue**)p2, p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge299 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMConstTrunc((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueType*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge300 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMConstSExt((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueType*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge301 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMConstZExt((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueType*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge302 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMConstFPTrunc((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueType*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge303 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMConstFPExt((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueType*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge304 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMConstUIToFP((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueType*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge305 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMConstSIToFP((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueType*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge306 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMConstFPToUI((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueType*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge307 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMConstFPToSI((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueType*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge308 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMConstPtrToInt((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueType*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge309 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMConstIntToPtr((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueType*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge310 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMConstBitCast((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueType*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge311 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMConstAddrSpaceCast((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueType*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge312 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMConstZExtOrBitCast((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueType*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge313 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMConstSExtOrBitCast((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueType*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge314 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMConstTruncOrBitCast((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueType*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge315 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMConstPointerCast((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueType*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge316 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jint p2) {
    return (jlong)LLVMConstIntCast((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueType*)p1, p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge317 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMConstFPCast((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueType*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge318 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)LLVMConstSelect((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue*)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge319 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMConstExtractElement((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueValue*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge320 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)LLVMConstInsertElement((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue*)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge321 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)LLVMConstShuffleVector((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue*)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge322 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMBlockAddress((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueBasicBlock*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge323 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jint p3, jint p4) {
    return (jlong)LLVMConstInlineAsm((struct LLVMOpaqueType*)p0, (char*)p1, (char*)p2, p3, p4);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge324 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetGlobalParent((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge325 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMIsDeclaration((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge326 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetLinkage((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge327 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMSetLinkage((struct LLVMOpaqueValue*)p0, p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge328 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetSection((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge329 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMSetSection((struct LLVMOpaqueValue*)p0, (char*)p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge330 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetVisibility((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge331 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMSetVisibility((struct LLVMOpaqueValue*)p0, p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge332 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetDLLStorageClass((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge333 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMSetDLLStorageClass((struct LLVMOpaqueValue*)p0, p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge334 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetUnnamedAddress((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge335 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMSetUnnamedAddress((struct LLVMOpaqueValue*)p0, p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge336 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGlobalGetValueType((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge337 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMHasUnnamedAddr((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge338 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMSetUnnamedAddr((struct LLVMOpaqueValue*)p0, p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge339 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetAlignment((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge340 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMSetAlignment((struct LLVMOpaqueValue*)p0, p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge341 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2) {
    LLVMGlobalSetMetadata((struct LLVMOpaqueValue*)p0, p1, (struct LLVMOpaqueMetadata*)p2);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge342 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMGlobalEraseMetadata((struct LLVMOpaqueValue*)p0, p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge343 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    LLVMGlobalClearMetadata((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge344 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMGlobalCopyAllMetadata((struct LLVMOpaqueValue*)p0, (unsigned long long*)p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge345 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    LLVMDisposeValueMetadataEntries((struct LLVMOpaqueValueMetadataEntry*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge346 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    return (jint)LLVMValueMetadataEntriesGetKind((struct LLVMOpaqueValueMetadataEntry*)p0, p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge347 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    return (jlong)LLVMValueMetadataEntriesGetMetadata((struct LLVMOpaqueValueMetadataEntry*)p0, p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge348 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)LLVMAddGlobal((struct LLVMOpaqueModule*)p0, (struct LLVMOpaqueType*)p1, (char*)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge349 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jint p3) {
    return (jlong)LLVMAddGlobalInAddressSpace((struct LLVMOpaqueModule*)p0, (struct LLVMOpaqueType*)p1, (char*)p2, p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge350 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMGetNamedGlobal((struct LLVMOpaqueModule*)p0, (char*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge351 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetFirstGlobal((struct LLVMOpaqueModule*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge352 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetLastGlobal((struct LLVMOpaqueModule*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge353 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetNextGlobal((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge354 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetPreviousGlobal((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge355 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    LLVMDeleteGlobal((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge356 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetInitializer((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge357 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMSetInitializer((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueValue*)p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge358 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMIsThreadLocal((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge359 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMSetThreadLocal((struct LLVMOpaqueValue*)p0, p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge360 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMIsGlobalConstant((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge361 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMSetGlobalConstant((struct LLVMOpaqueValue*)p0, p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge362 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetThreadLocalMode((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge363 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMSetThreadLocalMode((struct LLVMOpaqueValue*)p0, p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge364 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMIsExternallyInitialized((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge365 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMSetExternallyInitialized((struct LLVMOpaqueValue*)p0, p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge366 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jint p2, jlong p3, jlong p4) {
    return (jlong)LLVMAddAlias2((struct LLVMOpaqueModule*)p0, (struct LLVMOpaqueType*)p1, p2, (struct LLVMOpaqueValue*)p3, (char*)p4);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge367 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)LLVMGetNamedGlobalAlias((struct LLVMOpaqueModule*)p0, (char*)p1, p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge368 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetFirstGlobalAlias((struct LLVMOpaqueModule*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge369 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetLastGlobalAlias((struct LLVMOpaqueModule*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge370 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetNextGlobalAlias((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge371 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetPreviousGlobalAlias((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge372 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMAliasGetAliasee((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge373 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMAliasSetAliasee((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueValue*)p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge374 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    LLVMDeleteFunction((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge375 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMHasPersonalityFn((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge376 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetPersonalityFn((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge377 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMSetPersonalityFn((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueValue*)p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge378 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jint)LLVMLookupIntrinsicID((char*)p0, p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge379 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetIntrinsicID((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge380 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2, jlong p3) {
    return (jlong)LLVMGetIntrinsicDeclaration((struct LLVMOpaqueModule*)p0, p1, (struct LLVMOpaqueType**)p2, p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge381 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2, jlong p3) {
    return (jlong)LLVMIntrinsicGetType((struct LLVMOpaqueContext*)p0, p1, (struct LLVMOpaqueType**)p2, p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge382 (JNIEnv* jniEnv, jclass jclss, jint p0, jlong p1) {
    return (jlong)LLVMIntrinsicGetName(p0, (unsigned long long*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge383 (JNIEnv* jniEnv, jclass jclss, jint p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMIntrinsicCopyOverloadedName(p0, (struct LLVMOpaqueType**)p1, p2, (unsigned long long*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge384 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2, jlong p3, jlong p4) {
    return (jlong)LLVMIntrinsicCopyOverloadedName2((struct LLVMOpaqueModule*)p0, p1, (struct LLVMOpaqueType**)p2, p3, (unsigned long long*)p4);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge385 (JNIEnv* jniEnv, jclass jclss, jint p0) {
    return (jint)LLVMIntrinsicIsOverloaded(p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge386 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetFunctionCallConv((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge387 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMSetFunctionCallConv((struct LLVMOpaqueValue*)p0, p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge388 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetGC((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge389 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMSetGC((struct LLVMOpaqueValue*)p0, (char*)p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge390 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2) {
    LLVMAddAttributeAtIndex((struct LLVMOpaqueValue*)p0, p1, (struct LLVMOpaqueAttributeRef*)p2);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge391 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    return (jint)LLVMGetAttributeCountAtIndex((struct LLVMOpaqueValue*)p0, p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge392 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2) {
    LLVMGetAttributesAtIndex((struct LLVMOpaqueValue*)p0, p1, (struct LLVMOpaqueAttributeRef**)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge393 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jint p2) {
    return (jlong)LLVMGetEnumAttributeAtIndex((struct LLVMOpaqueValue*)p0, p1, p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge394 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2, jint p3) {
    return (jlong)LLVMGetStringAttributeAtIndex((struct LLVMOpaqueValue*)p0, p1, (char*)p2, p3);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge395 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jint p2) {
    LLVMRemoveEnumAttributeAtIndex((struct LLVMOpaqueValue*)p0, p1, p2);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge396 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2, jint p3) {
    LLVMRemoveStringAttributeAtIndex((struct LLVMOpaqueValue*)p0, p1, (char*)p2, p3);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge397 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    LLVMAddTargetDependentFunctionAttr((struct LLVMOpaqueValue*)p0, (char*)p1, (char*)p2);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge398 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMCountParams((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge399 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMGetParams((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueValue**)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge400 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    return (jlong)LLVMGetParam((struct LLVMOpaqueValue*)p0, p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge401 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetParamParent((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge402 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetFirstParam((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge403 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetLastParam((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge404 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetNextParam((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge405 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetPreviousParam((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge406 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMSetParamAlignment((struct LLVMOpaqueValue*)p0, p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge407 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jint p4, jlong p5) {
    return (jlong)LLVMAddGlobalIFunc((struct LLVMOpaqueModule*)p0, (char*)p1, p2, (struct LLVMOpaqueType*)p3, p4, (struct LLVMOpaqueValue*)p5);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge408 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)LLVMGetNamedGlobalIFunc((struct LLVMOpaqueModule*)p0, (char*)p1, p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge409 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetFirstGlobalIFunc((struct LLVMOpaqueModule*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge410 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetLastGlobalIFunc((struct LLVMOpaqueModule*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge411 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetNextGlobalIFunc((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge412 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetPreviousGlobalIFunc((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge413 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetGlobalIFuncResolver((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge414 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMSetGlobalIFuncResolver((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueValue*)p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge415 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    LLVMEraseGlobalIFunc((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge416 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    LLVMRemoveGlobalIFunc((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge417 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)LLVMMDStringInContext2((struct LLVMOpaqueContext*)p0, (char*)p1, p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge418 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)LLVMMDNodeInContext2((struct LLVMOpaqueContext*)p0, (struct LLVMOpaqueMetadata**)p1, p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge419 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMMetadataAsValue((struct LLVMOpaqueContext*)p0, (struct LLVMOpaqueMetadata*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge420 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMValueAsMetadata((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge421 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMGetMDString((struct LLVMOpaqueValue*)p0, (unsigned int*)p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge422 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetMDNodeNumOperands((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge423 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMGetMDNodeOperands((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueValue**)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge424 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jint p2) {
    return (jlong)LLVMMDStringInContext((struct LLVMOpaqueContext*)p0, (char*)p1, p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge425 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jint p2) {
    return (jlong)LLVMMDNodeInContext((struct LLVMOpaqueContext*)p0, (struct LLVMOpaqueValue**)p1, p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge426 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMBasicBlockAsValue((struct LLVMOpaqueBasicBlock*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge427 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMValueIsBasicBlock((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge428 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMValueAsBasicBlock((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge429 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetBasicBlockName((struct LLVMOpaqueBasicBlock*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge430 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetBasicBlockParent((struct LLVMOpaqueBasicBlock*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge431 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetBasicBlockTerminator((struct LLVMOpaqueBasicBlock*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge432 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMCountBasicBlocks((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge433 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMGetBasicBlocks((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueBasicBlock**)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge434 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetFirstBasicBlock((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge435 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetLastBasicBlock((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge436 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetNextBasicBlock((struct LLVMOpaqueBasicBlock*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge437 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetPreviousBasicBlock((struct LLVMOpaqueBasicBlock*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge438 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetEntryBasicBlock((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge439 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMInsertExistingBasicBlockAfterInsertBlock((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueBasicBlock*)p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge440 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMAppendExistingBasicBlock((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueBasicBlock*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge441 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMCreateBasicBlockInContext((struct LLVMOpaqueContext*)p0, (char*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge442 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)LLVMAppendBasicBlockInContext((struct LLVMOpaqueContext*)p0, (struct LLVMOpaqueValue*)p1, (char*)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge443 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)LLVMInsertBasicBlockInContext((struct LLVMOpaqueContext*)p0, (struct LLVMOpaqueBasicBlock*)p1, (char*)p2);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge444 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    LLVMDeleteBasicBlock((struct LLVMOpaqueBasicBlock*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge445 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    LLVMRemoveBasicBlockFromParent((struct LLVMOpaqueBasicBlock*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge446 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMMoveBasicBlockBefore((struct LLVMOpaqueBasicBlock*)p0, (struct LLVMOpaqueBasicBlock*)p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge447 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMMoveBasicBlockAfter((struct LLVMOpaqueBasicBlock*)p0, (struct LLVMOpaqueBasicBlock*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge448 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetFirstInstruction((struct LLVMOpaqueBasicBlock*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge449 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetLastInstruction((struct LLVMOpaqueBasicBlock*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge450 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMHasMetadata((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge451 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    return (jlong)LLVMGetMetadata((struct LLVMOpaqueValue*)p0, p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge452 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2) {
    LLVMSetMetadata((struct LLVMOpaqueValue*)p0, p1, (struct LLVMOpaqueValue*)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge453 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMInstructionGetAllMetadataOtherThanDebugLoc((struct LLVMOpaqueValue*)p0, (unsigned long long*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge454 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetInstructionParent((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge455 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetNextInstruction((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge456 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetPreviousInstruction((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge457 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    LLVMInstructionRemoveFromParent((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge458 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    LLVMInstructionEraseFromParent((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge459 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    LLVMDeleteInstruction((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge460 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetInstructionOpcode((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge461 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetICmpPredicate((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge462 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetFCmpPredicate((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge463 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMInstructionClone((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge464 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMIsATerminatorInst((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge465 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetNumArgOperands((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge466 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMSetInstructionCallConv((struct LLVMOpaqueValue*)p0, p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge467 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetInstructionCallConv((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge468 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jint p2) {
    LLVMSetInstrParamAlignment((struct LLVMOpaqueValue*)p0, p1, p2);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge469 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2) {
    LLVMAddCallSiteAttribute((struct LLVMOpaqueValue*)p0, p1, (struct LLVMOpaqueAttributeRef*)p2);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge470 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    return (jint)LLVMGetCallSiteAttributeCount((struct LLVMOpaqueValue*)p0, p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge471 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2) {
    LLVMGetCallSiteAttributes((struct LLVMOpaqueValue*)p0, p1, (struct LLVMOpaqueAttributeRef**)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge472 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jint p2) {
    return (jlong)LLVMGetCallSiteEnumAttribute((struct LLVMOpaqueValue*)p0, p1, p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge473 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2, jint p3) {
    return (jlong)LLVMGetCallSiteStringAttribute((struct LLVMOpaqueValue*)p0, p1, (char*)p2, p3);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge474 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jint p2) {
    LLVMRemoveCallSiteEnumAttribute((struct LLVMOpaqueValue*)p0, p1, p2);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge475 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2, jint p3) {
    LLVMRemoveCallSiteStringAttribute((struct LLVMOpaqueValue*)p0, p1, (char*)p2, p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge476 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetCalledFunctionType((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge477 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetCalledValue((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge478 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMIsTailCall((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge479 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMSetTailCall((struct LLVMOpaqueValue*)p0, p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge480 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetNormalDest((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge481 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetUnwindDest((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge482 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMSetNormalDest((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueBasicBlock*)p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge483 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMSetUnwindDest((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueBasicBlock*)p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge484 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetNumSuccessors((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge485 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    return (jlong)LLVMGetSuccessor((struct LLVMOpaqueValue*)p0, p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge486 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2) {
    LLVMSetSuccessor((struct LLVMOpaqueValue*)p0, p1, (struct LLVMOpaqueBasicBlock*)p2);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge487 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMIsConditional((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge488 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetCondition((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge489 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMSetCondition((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueValue*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge490 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetSwitchDefaultDest((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge491 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetAllocatedType((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge492 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMIsInBounds((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge493 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMSetIsInBounds((struct LLVMOpaqueValue*)p0, p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge494 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetGEPSourceElementType((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge495 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jint p3) {
    LLVMAddIncoming((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueValue**)p1, (struct LLVMOpaqueBasicBlock**)p2, p3);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge496 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMCountIncoming((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge497 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    return (jlong)LLVMGetIncomingValue((struct LLVMOpaqueValue*)p0, p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge498 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    return (jlong)LLVMGetIncomingBlock((struct LLVMOpaqueValue*)p0, p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge499 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetNumIndices((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge500 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetIndices((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge501 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMCreateBuilderInContext((struct LLVMOpaqueContext*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge502 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    LLVMPositionBuilder((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueBasicBlock*)p1, (struct LLVMOpaqueValue*)p2);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge503 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMPositionBuilderBefore((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge504 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMPositionBuilderAtEnd((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueBasicBlock*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge505 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetInsertBlock((struct LLVMOpaqueBuilder*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge506 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    LLVMClearInsertionPosition((struct LLVMOpaqueBuilder*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge507 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMInsertIntoBuilder((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge508 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    LLVMInsertIntoBuilderWithName((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (char*)p2);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge509 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    LLVMDisposeBuilder((struct LLVMOpaqueBuilder*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge510 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetCurrentDebugLocation2((struct LLVMOpaqueBuilder*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge511 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMSetCurrentDebugLocation2((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueMetadata*)p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge512 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMSetInstDebugLocation((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge513 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMAddMetadataToInst((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge514 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMBuilderGetDefaultFPMathTag((struct LLVMOpaqueBuilder*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge515 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMBuilderSetDefaultFPMathTag((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueMetadata*)p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge516 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMSetCurrentDebugLocation((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge517 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetCurrentDebugLocation((struct LLVMOpaqueBuilder*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge518 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMBuildRetVoid((struct LLVMOpaqueBuilder*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge519 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMBuildRet((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge520 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jint p2) {
    return (jlong)LLVMBuildAggregateRet((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue**)p1, p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge521 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMBuildBr((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueBasicBlock*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge522 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildCondBr((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueBasicBlock*)p2, (struct LLVMOpaqueBasicBlock*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge523 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jint p3) {
    return (jlong)LLVMBuildSwitch((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueBasicBlock*)p2, p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge524 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jint p2) {
    return (jlong)LLVMBuildIndirectBr((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge525 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jint p4, jlong p5, jlong p6, jlong p7) {
    return (jlong)LLVMBuildInvoke2((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueType*)p1, (struct LLVMOpaqueValue*)p2, (struct LLVMOpaqueValue**)p3, p4, (struct LLVMOpaqueBasicBlock*)p5, (struct LLVMOpaqueBasicBlock*)p6, (char*)p7);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge526 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMBuildUnreachable((struct LLVMOpaqueBuilder*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge527 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMBuildResume((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge528 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jint p3, jlong p4) {
    return (jlong)LLVMBuildLandingPad((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueType*)p1, (struct LLVMOpaqueValue*)p2, p3, (char*)p4);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge529 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)LLVMBuildCleanupRet((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueBasicBlock*)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge530 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)LLVMBuildCatchRet((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueBasicBlock*)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge531 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jint p3, jlong p4) {
    return (jlong)LLVMBuildCatchPad((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue**)p2, p3, (char*)p4);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge532 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jint p3, jlong p4) {
    return (jlong)LLVMBuildCleanupPad((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue**)p2, p3, (char*)p4);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge533 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jint p3, jlong p4) {
    return (jlong)LLVMBuildCatchSwitch((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueBasicBlock*)p2, p3, (char*)p4);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge534 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    LLVMAddCase((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueBasicBlock*)p2);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge535 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMAddDestination((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueBasicBlock*)p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge536 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetNumClauses((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge537 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    return (jlong)LLVMGetClause((struct LLVMOpaqueValue*)p0, p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge538 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMAddClause((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueValue*)p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge539 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMIsCleanup((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge540 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMSetCleanup((struct LLVMOpaqueValue*)p0, p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge541 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMAddHandler((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueBasicBlock*)p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge542 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetNumHandlers((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge543 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMGetHandlers((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueBasicBlock**)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge544 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    return (jlong)LLVMGetArgOperand((struct LLVMOpaqueValue*)p0, p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge545 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2) {
    LLVMSetArgOperand((struct LLVMOpaqueValue*)p0, p1, (struct LLVMOpaqueValue*)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge546 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetParentCatchSwitch((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge547 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMSetParentCatchSwitch((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueValue*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge548 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildAdd((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge549 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildNSWAdd((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge550 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildNUWAdd((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge551 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildFAdd((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge552 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildSub((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge553 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildNSWSub((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge554 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildNUWSub((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge555 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildFSub((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge556 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildMul((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge557 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildNSWMul((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge558 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildNUWMul((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge559 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildFMul((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge560 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildUDiv((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge561 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildExactUDiv((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge562 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildSDiv((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge563 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildExactSDiv((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge564 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildFDiv((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge565 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildURem((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge566 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildSRem((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge567 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildFRem((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge568 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildShl((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge569 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildLShr((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge570 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildAShr((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge571 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildAnd((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge572 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildOr((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge573 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildXor((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge574 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2, jlong p3, jlong p4) {
    return (jlong)LLVMBuildBinOp((struct LLVMOpaqueBuilder*)p0, p1, (struct LLVMOpaqueValue*)p2, (struct LLVMOpaqueValue*)p3, (char*)p4);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge575 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)LLVMBuildNeg((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (char*)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge576 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)LLVMBuildNSWNeg((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (char*)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge577 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)LLVMBuildNUWNeg((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (char*)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge578 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)LLVMBuildFNeg((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (char*)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge579 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)LLVMBuildNot((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (char*)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge580 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)LLVMBuildMalloc((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueType*)p1, (char*)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge581 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildArrayMalloc((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueType*)p1, (struct LLVMOpaqueValue*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge582 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jint p4) {
    return (jlong)LLVMBuildMemSet((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue*)p2, (struct LLVMOpaqueValue*)p3, p4);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge583 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jint p2, jlong p3, jint p4, jlong p5) {
    return (jlong)LLVMBuildMemCpy((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, p2, (struct LLVMOpaqueValue*)p3, p4, (struct LLVMOpaqueValue*)p5);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge584 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jint p2, jlong p3, jint p4, jlong p5) {
    return (jlong)LLVMBuildMemMove((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, p2, (struct LLVMOpaqueValue*)p3, p4, (struct LLVMOpaqueValue*)p5);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge585 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)LLVMBuildAlloca((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueType*)p1, (char*)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge586 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildArrayAlloca((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueType*)p1, (struct LLVMOpaqueValue*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge587 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMBuildFree((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge588 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildLoad2((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueType*)p1, (struct LLVMOpaqueValue*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge589 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)LLVMBuildStore((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue*)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge590 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jint p4, jlong p5) {
    return (jlong)LLVMBuildGEP2((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueType*)p1, (struct LLVMOpaqueValue*)p2, (struct LLVMOpaqueValue**)p3, p4, (char*)p5);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge591 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jint p4, jlong p5) {
    return (jlong)LLVMBuildInBoundsGEP2((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueType*)p1, (struct LLVMOpaqueValue*)p2, (struct LLVMOpaqueValue**)p3, p4, (char*)p5);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge592 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jint p3, jlong p4) {
    return (jlong)LLVMBuildStructGEP2((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueType*)p1, (struct LLVMOpaqueValue*)p2, p3, (char*)p4);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge593 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)LLVMBuildGlobalString((struct LLVMOpaqueBuilder*)p0, (char*)p1, (char*)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge594 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)LLVMBuildGlobalStringPtr((struct LLVMOpaqueBuilder*)p0, (char*)p1, (char*)p2);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge595 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetVolatile((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge596 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMSetVolatile((struct LLVMOpaqueValue*)p0, p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge597 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetWeak((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge598 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMSetWeak((struct LLVMOpaqueValue*)p0, p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge599 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetOrdering((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge600 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMSetOrdering((struct LLVMOpaqueValue*)p0, p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge601 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetAtomicRMWBinOp((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge602 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMSetAtomicRMWBinOp((struct LLVMOpaqueValue*)p0, p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge603 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildTrunc((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueType*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge604 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildZExt((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueType*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge605 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildSExt((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueType*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge606 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildFPToUI((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueType*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge607 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildFPToSI((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueType*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge608 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildUIToFP((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueType*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge609 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildSIToFP((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueType*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge610 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildFPTrunc((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueType*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge611 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildFPExt((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueType*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge612 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildPtrToInt((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueType*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge613 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildIntToPtr((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueType*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge614 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildBitCast((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueType*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge615 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildAddrSpaceCast((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueType*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge616 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildZExtOrBitCast((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueType*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge617 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildSExtOrBitCast((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueType*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge618 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildTruncOrBitCast((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueType*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge619 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2, jlong p3, jlong p4) {
    return (jlong)LLVMBuildCast((struct LLVMOpaqueBuilder*)p0, p1, (struct LLVMOpaqueValue*)p2, (struct LLVMOpaqueType*)p3, (char*)p4);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge620 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildPointerCast((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueType*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge621 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jint p3, jlong p4) {
    return (jlong)LLVMBuildIntCast2((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueType*)p2, p3, (char*)p4);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge622 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildFPCast((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueType*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge623 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildIntCast((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueType*)p2, (char*)p3);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge624 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2, jint p3) {
    return (jint)LLVMGetCastOpcode((struct LLVMOpaqueValue*)p0, p1, (struct LLVMOpaqueType*)p2, p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge625 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2, jlong p3, jlong p4) {
    return (jlong)LLVMBuildICmp((struct LLVMOpaqueBuilder*)p0, p1, (struct LLVMOpaqueValue*)p2, (struct LLVMOpaqueValue*)p3, (char*)p4);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge626 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2, jlong p3, jlong p4) {
    return (jlong)LLVMBuildFCmp((struct LLVMOpaqueBuilder*)p0, p1, (struct LLVMOpaqueValue*)p2, (struct LLVMOpaqueValue*)p3, (char*)p4);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge627 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)LLVMBuildPhi((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueType*)p1, (char*)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge628 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jint p4, jlong p5) {
    return (jlong)LLVMBuildCall2((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueType*)p1, (struct LLVMOpaqueValue*)p2, (struct LLVMOpaqueValue**)p3, p4, (char*)p5);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge629 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jlong p4) {
    return (jlong)LLVMBuildSelect((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue*)p2, (struct LLVMOpaqueValue*)p3, (char*)p4);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge630 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildVAArg((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueType*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge631 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMBuildExtractElement((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue*)p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge632 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jlong p4) {
    return (jlong)LLVMBuildInsertElement((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue*)p2, (struct LLVMOpaqueValue*)p3, (char*)p4);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge633 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jlong p4) {
    return (jlong)LLVMBuildShuffleVector((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue*)p2, (struct LLVMOpaqueValue*)p3, (char*)p4);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge634 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jint p2, jlong p3) {
    return (jlong)LLVMBuildExtractValue((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge635 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jint p3, jlong p4) {
    return (jlong)LLVMBuildInsertValue((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue*)p2, p3, (char*)p4);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge636 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)LLVMBuildFreeze((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (char*)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge637 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)LLVMBuildIsNull((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (char*)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge638 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)LLVMBuildIsNotNull((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (char*)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge639 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jlong p4) {
    return (jlong)LLVMBuildPtrDiff2((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueType*)p1, (struct LLVMOpaqueValue*)p2, (struct LLVMOpaqueValue*)p3, (char*)p4);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge640 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jint p2, jlong p3) {
    return (jlong)LLVMBuildFence((struct LLVMOpaqueBuilder*)p0, p1, p2, (char*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge641 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2, jlong p3, jint p4, jint p5) {
    return (jlong)LLVMBuildAtomicRMW((struct LLVMOpaqueBuilder*)p0, p1, (struct LLVMOpaqueValue*)p2, (struct LLVMOpaqueValue*)p3, p4, p5);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge642 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jint p4, jint p5, jint p6) {
    return (jlong)LLVMBuildAtomicCmpXchg((struct LLVMOpaqueBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueValue*)p2, (struct LLVMOpaqueValue*)p3, p4, p5, p6);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge643 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetNumMaskElements((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge644 (JNIEnv* jniEnv, jclass jclss) {
    return (jint)LLVMGetUndefMaskElem();
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge645 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    return (jint)LLVMGetMaskValue((struct LLVMOpaqueValue*)p0, p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge646 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMIsAtomicSingleThread((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge647 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMSetAtomicSingleThread((struct LLVMOpaqueValue*)p0, p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge648 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetCmpXchgSuccessOrdering((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge649 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMSetCmpXchgSuccessOrdering((struct LLVMOpaqueValue*)p0, p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge650 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetCmpXchgFailureOrdering((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge651 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMSetCmpXchgFailureOrdering((struct LLVMOpaqueValue*)p0, p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge652 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMCreateModuleProviderForExistingModule((struct LLVMOpaqueModule*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge653 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    LLVMDisposeModuleProvider((struct LLVMOpaqueModuleProvider*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge654 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jint)LLVMCreateMemoryBufferWithContentsOfFile((char*)p0, (struct LLVMOpaqueMemoryBuffer**)p1, (char**)p2);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge655 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jint)LLVMCreateMemoryBufferWithSTDIN((struct LLVMOpaqueMemoryBuffer**)p0, (char**)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge656 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jint p3) {
    return (jlong)LLVMCreateMemoryBufferWithMemoryRange((char*)p0, p1, (char*)p2, p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge657 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)LLVMCreateMemoryBufferWithMemoryRangeCopy((char*)p0, p1, (char*)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge658 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetBufferStart((struct LLVMOpaqueMemoryBuffer*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge659 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetBufferSize((struct LLVMOpaqueMemoryBuffer*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge660 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    LLVMDisposeMemoryBuffer((struct LLVMOpaqueMemoryBuffer*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge661 (JNIEnv* jniEnv, jclass jclss) {
    return (jlong)LLVMGetGlobalPassRegistry();
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge662 (JNIEnv* jniEnv, jclass jclss) {
    return (jlong)LLVMCreatePassManager();
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge663 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMCreateFunctionPassManagerForModule((struct LLVMOpaqueModule*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge664 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMCreateFunctionPassManager((struct LLVMOpaqueModuleProvider*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge665 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jint)LLVMRunPassManager((struct LLVMOpaquePassManager*)p0, (struct LLVMOpaqueModule*)p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge666 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMInitializeFunctionPassManager((struct LLVMOpaquePassManager*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge667 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jint)LLVMRunFunctionPassManager((struct LLVMOpaquePassManager*)p0, (struct LLVMOpaqueValue*)p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge668 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMFinalizeFunctionPassManager((struct LLVMOpaquePassManager*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge669 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    LLVMDisposePassManager((struct LLVMOpaquePassManager*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge670 (JNIEnv* jniEnv, jclass jclss) {
    return (jint)LLVMStartMultithreaded();
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge671 (JNIEnv* jniEnv, jclass jclss) {
    LLVMStopMultithreaded();
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge672 (JNIEnv* jniEnv, jclass jclss) {
    return (jint)LLVMIsMultithreaded();
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge673 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetModuleDataLayout((struct LLVMOpaqueModule*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge674 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMSetModuleDataLayout((struct LLVMOpaqueModule*)p0, (struct LLVMOpaqueTargetData*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge675 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMCreateTargetData((char*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge676 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    LLVMDisposeTargetData((struct LLVMOpaqueTargetData*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge677 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMAddTargetLibraryInfo((struct LLVMOpaqueTargetLibraryInfotData*)p0, (struct LLVMOpaquePassManager*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge678 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMCopyStringRepOfTargetData((struct LLVMOpaqueTargetData*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge679 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMByteOrder((struct LLVMOpaqueTargetData*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge680 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMPointerSize((struct LLVMOpaqueTargetData*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge681 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    return (jint)LLVMPointerSizeForAS((struct LLVMOpaqueTargetData*)p0, p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge682 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMIntPtrTypeInContext((struct LLVMOpaqueContext*)p0, (struct LLVMOpaqueTargetData*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge683 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jint p2) {
    return (jlong)LLVMIntPtrTypeForASInContext((struct LLVMOpaqueContext*)p0, (struct LLVMOpaqueTargetData*)p1, p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge684 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMSizeOfTypeInBits((struct LLVMOpaqueTargetData*)p0, (struct LLVMOpaqueType*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge685 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMStoreSizeOfType((struct LLVMOpaqueTargetData*)p0, (struct LLVMOpaqueType*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge686 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMABISizeOfType((struct LLVMOpaqueTargetData*)p0, (struct LLVMOpaqueType*)p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge687 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jint)LLVMABIAlignmentOfType((struct LLVMOpaqueTargetData*)p0, (struct LLVMOpaqueType*)p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge688 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jint)LLVMCallFrameAlignmentOfType((struct LLVMOpaqueTargetData*)p0, (struct LLVMOpaqueType*)p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge689 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jint)LLVMPreferredAlignmentOfType((struct LLVMOpaqueTargetData*)p0, (struct LLVMOpaqueType*)p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge690 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jint)LLVMPreferredAlignmentOfGlobal((struct LLVMOpaqueTargetData*)p0, (struct LLVMOpaqueValue*)p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge691 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jint)LLVMElementAtOffset((struct LLVMOpaqueTargetData*)p0, (struct LLVMOpaqueType*)p1, p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge692 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jint p2) {
    return (jlong)LLVMOffsetOfElement((struct LLVMOpaqueTargetData*)p0, (struct LLVMOpaqueType*)p1, p2);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge693 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2) {
    return (jint)LLVMVerifyModule((struct LLVMOpaqueModule*)p0, p1, (char**)p2);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge694 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    return (jint)LLVMVerifyFunction((struct LLVMOpaqueValue*)p0, p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge695 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    LLVMViewFunctionCFG((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge696 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    LLVMViewFunctionCFGOnly((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge697 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jint)LLVMWriteBitcodeToFile((struct LLVMOpaqueModule*)p0, (char*)p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge698 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jint p2, jint p3) {
    return (jint)LLVMWriteBitcodeToFD((struct LLVMOpaqueModule*)p0, p1, p2, p3);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge699 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    return (jint)LLVMWriteBitcodeToFileHandle((struct LLVMOpaqueModule*)p0, p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge700 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMWriteBitcodeToMemoryBuffer((struct LLVMOpaqueModule*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge701 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jint)LLVMParseBitcodeInContext((struct LLVMOpaqueContext*)p0, (struct LLVMOpaqueMemoryBuffer*)p1, (struct LLVMOpaqueModule**)p2, (char**)p3);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge702 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jint)LLVMParseBitcodeInContext2((struct LLVMOpaqueContext*)p0, (struct LLVMOpaqueMemoryBuffer*)p1, (struct LLVMOpaqueModule**)p2);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge703 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jint)LLVMGetBitcodeModuleInContext((struct LLVMOpaqueContext*)p0, (struct LLVMOpaqueMemoryBuffer*)p1, (struct LLVMOpaqueModule**)p2, (char**)p3);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge704 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jint)LLVMGetBitcodeModuleInContext2((struct LLVMOpaqueContext*)p0, (struct LLVMOpaqueMemoryBuffer*)p1, (struct LLVMOpaqueModule**)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge705 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetErrorTypeId((struct LLVMOpaqueError*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge706 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    LLVMConsumeError((struct LLVMOpaqueError*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge707 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetErrorMessage((struct LLVMOpaqueError*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge708 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    LLVMDisposeErrorMessage((char*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge709 (JNIEnv* jniEnv, jclass jclss) {
    return (jlong)LLVMGetStringErrorTypeId();
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge710 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMCreateStringError((char*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge711 (JNIEnv* jniEnv, jclass jclss) {
    return (jlong)LLVMGetFirstTarget();
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge712 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetNextTarget((struct LLVMTarget*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge713 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetTargetFromName((char*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge714 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jint)LLVMGetTargetFromTriple((char*)p0, (struct LLVMTarget**)p1, (char**)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge715 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetTargetName((struct LLVMTarget*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge716 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetTargetDescription((struct LLVMTarget*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge717 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMTargetHasJIT((struct LLVMTarget*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge718 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMTargetHasTargetMachine((struct LLVMTarget*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge719 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMTargetHasAsmBackend((struct LLVMTarget*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge720 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jint p4, jint p5, jint p6) {
    return (jlong)LLVMCreateTargetMachine((struct LLVMTarget*)p0, (char*)p1, (char*)p2, (char*)p3, p4, p5, p6);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge721 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    LLVMDisposeTargetMachine((struct LLVMOpaqueTargetMachine*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge722 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetTargetMachineTarget((struct LLVMOpaqueTargetMachine*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge723 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetTargetMachineTriple((struct LLVMOpaqueTargetMachine*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge724 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetTargetMachineCPU((struct LLVMOpaqueTargetMachine*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge725 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetTargetMachineFeatureString((struct LLVMOpaqueTargetMachine*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge726 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMCreateTargetDataLayout((struct LLVMOpaqueTargetMachine*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge727 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMSetTargetMachineAsmVerbosity((struct LLVMOpaqueTargetMachine*)p0, p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge728 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jint p3, jlong p4) {
    return (jint)LLVMTargetMachineEmitToFile((struct LLVMOpaqueTargetMachine*)p0, (struct LLVMOpaqueModule*)p1, (char*)p2, p3, (char**)p4);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge729 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jint p2, jlong p3, jlong p4) {
    return (jint)LLVMTargetMachineEmitToMemoryBuffer((struct LLVMOpaqueTargetMachine*)p0, (struct LLVMOpaqueModule*)p1, p2, (char**)p3, (struct LLVMOpaqueMemoryBuffer**)p4);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge730 (JNIEnv* jniEnv, jclass jclss) {
    return (jlong)LLVMGetDefaultTargetTriple();
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge731 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMNormalizeTargetTriple((char*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge732 (JNIEnv* jniEnv, jclass jclss) {
    return (jlong)LLVMGetHostCPUName();
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge733 (JNIEnv* jniEnv, jclass jclss) {
    return (jlong)LLVMGetHostCPUFeatures();
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge734 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMAddAnalysisPasses((struct LLVMOpaqueTargetMachine*)p0, (struct LLVMOpaquePassManager*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge735 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jlong)LLVMRunPasses((struct LLVMOpaqueModule*)p0, (char*)p1, (struct LLVMOpaqueTargetMachine*)p2, (struct LLVMOpaquePassBuilderOptions*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge736 (JNIEnv* jniEnv, jclass jclss) {
    return (jlong)LLVMCreatePassBuilderOptions();
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge737 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMPassBuilderOptionsSetVerifyEach((struct LLVMOpaquePassBuilderOptions*)p0, p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge738 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMPassBuilderOptionsSetDebugLogging((struct LLVMOpaquePassBuilderOptions*)p0, p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge739 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMPassBuilderOptionsSetLoopInterleaving((struct LLVMOpaquePassBuilderOptions*)p0, p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge740 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMPassBuilderOptionsSetLoopVectorization((struct LLVMOpaquePassBuilderOptions*)p0, p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge741 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMPassBuilderOptionsSetSLPVectorization((struct LLVMOpaquePassBuilderOptions*)p0, p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge742 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMPassBuilderOptionsSetLoopUnrolling((struct LLVMOpaquePassBuilderOptions*)p0, p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge743 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMPassBuilderOptionsSetForgetAllSCEVInLoopUnroll((struct LLVMOpaquePassBuilderOptions*)p0, p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge744 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMPassBuilderOptionsSetLicmMssaOptCap((struct LLVMOpaquePassBuilderOptions*)p0, p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge745 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMPassBuilderOptionsSetLicmMssaNoAccForPromotionCap((struct LLVMOpaquePassBuilderOptions*)p0, p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge746 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMPassBuilderOptionsSetCallGraphProfile((struct LLVMOpaquePassBuilderOptions*)p0, p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge747 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMPassBuilderOptionsSetMergeFunctions((struct LLVMOpaquePassBuilderOptions*)p0, p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge748 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMPassBuilderOptionsSetInlinerThreshold((struct LLVMOpaquePassBuilderOptions*)p0, p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge749 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMPassBuilderOptionsSetMaxDevirtIterations((struct LLVMOpaquePassBuilderOptions*)p0, p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge750 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    LLVMDisposePassBuilderOptions((struct LLVMOpaquePassBuilderOptions*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge751 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jint)LLVMLinkModules2((struct LLVMOpaqueModule*)p0, (struct LLVMOpaqueModule*)p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge752 (JNIEnv* jniEnv, jclass jclss) {
    return (jint)LLVMDebugMetadataVersion();
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge753 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetModuleDebugMetadataVersion((struct LLVMOpaqueModule*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge754 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMStripModuleDebugInfo((struct LLVMOpaqueModule*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge755 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMCreateDIBuilderDisallowUnresolved((struct LLVMOpaqueModule*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge756 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMCreateDIBuilder((struct LLVMOpaqueModule*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge757 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    LLVMDisposeDIBuilder((struct LLVMOpaqueDIBuilder*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge758 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    LLVMDIBuilderFinalize((struct LLVMOpaqueDIBuilder*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge759 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMDIBuilderFinalizeSubprogram((struct LLVMOpaqueDIBuilder*)p0, (struct LLVMOpaqueMetadata*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge760 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2, jlong p3, jlong p4, jint p5, jlong p6, jlong p7, jint p8, jlong p9, jlong p10, jint p11, jint p12, jint p13, jint p14, jlong p15, jlong p16, jlong p17, jlong p18) {
    return (jlong)LLVMDIBuilderCreateCompileUnit((struct LLVMOpaqueDIBuilder*)p0, p1, (struct LLVMOpaqueMetadata*)p2, (char*)p3, p4, p5, (char*)p6, p7, p8, (char*)p9, p10, p11, p12, p13, p14, (char*)p15, p16, (char*)p17, p18);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge761 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jlong p4) {
    return (jlong)LLVMDIBuilderCreateFile((struct LLVMOpaqueDIBuilder*)p0, (char*)p1, p2, (char*)p3, p4);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge762 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jlong p4, jlong p5, jlong p6, jlong p7, jlong p8, jlong p9) {
    return (jlong)LLVMDIBuilderCreateModule((struct LLVMOpaqueDIBuilder*)p0, (struct LLVMOpaqueMetadata*)p1, (char*)p2, p3, (char*)p4, p5, (char*)p6, p7, (char*)p8, p9);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge763 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jint p4) {
    return (jlong)LLVMDIBuilderCreateNameSpace((struct LLVMOpaqueDIBuilder*)p0, (struct LLVMOpaqueMetadata*)p1, (char*)p2, p3, p4);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge764 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jlong p4, jlong p5, jlong p6, jint p7, jlong p8, jint p9, jint p10, jint p11, jint p12, jint p13) {
    return (jlong)LLVMDIBuilderCreateFunction((struct LLVMOpaqueDIBuilder*)p0, (struct LLVMOpaqueMetadata*)p1, (char*)p2, p3, (char*)p4, p5, (struct LLVMOpaqueMetadata*)p6, p7, (struct LLVMOpaqueMetadata*)p8, p9, p10, p11, p12, p13);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge765 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jint p3, jint p4) {
    return (jlong)LLVMDIBuilderCreateLexicalBlock((struct LLVMOpaqueDIBuilder*)p0, (struct LLVMOpaqueMetadata*)p1, (struct LLVMOpaqueMetadata*)p2, p3, p4);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge766 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jint p3) {
    return (jlong)LLVMDIBuilderCreateLexicalBlockFile((struct LLVMOpaqueDIBuilder*)p0, (struct LLVMOpaqueMetadata*)p1, (struct LLVMOpaqueMetadata*)p2, p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge767 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jint p4) {
    return (jlong)LLVMDIBuilderCreateImportedModuleFromNamespace((struct LLVMOpaqueDIBuilder*)p0, (struct LLVMOpaqueMetadata*)p1, (struct LLVMOpaqueMetadata*)p2, (struct LLVMOpaqueMetadata*)p3, p4);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge768 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jint p4, jlong p5, jint p6) {
    return (jlong)LLVMDIBuilderCreateImportedModuleFromAlias((struct LLVMOpaqueDIBuilder*)p0, (struct LLVMOpaqueMetadata*)p1, (struct LLVMOpaqueMetadata*)p2, (struct LLVMOpaqueMetadata*)p3, p4, (struct LLVMOpaqueMetadata**)p5, p6);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge769 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jint p4, jlong p5, jint p6) {
    return (jlong)LLVMDIBuilderCreateImportedModuleFromModule((struct LLVMOpaqueDIBuilder*)p0, (struct LLVMOpaqueMetadata*)p1, (struct LLVMOpaqueMetadata*)p2, (struct LLVMOpaqueMetadata*)p3, p4, (struct LLVMOpaqueMetadata**)p5, p6);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge770 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jint p4, jlong p5, jlong p6, jlong p7, jint p8) {
    return (jlong)LLVMDIBuilderCreateImportedDeclaration((struct LLVMOpaqueDIBuilder*)p0, (struct LLVMOpaqueMetadata*)p1, (struct LLVMOpaqueMetadata*)p2, (struct LLVMOpaqueMetadata*)p3, p4, (char*)p5, p6, (struct LLVMOpaqueMetadata**)p7, p8);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge771 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jint p2, jlong p3, jlong p4) {
    return (jlong)LLVMDIBuilderCreateDebugLocation((struct LLVMOpaqueContext*)p0, p1, p2, (struct LLVMOpaqueMetadata*)p3, (struct LLVMOpaqueMetadata*)p4);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge772 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMDILocationGetLine((struct LLVMOpaqueMetadata*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge773 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMDILocationGetColumn((struct LLVMOpaqueMetadata*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge774 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMDILocationGetScope((struct LLVMOpaqueMetadata*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge775 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMDILocationGetInlinedAt((struct LLVMOpaqueMetadata*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge776 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMDIScopeGetFile((struct LLVMOpaqueMetadata*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge777 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMDIFileGetDirectory((struct LLVMOpaqueMetadata*)p0, (unsigned int*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge778 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMDIFileGetFilename((struct LLVMOpaqueMetadata*)p0, (unsigned int*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge779 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMDIFileGetSource((struct LLVMOpaqueMetadata*)p0, (unsigned int*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge780 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)LLVMDIBuilderGetOrCreateTypeArray((struct LLVMOpaqueDIBuilder*)p0, (struct LLVMOpaqueMetadata**)p1, p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge781 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jint p3, jint p4) {
    return (jlong)LLVMDIBuilderCreateSubroutineType((struct LLVMOpaqueDIBuilder*)p0, (struct LLVMOpaqueMetadata*)p1, (struct LLVMOpaqueMetadata**)p2, p3, p4);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge782 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jint p2, jint p3, jlong p4, jlong p5, jlong p6, jlong p7) {
    return (jlong)LLVMDIBuilderCreateMacro((struct LLVMOpaqueDIBuilder*)p0, (struct LLVMOpaqueMetadata*)p1, p2, p3, (char*)p4, p5, (char*)p6, p7);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge783 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jint p2, jlong p3) {
    return (jlong)LLVMDIBuilderCreateTempMacroFile((struct LLVMOpaqueDIBuilder*)p0, (struct LLVMOpaqueMetadata*)p1, p2, (struct LLVMOpaqueMetadata*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge784 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jint p4) {
    return (jlong)LLVMDIBuilderCreateEnumerator((struct LLVMOpaqueDIBuilder*)p0, (char*)p1, p2, p3, p4);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge785 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jlong p4, jint p5, jlong p6, jint p7, jlong p8, jint p9, jlong p10) {
    return (jlong)LLVMDIBuilderCreateEnumerationType((struct LLVMOpaqueDIBuilder*)p0, (struct LLVMOpaqueMetadata*)p1, (char*)p2, p3, (struct LLVMOpaqueMetadata*)p4, p5, p6, p7, (struct LLVMOpaqueMetadata**)p8, p9, (struct LLVMOpaqueMetadata*)p10);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge786 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jlong p4, jint p5, jlong p6, jint p7, jint p8, jlong p9, jint p10, jint p11, jlong p12, jlong p13) {
    return (jlong)LLVMDIBuilderCreateUnionType((struct LLVMOpaqueDIBuilder*)p0, (struct LLVMOpaqueMetadata*)p1, (char*)p2, p3, (struct LLVMOpaqueMetadata*)p4, p5, p6, p7, p8, (struct LLVMOpaqueMetadata**)p9, p10, p11, (char*)p12, p13);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge787 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jint p2, jlong p3, jlong p4, jint p5) {
    return (jlong)LLVMDIBuilderCreateArrayType((struct LLVMOpaqueDIBuilder*)p0, p1, p2, (struct LLVMOpaqueMetadata*)p3, (struct LLVMOpaqueMetadata**)p4, p5);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge788 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jint p2, jlong p3, jlong p4, jint p5) {
    return (jlong)LLVMDIBuilderCreateVectorType((struct LLVMOpaqueDIBuilder*)p0, p1, p2, (struct LLVMOpaqueMetadata*)p3, (struct LLVMOpaqueMetadata**)p4, p5);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge789 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)LLVMDIBuilderCreateUnspecifiedType((struct LLVMOpaqueDIBuilder*)p0, (char*)p1, p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge790 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jint p4, jint p5) {
    return (jlong)LLVMDIBuilderCreateBasicType((struct LLVMOpaqueDIBuilder*)p0, (char*)p1, p2, p3, p4, p5);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge791 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jint p3, jint p4, jlong p5, jlong p6) {
    return (jlong)LLVMDIBuilderCreatePointerType((struct LLVMOpaqueDIBuilder*)p0, (struct LLVMOpaqueMetadata*)p1, p2, p3, p4, (char*)p5, p6);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge792 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jlong p4, jint p5, jlong p6, jint p7, jint p8, jlong p9, jlong p10, jint p11, jint p12, jlong p13, jlong p14, jlong p15) {
    return (jlong)LLVMDIBuilderCreateStructType((struct LLVMOpaqueDIBuilder*)p0, (struct LLVMOpaqueMetadata*)p1, (char*)p2, p3, (struct LLVMOpaqueMetadata*)p4, p5, p6, p7, p8, (struct LLVMOpaqueMetadata*)p9, (struct LLVMOpaqueMetadata**)p10, p11, p12, (struct LLVMOpaqueMetadata*)p13, (char*)p14, p15);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge793 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jlong p4, jint p5, jlong p6, jint p7, jlong p8, jint p9, jlong p10) {
    return (jlong)LLVMDIBuilderCreateMemberType((struct LLVMOpaqueDIBuilder*)p0, (struct LLVMOpaqueMetadata*)p1, (char*)p2, p3, (struct LLVMOpaqueMetadata*)p4, p5, p6, p7, p8, p9, (struct LLVMOpaqueMetadata*)p10);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge794 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jlong p4, jint p5, jlong p6, jint p7, jlong p8, jint p9) {
    return (jlong)LLVMDIBuilderCreateStaticMemberType((struct LLVMOpaqueDIBuilder*)p0, (struct LLVMOpaqueMetadata*)p1, (char*)p2, p3, (struct LLVMOpaqueMetadata*)p4, p5, (struct LLVMOpaqueMetadata*)p6, p7, (struct LLVMOpaqueValue*)p8, p9);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge795 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jint p4, jint p5) {
    return (jlong)LLVMDIBuilderCreateMemberPointerType((struct LLVMOpaqueDIBuilder*)p0, (struct LLVMOpaqueMetadata*)p1, (struct LLVMOpaqueMetadata*)p2, p3, p4, p5);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge796 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jint p4, jlong p5, jint p6, jlong p7, jint p8, jlong p9, jlong p10) {
    return (jlong)LLVMDIBuilderCreateObjCIVar((struct LLVMOpaqueDIBuilder*)p0, (char*)p1, p2, (struct LLVMOpaqueMetadata*)p3, p4, p5, p6, p7, p8, (struct LLVMOpaqueMetadata*)p9, (struct LLVMOpaqueMetadata*)p10);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge797 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jint p4, jlong p5, jlong p6, jlong p7, jlong p8, jint p9, jlong p10) {
    return (jlong)LLVMDIBuilderCreateObjCProperty((struct LLVMOpaqueDIBuilder*)p0, (char*)p1, p2, (struct LLVMOpaqueMetadata*)p3, p4, (char*)p5, p6, (char*)p7, p8, p9, (struct LLVMOpaqueMetadata*)p10);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge798 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMDIBuilderCreateObjectPointerType((struct LLVMOpaqueDIBuilder*)p0, (struct LLVMOpaqueMetadata*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge799 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2) {
    return (jlong)LLVMDIBuilderCreateQualifiedType((struct LLVMOpaqueDIBuilder*)p0, p1, (struct LLVMOpaqueMetadata*)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge800 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2) {
    return (jlong)LLVMDIBuilderCreateReferenceType((struct LLVMOpaqueDIBuilder*)p0, p1, (struct LLVMOpaqueMetadata*)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge801 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMDIBuilderCreateNullPtrType((struct LLVMOpaqueDIBuilder*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge802 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jlong p4, jint p5, jlong p6, jint p7) {
    return (jlong)LLVMDIBuilderCreateTypedef((struct LLVMOpaqueDIBuilder*)p0, (struct LLVMOpaqueMetadata*)p1, (char*)p2, p3, (struct LLVMOpaqueMetadata*)p4, p5, (struct LLVMOpaqueMetadata*)p6, p7);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge803 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jint p4, jint p5) {
    return (jlong)LLVMDIBuilderCreateInheritance((struct LLVMOpaqueDIBuilder*)p0, (struct LLVMOpaqueMetadata*)p1, (struct LLVMOpaqueMetadata*)p2, p3, p4, p5);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge804 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2, jlong p3, jlong p4, jlong p5, jint p6, jint p7, jlong p8, jint p9, jlong p10, jlong p11) {
    return (jlong)LLVMDIBuilderCreateForwardDecl((struct LLVMOpaqueDIBuilder*)p0, p1, (char*)p2, p3, (struct LLVMOpaqueMetadata*)p4, (struct LLVMOpaqueMetadata*)p5, p6, p7, p8, p9, (char*)p10, p11);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge805 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2, jlong p3, jlong p4, jlong p5, jint p6, jint p7, jlong p8, jint p9, jint p10, jlong p11, jlong p12) {
    return (jlong)LLVMDIBuilderCreateReplaceableCompositeType((struct LLVMOpaqueDIBuilder*)p0, p1, (char*)p2, p3, (struct LLVMOpaqueMetadata*)p4, (struct LLVMOpaqueMetadata*)p5, p6, p7, p8, p9, p10, (char*)p11, p12);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge806 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jlong p4, jint p5, jlong p6, jlong p7, jlong p8, jint p9, jlong p10) {
    return (jlong)LLVMDIBuilderCreateBitFieldMemberType((struct LLVMOpaqueDIBuilder*)p0, (struct LLVMOpaqueMetadata*)p1, (char*)p2, p3, (struct LLVMOpaqueMetadata*)p4, p5, p6, p7, p8, p9, (struct LLVMOpaqueMetadata*)p10);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge807 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jlong p4, jint p5, jlong p6, jint p7, jlong p8, jint p9, jlong p10, jlong p11, jint p12, jlong p13, jlong p14, jlong p15, jlong p16) {
    return (jlong)LLVMDIBuilderCreateClassType((struct LLVMOpaqueDIBuilder*)p0, (struct LLVMOpaqueMetadata*)p1, (char*)p2, p3, (struct LLVMOpaqueMetadata*)p4, p5, p6, p7, p8, p9, (struct LLVMOpaqueMetadata*)p10, (struct LLVMOpaqueMetadata**)p11, p12, (struct LLVMOpaqueMetadata*)p13, (struct LLVMOpaqueMetadata*)p14, (char*)p15, p16);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge808 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMDIBuilderCreateArtificialType((struct LLVMOpaqueDIBuilder*)p0, (struct LLVMOpaqueMetadata*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge809 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMDITypeGetName((struct LLVMOpaqueMetadata*)p0, (unsigned long long*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge810 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMDITypeGetSizeInBits((struct LLVMOpaqueMetadata*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge811 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMDITypeGetOffsetInBits((struct LLVMOpaqueMetadata*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge812 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMDITypeGetAlignInBits((struct LLVMOpaqueMetadata*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge813 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMDITypeGetLine((struct LLVMOpaqueMetadata*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge814 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMDITypeGetFlags((struct LLVMOpaqueMetadata*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge815 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)LLVMDIBuilderGetOrCreateSubrange((struct LLVMOpaqueDIBuilder*)p0, p1, p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge816 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)LLVMDIBuilderGetOrCreateArray((struct LLVMOpaqueDIBuilder*)p0, (struct LLVMOpaqueMetadata**)p1, p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge817 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)LLVMDIBuilderCreateExpression((struct LLVMOpaqueDIBuilder*)p0, (unsigned long long*)p1, p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge818 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)LLVMDIBuilderCreateConstantValueExpression((struct LLVMOpaqueDIBuilder*)p0, p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge819 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jlong p4, jlong p5, jlong p6, jint p7, jlong p8, jint p9, jlong p10, jlong p11, jint p12) {
    return (jlong)LLVMDIBuilderCreateGlobalVariableExpression((struct LLVMOpaqueDIBuilder*)p0, (struct LLVMOpaqueMetadata*)p1, (char*)p2, p3, (char*)p4, p5, (struct LLVMOpaqueMetadata*)p6, p7, (struct LLVMOpaqueMetadata*)p8, p9, (struct LLVMOpaqueMetadata*)p10, (struct LLVMOpaqueMetadata*)p11, p12);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge820 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMDIGlobalVariableExpressionGetVariable((struct LLVMOpaqueMetadata*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge821 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMDIGlobalVariableExpressionGetExpression((struct LLVMOpaqueMetadata*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge822 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMDIVariableGetFile((struct LLVMOpaqueMetadata*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge823 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMDIVariableGetScope((struct LLVMOpaqueMetadata*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge824 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMDIVariableGetLine((struct LLVMOpaqueMetadata*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge825 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)LLVMTemporaryMDNode((struct LLVMOpaqueContext*)p0, (struct LLVMOpaqueMetadata**)p1, p2);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge826 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    LLVMDisposeTemporaryMDNode((struct LLVMOpaqueMetadata*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge827 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMMetadataReplaceAllUsesWith((struct LLVMOpaqueMetadata*)p0, (struct LLVMOpaqueMetadata*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge828 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jlong p4, jlong p5, jlong p6, jint p7, jlong p8, jint p9, jlong p10, jint p11) {
    return (jlong)LLVMDIBuilderCreateTempGlobalVariableFwdDecl((struct LLVMOpaqueDIBuilder*)p0, (struct LLVMOpaqueMetadata*)p1, (char*)p2, p3, (char*)p4, p5, (struct LLVMOpaqueMetadata*)p6, p7, (struct LLVMOpaqueMetadata*)p8, p9, (struct LLVMOpaqueMetadata*)p10, p11);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge829 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jlong p4, jlong p5) {
    return (jlong)LLVMDIBuilderInsertDeclareBefore((struct LLVMOpaqueDIBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueMetadata*)p2, (struct LLVMOpaqueMetadata*)p3, (struct LLVMOpaqueMetadata*)p4, (struct LLVMOpaqueValue*)p5);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge830 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jlong p4, jlong p5) {
    return (jlong)LLVMDIBuilderInsertDeclareAtEnd((struct LLVMOpaqueDIBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueMetadata*)p2, (struct LLVMOpaqueMetadata*)p3, (struct LLVMOpaqueMetadata*)p4, (struct LLVMOpaqueBasicBlock*)p5);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge831 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jlong p4, jlong p5) {
    return (jlong)LLVMDIBuilderInsertDbgValueBefore((struct LLVMOpaqueDIBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueMetadata*)p2, (struct LLVMOpaqueMetadata*)p3, (struct LLVMOpaqueMetadata*)p4, (struct LLVMOpaqueValue*)p5);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge832 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jlong p4, jlong p5) {
    return (jlong)LLVMDIBuilderInsertDbgValueAtEnd((struct LLVMOpaqueDIBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct LLVMOpaqueMetadata*)p2, (struct LLVMOpaqueMetadata*)p3, (struct LLVMOpaqueMetadata*)p4, (struct LLVMOpaqueBasicBlock*)p5);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge833 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jlong p4, jint p5, jlong p6, jint p7, jint p8, jint p9) {
    return (jlong)LLVMDIBuilderCreateAutoVariable((struct LLVMOpaqueDIBuilder*)p0, (struct LLVMOpaqueMetadata*)p1, (char*)p2, p3, (struct LLVMOpaqueMetadata*)p4, p5, (struct LLVMOpaqueMetadata*)p6, p7, p8, p9);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge834 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jint p4, jlong p5, jint p6, jlong p7, jint p8, jint p9) {
    return (jlong)LLVMDIBuilderCreateParameterVariable((struct LLVMOpaqueDIBuilder*)p0, (struct LLVMOpaqueMetadata*)p1, (char*)p2, p3, p4, (struct LLVMOpaqueMetadata*)p5, p6, (struct LLVMOpaqueMetadata*)p7, p8, p9);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge835 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMGetSubprogram((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge836 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMSetSubprogram((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueMetadata*)p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge837 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMDISubprogramGetLine((struct LLVMOpaqueMetadata*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge838 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMInstructionGetDebugLoc((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge839 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMInstructionSetDebugLoc((struct LLVMOpaqueValue*)p0, (struct LLVMOpaqueMetadata*)p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge840 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetMetadataKind((struct LLVMOpaqueMetadata*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge841 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    DIFinalize((struct LLVMOpaqueDIBuilder*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge842 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2, jlong p3, jlong p4, jint p5, jlong p6, jint p7) {
    return (jlong)DICreateCompilationUnit((struct LLVMOpaqueDIBuilder*)p0, p1, (char*)p2, (char*)p3, (char*)p4, p5, (char*)p6, p7);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge843 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)DICreateFile((struct LLVMOpaqueDIBuilder*)p0, (char*)p1, (char*)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge844 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jint p4) {
    return (jlong)DICreateBasicType((struct LLVMOpaqueDIBuilder*)p0, (char*)p1, p2, p3, p4);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge845 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jint p4, jlong p5, jlong p6, jint p7, jlong p8, jlong p9, jlong p10, jlong p11) {
    return (jlong)DICreateStructType((struct LLVMOpaqueDIBuilder*)p0, (struct DIScope*)p1, (char*)p2, (struct DIFile*)p3, p4, p5, p6, p7, (struct DIType*)p8, (struct DIDerivedType**)p9, p10, (struct DICompositeType*)p11);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge846 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jlong p4) {
    return (jlong)DICreateArrayType((struct LLVMOpaqueDIBuilder*)p0, p1, p2, (struct DIType*)p3, p4);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge847 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)DICreateReferenceType((struct LLVMOpaqueDIBuilder*)p0, (struct DIType*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge848 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)DICreatePointerType((struct LLVMOpaqueDIBuilder*)p0, (struct DIType*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge849 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2, jlong p3, jlong p4, jint p5) {
    return (jlong)DICreateReplaceableCompositeType((struct LLVMOpaqueDIBuilder*)p0, p1, (char*)p2, (struct DIScope*)p3, (struct DIFile*)p4, p5);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge850 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jint p4, jlong p5, jlong p6, jlong p7, jint p8, jlong p9) {
    return (jlong)DICreateMemberType((struct LLVMOpaqueDIBuilder*)p0, (struct DIScope*)p1, (char*)p2, (struct DIFile*)p3, p4, p5, p6, p7, p8, (struct DIType*)p9);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge851 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jlong p4, jlong p5) {
    return (jlong)DICreateModule((struct LLVMOpaqueDIBuilder*)p0, (struct DIScope*)p1, (char*)p2, (char*)p3, (char*)p4, (char*)p5);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge852 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)DICreateLexicalBlockFile((struct LLVMOpaqueDIBuilder*)p0, (struct DIScope*)p1, (struct DIFile*)p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge853 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jint p3, jint p4) {
    return (jlong)DICreateLexicalBlock((struct LLVMOpaqueDIBuilder*)p0, (struct DIScope*)p1, (struct DIFile*)p2, p3, p4);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge854 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jlong p4, jint p5, jlong p6, jint p7, jint p8, jint p9, jint p10) {
    return (jlong)DICreateFunction((struct LLVMOpaqueDIBuilder*)p0, (struct DIScope*)p1, (char*)p2, (char*)p3, (struct DIFile*)p4, p5, (struct DISubroutineType*)p6, p7, p8, p9, p10);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge855 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jint p4, jlong p5, jint p6, jint p7, jint p8, jint p9) {
    return (jlong)DICreateBridgeFunction((struct LLVMOpaqueDIBuilder*)p0, (struct DIScope*)p1, (struct LLVMOpaqueValue*)p2, (struct DIFile*)p3, p4, (struct DISubroutineType*)p5, p6, p7, p8, p9);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge856 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jint p2) {
    return (jlong)DICreateSubroutineType((struct LLVMOpaqueDIBuilder*)p0, (struct DIType**)p1, p2);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge857 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jint p4, jlong p5) {
    return (jlong)DICreateAutoVariable((struct LLVMOpaqueDIBuilder*)p0, (struct DIScope*)p1, (char*)p2, (struct DIFile*)p3, p4, (struct DIType*)p5);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge858 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jint p3, jlong p4, jint p5, jlong p6) {
    return (jlong)DICreateParameterVariable((struct LLVMOpaqueDIBuilder*)p0, (struct DIScope*)p1, (char*)p2, p3, (struct DIFile*)p4, p5, (struct DIType*)p6);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge859 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jlong p4, jlong p5, jlong p6) {
    DIInsertDeclaration((struct LLVMOpaqueDIBuilder*)p0, (struct LLVMOpaqueValue*)p1, (struct DILocalVariable*)p2, (struct DILocation*)p3, (struct LLVMOpaqueBasicBlock*)p4, (long long*)p5, p6);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge860 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)DICreateEmptyExpression((struct LLVMOpaqueDIBuilder*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge861 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    DIFunctionAddSubprogram((struct LLVMOpaqueValue*)p0, (struct DISubprogram*)p1);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge862 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jint p2, jlong p3) {
    return (jlong)LLVMCreateLocation((struct LLVMOpaqueContext*)p0, p1, p2, (struct DIScope*)p3);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge863 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jint p2, jlong p3, jlong p4) {
    return (jlong)LLVMCreateLocationInlinedAt((struct LLVMOpaqueContext*)p0, p1, p2, (struct DIScope*)p3, (struct DILocation*)p4);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge864 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    LLVMBuilderSetDebugLocation((struct LLVMOpaqueBuilder*)p0, (struct DILocation*)p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge865 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    LLVMBuilderResetDebugLocation((struct LLVMOpaqueBuilder*)p0);
}
JNIEXPORT jlong JNICALL Java_llvm_llvm_kniBridge866 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)LLVMBuilderGetCurrentFunction((struct LLVMOpaqueBuilder*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge867 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jint)DISubprogramDescribesFunction((struct DISubprogram*)p0, (struct LLVMOpaqueValue*)p1);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge868 (JNIEnv* jniEnv, jclass jclss) {
    LLVMKotlinInitializeTargets();
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge869 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    LLVMSetNoTailCall((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge870 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMInlineCall((struct LLVMOpaqueValue*)p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge871 (JNIEnv* jniEnv, jclass jclss, jint p0) {
    LLVMSetTimePasses(p0);
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge872 (JNIEnv* jniEnv, jclass jclss) {
    LLVMPrintAllTimersToStdOut();
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge873 (JNIEnv* jniEnv, jclass jclss) {
    LLVMClearAllTimers();
}
JNIEXPORT void JNICALL Java_llvm_llvm_kniBridge874 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    LLVMKotlinRemoveRedundantSafepoints((struct LLVMOpaqueModule*)p0, p1);
}
JNIEXPORT jint JNICALL Java_llvm_llvm_kniBridge875 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)LLVMGetProgramAddressSpace((struct LLVMOpaqueModule*)p0);
}
