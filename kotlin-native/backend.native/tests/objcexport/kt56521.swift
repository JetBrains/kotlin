/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import Kt

func testKt56521() throws {
    let object = Kt56521()
    try assertTrue(object is Kt56521)
    try assertEquals(actual: Kt56521Kt.initialized, expected: 1)
}

class Kt56521Tests : SimpleTestProvider {
    override init() {
        super.init()

        test("testKt56521", testKt56521)
    }
}
