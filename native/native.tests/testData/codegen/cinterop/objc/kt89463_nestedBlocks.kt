// TARGET_BACKEND: NATIVE
// DISABLE_NATIVE: isAppleTarget=false
// WITH_PLATFORM_LIBS
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_STAGE: Native:2.4
// ^^^ KT-89463: Unable to compile C bridge

// KT-89463
// IGNORE_NATIVE: isAppleTarget=true

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

@protocol KT89463NestedBlockConsumer <NSObject>
- (void)callWithStructConsumer:(void (^)(void (^)(KT89463Struct)))block;
- (void)callWithStructProducer:(void (^)(KT89463Struct (^)(void)))block;
@end

// Calls [consumer callWithStructConsumer:] with a block that passes {x, y} to its argument.
void kt89463PassStructToConsumer(id<KT89463NestedBlockConsumer> consumer, int x, int y);

// Calls [consumer callWithStructProducer:] with a block that calls its argument,
// and returns `s.x * 10 + s.y` for the struct `s` it produced.
int kt89463TakeStructFromProducer(id<KT89463NestedBlockConsumer> consumer);

NS_ASSUME_NONNULL_END

// FILE: lib.m
#import "lib.h"

void kt89463PassStructToConsumer(id<KT89463NestedBlockConsumer> consumer, int x, int y) {
    [consumer callWithStructConsumer:^(void (^structConsumer)(KT89463Struct)) {
        KT89463Struct s = { x, y };
        structConsumer(s);
    }];
}

int kt89463TakeStructFromProducer(id<KT89463NestedBlockConsumer> consumer) {
    __block int result = -1;
    [consumer callWithStructProducer:^(KT89463Struct (^structProducer)(void)) {
        KT89463Struct s = structProducer();
        result = s.x * 10 + s.y;
    }];
    return result;
}

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import lib.*
import kotlinx.cinterop.*
import kotlin.test.*

class NestedBlockConsumer : NSObject(), KT89463NestedBlockConsumerProtocol {
    var consumed: Int = -1

    override fun callWithStructConsumer(block: (((CValue<KT89463Struct>) -> Unit)?) -> Unit) {
        block { s -> consumed = s.useContents { x * 10 + y } }
    }

    override fun callWithStructProducer(block: ((() -> CValue<KT89463Struct>)?) -> Unit) {
        block {
            cValue {
                x = 3
                y = 4
            }
        }
    }
}

fun box(): String {
    val consumer = NestedBlockConsumer()

    // Kotlin passes a lambda taking a struct to a block received from Obj-C.
    kt89463PassStructToConsumer(consumer, 1, 2)
    assertEquals(12, consumer.consumed)

    // Kotlin passes a lambda returning a struct to a block received from Obj-C.
    assertEquals(34, kt89463TakeStructFromProducer(consumer))

    return "OK"
}
