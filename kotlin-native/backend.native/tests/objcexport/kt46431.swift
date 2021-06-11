/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Kt

// Based on https://youtrack.jetbrains.com/issue/KT-46431.

private func test1() throws {
    try assertEquals(actual: Kt46431Kt.createAbstractHost().test, expected: "1234")
}

class Kt46431Tests : SimpleTestProvider {
    override init() {
        super.init()

        test("Test1", test1)
    }
}
