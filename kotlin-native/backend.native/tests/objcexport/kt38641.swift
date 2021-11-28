/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Kt

private func testIntType() throws {
    let i = KT38641.IntType()

    try assertEquals(actual: i.description_, expected: 42)

    i.description_ = 17
    try assertEquals(actual: i.description_, expected: 17)
}

private func testVal() throws {
    try assertEquals(actual: KT38641.Val().description_, expected: "val")
}

private func testVar() throws {
    let v = KT38641.Var()

    try assertEquals(actual: v.description_, expected: "var")

    v.description_ = "newValue"
    try assertEquals(actual: v.description_, expected: "newValue")
}

private func testTwoProperties() throws {
    let t = KT38641.TwoProperties()
    try assertEquals(actual: t.description_, expected: "description")
    try assertEquals(actual: t.description__, expected: "description_")
}

private func testOverrideVal() throws {
    try assertEquals(actual: Kt38641Kt.getOverrideValDescription(impl: KT38641OverrideValImpl()), expected: "description_")
}

class KT38641OverrideValImpl : KT38641.OverrideVal {
    override var description_: String {
        get {
            return "description_"
        }
    }
}

private func testOverrideVar() throws {
    let impl = KT38641OverrideVarImpl()

    try assertEquals(actual: Kt38641Kt.getOverrideVarDescription(impl: impl), expected: "description_")

    Kt38641Kt.setOverrideVarDescription(impl: impl, newValue: "d")
    try assertEquals(actual: Kt38641Kt.getOverrideVarDescription(impl: impl), expected: "d")
}

class KT38641OverrideVarImpl : KT38641OverrideVar {
    var description_: String = "description_"
}

class Kt38641Tests : SimpleTestProvider {
    override init() {
        super.init()

        test("TestIntType", testIntType)
        test("TestVal", testVal)
        test("TestVar", testVar)
        test("TestTwoProperties", testTwoProperties)
        test("TestOverrideVal", testOverrideVal)
        test("TestOverrideVar", testOverrideVar)
    }
}