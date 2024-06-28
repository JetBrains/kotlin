/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import Foundation
import Kt

private func testFill() throws {
    let result = Kt55736Kt.callback(block: Kt55736Kt.getFillFunction())
    try assertEquals(actual: result.count, expected: 2)
    try assertEquals(actual: result[0], expected: 1)
    try assertEquals(actual: result[1], expected: 2)
}

class Kt55736Tests : SimpleTestProvider {
    override init() {
        super.init()

        test("testFill", testFill)
    }
}
