/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import Kt

class FooImpl : Foo {
    func getCkt57791() -> Ckt57791Final { return Ckt57791Final() }
}

func testKt57791() throws {
    try assertTrue(Kt57791Kt.foobar(f: false, foo: FooImpl()))
}

class Kt57791Tests : SimpleTestProvider {
    override init() {
        super.init()

        test("testKt57791", testKt57791)
    }
}
