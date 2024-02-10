/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import Foundation
import Kt

// See https://youtrack.jetbrains.com/issue/KT-54119/Native-runtime-assertion-failed-due-to-missing-thread-state-switch

private func testSetContains() throws {
    try assertFalse(Kt54119Kt.callContains(set: ["111"]))
}

private func testSetGetElement() throws {
    try assertNil(Kt54119Kt.callGetElement(set: [222]))
}

private func testMapContainsKey() throws {
    try assertFalse(Kt54119Kt.callContainsKey(map: ["abc" : "def"]))
}

private func testMapContainsValue() throws {
    try assertFalse(Kt54119Kt.callContainsValue(map: [KT54119KotlinKey() : 1]))
}

private func testMapGet() throws {
    try assertNil(Kt54119Kt.callGet(map: [0 : 0]))
}

private func testMapGetOrThrowConcurrentModification() throws {
    Kt54119Kt.callGetOrThrowConcurrentModification(map: [KT54119KotlinKey() : 2])
}

private func testMapContainsEntry() throws {
    try assertTrue(Kt54119Kt.callContainsEntry(map: [KT54119KotlinKey() : 3]))
}

class Kt54119Tests : SimpleTestProvider {
    override init() {
        super.init()

        test("testSetContains", testSetContains)
        test("testSetGetElement", testSetGetElement)
        test("testMapContainsKey", testMapContainsKey)
        test("testMapContainsValue", testMapContainsValue)
        test("testMapGet", testMapGet)
        test("testMapGetOrThrowConcurrentModification", testMapGetOrThrowConcurrentModification)
        test("testMapContainsEntry", testMapContainsEntry)
    }
}
