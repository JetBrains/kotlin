// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_STAGE: Native:2.3
// ^^^ KT-86031: NPE: null cannot be cast to non-null type org.jetbrains.kotlin.ir.expressions.IrConst

// TARGET_BACKEND: NATIVE
// DISABLE_NATIVE: isAppleTarget=false

// MODULE: cinterop
// FILE: lib.def
language = Objective-C
headers = lib.h

// FILE: lib.h
#import <Foundation/Foundation.h>

@protocol AService <NSObject>
@end

@interface AServiceImpl : NSObject <AService>
@end

@interface ServiceModule : NSObject
@property (nonatomic, strong) Class<AService> _Nonnull aService;
@end

// FILE: lib.m
#import "lib.h"

@implementation AServiceImpl
@end

@implementation ServiceModule
- (instancetype)init {
    self = [super init];
    if (self) {
        _aService = [AServiceImpl class];
    }
    return self;
}
@end

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import lib.*

fun box(): String {
    val module = ServiceModule()
    val service = module.aService

    return if (service is AServiceProtocolMeta) "OK" else "FAIL: $service"
}
