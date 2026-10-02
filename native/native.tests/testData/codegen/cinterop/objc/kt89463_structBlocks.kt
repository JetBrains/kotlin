// TARGET_BACKEND: NATIVE
// DISABLE_NATIVE: isAppleTarget=false
// WITH_PLATFORM_LIBS
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_STAGE: Native:2.4
// ^^^ KT-89463: Unable to compile C bridge

// MODULE: cinterop
// FILE: lib.def
language = Objective-C
headers = lib.h
headerFilter = lib.h

// FILE: lib.h
#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

typedef struct {
    int x;
    int y;
} KT89463Struct;

@protocol KT89463StructBlockConsumer <NSObject>
- (int)callStructReturningBlock:(KT89463Struct (^)(void))block;
@end

// Calls [consumer callStructReturningBlock:] with a block returning {x, y}.
int kt89463CallStructReturningBlock(id<KT89463StructBlockConsumer> consumer, int x, int y);

// Returns a block computing `s.x * 10 + s.y`.
int (^kt89463GetStructBlock(void))(KT89463Struct s);

// Returns the same block as a retained raw pointer.
void* kt89463GetStructBlockPtr(void);

NS_ASSUME_NONNULL_END

// FILE: lib.m
#import "lib.h"

int kt89463CallStructReturningBlock(id<KT89463StructBlockConsumer> consumer, int x, int y) {
    return [consumer callStructReturningBlock:^KT89463Struct {
        KT89463Struct result = { x, y };
        return result;
    }];
}

int (^kt89463GetStructBlock(void))(KT89463Struct s) {
    return ^int(KT89463Struct s) {
        return s.x * 10 + s.y;
    };
}

void* kt89463GetStructBlockPtr(void) {
    return (__bridge_retained void*)kt89463GetStructBlock();
}

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import lib.*
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction
import kotlin.test.*

class StructBlockConsumer : NSObject(), KT89463StructBlockConsumerProtocol {
    override fun callStructReturningBlock(block: () -> CValue<KT89463Struct>): Int =
        block().useContents { x * 10 + y }
}

fun structOf(x: Int, y: Int): CValue<KT89463Struct> = cValue {
    this.x = x
    this.y = y
}

fun box(): String {
    // An Obj-C method implemented in Kotlin receives a block returning a struct.
    assertEquals(12, kt89463CallStructReturningBlock(StructBlockConsumer(), 1, 2))

    // A block with a struct parameter is returned from Obj-C.
    assertEquals(34, kt89463GetStructBlock()(structOf(3, 4)))

    // The same block is converted by the intrinsic used by Swift export.
    val blockPtr = kt89463GetStructBlockPtr()!!.rawValue
    val function = convertBlockPtrToKotlinFunction<(CValue<KT89463Struct>) -> Int>(blockPtr)
    objc_release(blockPtr) // Balance __bridge_retained.
    assertEquals(56, function(structOf(5, 6)))

    return "OK"
}
