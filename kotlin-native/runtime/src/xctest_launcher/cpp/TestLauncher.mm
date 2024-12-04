/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

#import "Memory.h"
#import "ObjCExport.h"
#import "TestLauncher.h"

extern "C" OBJ_GETTER0(Konan_create_testSuite);

@interface XCTestLauncher : XCTestCase
@end

@implementation XCTestLauncher
/**
 * Test suites factory.
 *
 * This is a starting point for XCTest to get test suites with test cases.
 * K/N dynamically adds test suites for Kotlin tests.
 * See `setupXCTestSuite` Kotlin method.
 */
+ (id)defaultTestSuite {
    kotlin::CalledFromNativeGuard guard;
    ObjHolder holder;
    return Kotlin_ObjCExport_refToObjC(Konan_create_testSuite(holder.slot()));
}
@end
