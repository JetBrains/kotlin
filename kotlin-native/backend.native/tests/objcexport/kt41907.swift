/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Kt

class Ikt41907Impl : Ikt41907 {
    func foo(c: Ckt41907) {
        Kt41907Kt.escapeC(c: c)
    }
}

private func test1() {
    Kt41907Kt.testKt41907(o: Ikt41907Impl())
}

class Kt41907Tests : SimpleTestProvider {
    override init() {
        super.init()

        test("Test1", test1)
    }
}