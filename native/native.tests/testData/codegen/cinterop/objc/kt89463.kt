// TARGET_BACKEND: NATIVE
// DISABLE_NATIVE: isAppleTarget=false
// WITH_PLATFORM_LIBS
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_STAGE: Native:2.4
// ^^^ KT-89463: Unable to compile C bridge

// KT-89463
// IGNORE_BACKEND: NATIVE

// MODULE: cinterop
// FILE: lib.def
language = Objective-C
headers = lib.h
headerFilter = lib.h

// FILE: lib.h
#import <Foundation/Foundation.h>
#import <CoreGraphics/CoreGraphics.h>

NS_ASSUME_NONNULL_BEGIN

// Has the same block type as `UIScreenshotServiceDelegate.screenshotService(_:generatePDFRepresentationWithCompletion:)`.
@protocol KT89463Delegate <NSObject>
- (void)generateWithCompletion:(void (^)(NSData * _Nullable data, NSInteger index, CGRect rect))completion;
@end

NSString* kt89463CallDelegate(id<KT89463Delegate> delegate);

NS_ASSUME_NONNULL_END

// FILE: lib.m
#import "lib.h"

NSString* kt89463CallDelegate(id<KT89463Delegate> delegate) {
    __block NSString* result = @"not called";
    [delegate generateWithCompletion:^(NSData * _Nullable data, NSInteger index, CGRect rect) {
        result = [NSString stringWithFormat:@"%lu %ld %g %g %g %g",
            (unsigned long)data.length, (long)index, rect.origin.x, rect.origin.y, rect.size.width, rect.size.height];
    }];
    return result;
}

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import lib.*
import kotlinx.cinterop.*

class Delegate : NSObject(), KT89463DelegateProtocol {
    override fun generateWithCompletion(completion: (NSData?, NSInteger, CValue<CGRect>) -> Unit) {
        completion(NSData(), 2, cValue {
            origin.x = 3.0
            origin.y = 4.0
            size.width = 5.0
            size.height = 6.0
        })
    }
}

fun box(): String {
    val result = kt89463CallDelegate(Delegate())
    return if (result == "0 2 3 4 5 6") "OK" else "FAIL: $result"
}
