/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Kt

private func testPropertyWithPrivateSetter() throws {
    try assertEquals(actual: KT43599().memberProperty, expected: "memberProperty")
    try assertEquals(actual: KT43599().extensionProperty, expected: "extensionProperty")
    try assertEquals(actual: Kt43599Kt.topLevelProperty, expected: "topLevelProperty")

    // Checking the reported case too:
    Kt43599Kt.setTopLevelLateinitProperty(value: "topLevelLateinitProperty")
    try assertEquals(actual: Kt43599Kt.topLevelLateinitProperty, expected: "topLevelLateinitProperty")
}

class Kt43599Tests : SimpleTestProvider {
    override init() {
        super.init()

        test("TestPropertyWithPrivateSetter", testPropertyWithPrivateSetter)
    }
}
