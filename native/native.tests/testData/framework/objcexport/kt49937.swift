/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Kt

// Based on https://youtrack.jetbrains.com/issue/KT-49937.

public extension KT49937 {
    override var description: String { "KT49937Swift" }
}

private func test1() throws {
    try assertEquals(actual: KT49937().description, expected: "KT49937Swift")

    let nsObject: NSObject = KT49937()
    try assertEquals(actual: nsObject.description, expected: "KT49937Swift")
}

class Kt49937Tests : SimpleTestProvider {
    override init() {
        super.init()

        test("Test1", test1)
    }
}
