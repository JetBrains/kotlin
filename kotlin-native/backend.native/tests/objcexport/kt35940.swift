/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Kt

private func test1() throws {
    try assertEquals(actual: Kt35940Kt.testKt35940(), expected: "zzz")
}

class Kt35940Tests : SimpleTestProvider {
    override init() {
        super.init()

        test("Test1", test1)
    }
}