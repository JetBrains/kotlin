/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Kt

class OverrideKotlinMethodsImpl : OverrideKotlinMethods4, OverrideKotlinMethods6 {
    override func one() -> Int32 {
        return 1
    }
}

private func test1() throws {
    let obj = OverrideKotlinMethodsImpl()

    try OverrideKotlinMethodsKt.test0(obj: obj)
    try OverrideKotlinMethodsKt.test1(obj: obj)
    try OverrideKotlinMethodsKt.test2(obj: obj)
    try OverrideKotlinMethodsKt.test3(obj: obj)
    try OverrideKotlinMethodsKt.test4(obj: obj)
    try OverrideKotlinMethodsKt.test5(obj: obj)
    try OverrideKotlinMethodsKt.test6(obj: obj)
}

class OverrideKotlinMethodsTests : SimpleTestProvider {
    override init() {
        super.init()

        test("Test1", test1)
    }
}
