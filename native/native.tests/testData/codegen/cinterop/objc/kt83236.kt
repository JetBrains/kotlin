// ISSUE: KT-83236
// TARGET_BACKEND: NATIVE
// NATIVE_STANDALONE
// DISABLE_NATIVE: isAppleTarget=false
// WITH_PLATFORM_LIBS

// The Objective-C members below use Foundation's NSRoundingMode, which is a CEnum.
// The following problem was triggered while deserializing inherited Objective-C members for a Kotlin subclass,
// when enum is not mentioned by name from Kotlin:
// exception: java.lang.IllegalArgumentException: The symbol table has been sealed. irClass = CLASS IR_EXTERNAL_DECLARATION_STUB ENUM_CLASS name:NSRoundingMode modality:FINAL visibility:public superTypes:[kotlin.Enum<lib.NSRoundingMode>; kotlinx.cinterop.CEnum]
//     at org.jetbrains.kotlin.backend.konan.optimizations.DataFlowIR$SymbolTable.mapClassReferenceType(DataFlowIR.kt:529)

// MODULE: cinterop
// FILE: lib.def
language = Objective-C
headers = lib.h
headerFilter = lib.h

// FILE: lib.h
#import <Foundation/Foundation.h>

@protocol KT83236Protocol <NSObject>
@property(nonatomic, assign) NSRoundingMode roundingMode;
@end

@interface KT83236Object : NSObject <KT83236Protocol>
@end

// FILE: lib.m
#import "lib.h"

@implementation KT83236Object

- (NSRoundingMode)roundingMode {
    return NSRoundPlain;
}

- (void)setRoundingMode:(NSRoundingMode)roundingMode {
}

@end

// MODULE: lib(cinterop)
// FILE: lib.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import lib.KT83236Object

class KT83236KotlinSubclass : KT83236Object()

// MODULE: main(lib, cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

fun box(): String {
    KT83236KotlinSubclass()
    return "OK"
}
