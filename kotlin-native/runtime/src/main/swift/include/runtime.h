#include <stdint.h>
#pragma clang assume_nonnull begin

void initRuntimeIfNeeded() asm("_Kotlin_initRuntimeIfNeeded");

void switchThreadStateToRunnable() asm("_Kotlin_mm_switchThreadStateRunnable");

void switchThreadStateToNative() asm("_Kotlin_mm_switchThreadStateNative");

void *refToSwiftObject(void * kotlinObject)  asm("_Kotlin_SwiftExport_refToSwiftObject");

void *swiftObjectToRef(void * swiftObject, void * result)  asm("_Kotlin_SwiftExport_swiftObjectToRef");

void EnterFrame(void* start, int parameters, int count);

void LeaveFrame(void* start, int parameters, int count);

#pragma clang assume_nonnull end