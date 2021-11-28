/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Foundation
import Kt

// -------- Tests --------

func testInstantiation() {
#if NO_GENERICS
    Invariant()
    OutVariant()
    InVariant()
#else
    Invariant<VarianceTests>()
    OutVariant<VarianceTests>()
    InVariant<VarianceTests>()
#endif
}

// -------- Execution of the test --------

class VarianceTests : SimpleTestProvider {
    override init() {
        super.init()

        test("TestInstantiation", testInstantiation)
    }
}