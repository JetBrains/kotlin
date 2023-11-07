/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/**
 * These are stub declarations from XCTest.framework needed for the launcher compilation.
 * It makes possible to compile it without dependency on the framework, while the
 * final build of the bundle should be done with a real XCTest.framework library.
 */

@interface XCTestCase
@end

@interface XCTestSuite
@end

@interface XCTestCase (XCTestSuiteExtensions)
@property (class, readonly) XCTestSuite *defaultTestSuite;
@end