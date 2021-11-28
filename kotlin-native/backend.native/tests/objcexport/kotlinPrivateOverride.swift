/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Kt

private func testI1Methods(p: Any, base: Int32) throws {
    guard let i1 = p as? KotlinPrivateOverrideI1 else { try fail() }
    try assertEquals(actual: i1.i123AbstractMethod(), expected: base + 1)
    try assertEquals(actual: i1.i1OpenMethod(), expected: base + 2)
}

private func testI2Methods(p: Any, base: Int32) throws {
    guard let i2 = p as? KotlinPrivateOverrideI2 else { try fail() }
    try assertEquals(actual: i2.i123AbstractMethod(), expected: base + 1)
    try assertEquals(actual: i2.i234AbstractMethod(), expected: base + 3)
    try assertEquals(actual: i2.i2AbstractMethod(), expected: base + 4)
}

private func testA1Methods(p: Any, base: Int32) throws {
    guard let a1 = p as? KotlinPrivateOverrideA1 else { try fail() }
    try assertEquals(actual: a1.i123AbstractMethod(), expected: base + 1)
    try assertEquals(actual: a1.i1OpenMethod(), expected: base + 2)
    try assertEquals(actual: a1.i234AbstractMethod(), expected: base + 3)
    try assertEquals(actual: a1.i2AbstractMethod(), expected: base + 4)
    try assertEquals(actual: a1.a1AbstractMethod(), expected: base + 6)
    try assertEquals(actual: a1.a1OpenMethod(), expected: base + 7)
}

private func testI3Methods(p: Any, base: Int32) throws {
    guard let i3 = p as? KotlinPrivateOverrideI3 else { try fail() }
    try assertEquals(actual: i3.i123AbstractMethod(), expected: base + 1)
    try assertEquals(actual: i3.i234AbstractMethod(), expected: base + 3)
    try assertEquals(actual: i3.i3AbstractMethod(), expected: base + 8)
}

private func testI4Methods(p: Any, base: Int32) throws {
    guard let i4 = p as? KotlinPrivateOverrideI4 else { try fail() }
    try assertEquals(actual: i4.i234AbstractMethod(), expected: base + 3)
    try assertEquals(actual: i4.i4AbstractMethod(), expected: base + 10)
}

private func test(p: Any, base: Int32, isI4: Bool) throws {
    try testI1Methods(p: p, base: base)
    try testI2Methods(p: p, base: base)
    try testA1Methods(p: p, base: base)
    try testI3Methods(p: p, base: base)
    if isI4 {
        try testI4Methods(p: p, base: base)
    }
}

private func test1() throws {
    try test(p: KotlinPrivateOverrideKt.createP1(), base: 0, isI4: false)
    try test(p: KotlinPrivateOverrideKt.createP12(), base: 10, isI4: true)
}


class KotlinPrivateOverrideTests : SimpleTestProvider {
    override init() {
        super.init()

        test("Test1", test1)
    }
}
