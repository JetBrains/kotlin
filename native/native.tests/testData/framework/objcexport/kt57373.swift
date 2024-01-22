/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import Kt

func testKt57373() throws {
    let impl = CKt57373()
    let x = DKt57373(foo: impl)
    try assertEquals(actual: x.bar, expected: 42)
}

class Kt57373Tests : SimpleTestProvider {
    override init() {
        super.init()

        test("testKt57373", testKt57373)
    }
}