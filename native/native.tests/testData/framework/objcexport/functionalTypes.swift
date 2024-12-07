/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Kt

private func test1() {
    FunctionalTypesKt.callStaticType2(fct: foo2, param: "from swift")
    FunctionalTypesKt.callDynType2(list: [ foo2 ], param: "from swift")

    FunctionalTypesKt.callStaticType2(fct : {a1, _ in return a1 }, param: "from swift block")
    FunctionalTypesKt.callDynType2(list: [ {a1, _ in return a1 } ], param: "from swift block")

    // 32 params is mapped as regular; block is OK
    FunctionalTypesKt.callStaticType32(fct : {
        a1, _, _, _, _, _, _, _,
         _, _, _, _, _, _, _, _,
         _, _, _, _, _, _, _, _,
         _, _, _, _, _, _, _, _
        in return a1 }, param: "from swift block")

    FunctionalTypesKt.callDynType32(list : [{
        a1, _, _, _, _, _, _, _,
         _, _, _, _, _, _, _, _,
         _, _, _, _, _, _, _, _,
         _, _, _, _, _, _, _, _
        in return a1 }], param: "from swift block")

    // 33 params requires explicit implementation of KotlinFunction33
    FunctionalTypesKt.callStaticType33(fct: foo33, param: "from swift")
    FunctionalTypesKt.callDynType33(list: [ Foo33() ], param: "from swift")
}

private func test2() throws {
    try assertEquals(actual: FunctionalTypesKt.getDynTypeLambda2().value("one", nil) as? String, expected: "one")
    try assertEquals(actual: FunctionalTypesKt.getStaticLambda2()("two", nil) as? String, expected: "two")

    try assertEquals(actual: FunctionalTypesKt.getDynTypeRef2().value("three", nil) as? String, expected: "three")
    try assertEquals(actual: FunctionalTypesKt.getStaticRef2()("four", nil) as? String, expected: "four")

    // 32 params is mapped as regular; calling result as block is OK
    try assertEquals(
        actual: FunctionalTypesKt.getDynType32().value(
            "five",
            nil, nil, nil, nil, nil, nil, nil, nil,
            nil, nil, nil, nil, nil, nil, nil, nil,
            nil, nil, nil, nil, nil, nil, nil, nil,
            nil, nil, nil, nil, nil, nil, nil
        ) as? String,
        expected: "five"
    )

    try assertEquals(
        actual: FunctionalTypesKt.getStaticType32()(
            "six",
            nil, nil, nil, nil, nil, nil, nil, nil,
            nil, nil, nil, nil, nil, nil, nil, nil,
            nil, nil, nil, nil, nil, nil, nil, nil,
            nil, nil, nil, nil, nil, nil, nil
        ) as? String,
        expected: "six"
    )

    // 33 params requires explicit invocation of KotlinFunction33.invoke
    try assertEquals(
        actual: FunctionalTypesKt.getDynTypeRef33().value.invoke(
            p1: "seven",
            p2: nil, p3: nil, p4: nil, p5: nil, p6: nil, p7: nil, p8: nil, p9: nil,
            p10: nil, p11: nil, p12: nil, p13: nil, p14: nil, p15: nil, p16: nil, p17: nil,
            p18: nil, p19: nil, p20: nil, p21: nil, p22: nil, p23: nil, p24: nil, p25: nil,
            p26: nil, p27: nil, p28: nil, p29: nil, p30: nil, p31: nil, p32: nil, p33: nil
        ) as? String,
        expected: "seven"
    )

    // static conversion is ok though.
    try assertEquals(
        actual: FunctionalTypesKt.getStaticTypeRef33()(
            "eight",
            nil, nil, nil, nil, nil, nil, nil, nil,
            nil, nil, nil, nil, nil, nil, nil, nil,
            nil, nil, nil, nil, nil, nil, nil, nil,
            nil, nil, nil, nil, nil, nil, nil, nil
        ) as? String,
        expected: "eight"
    )

    try assertEquals(
        actual: FunctionalTypesKt.getDynTypeLambda33().value.invoke(
            p1: "nine",
            p2: nil, p3: nil, p4: nil, p5: nil, p6: nil, p7: nil, p8: nil, p9: nil,
            p10: nil, p11: nil, p12: nil, p13: nil, p14: nil, p15: nil, p16: nil, p17: nil,
            p18: nil, p19: nil, p20: nil, p21: nil, p22: nil, p23: nil, p24: nil, p25: nil,
            p26: nil, p27: nil, p28: nil, p29: nil, p30: nil, p31: nil, p32: nil, p33: nil
        ) as? String,
        expected: "nine"
    )

    try assertEquals(
        actual: FunctionalTypesKt.getStaticTypeLambda33()(
            "ten",
            nil, nil, nil, nil, nil, nil, nil, nil,
            nil, nil, nil, nil, nil, nil, nil, nil,
            nil, nil, nil, nil, nil, nil, nil, nil,
            nil, nil, nil, nil, nil, nil, nil, nil
        ) as? String,
        expected: "ten"
    )
}

class FunctionalTypesTests : SimpleTestProvider {
    override init() {
        super.init()

        test("Test1", test1)
        test("Test2", test2)
    }
}

private func foo2(a1: Any?, _: Any?) -> Any? {
    return a1
}

private func foo33(a1: Any?, _: Any?, _: Any?, _: Any?, _: Any?, _: Any?, _: Any?, _: Any?,
        _: Any?, _: Any?, _: Any?, _: Any?, _: Any?, _: Any?, _: Any?, _: Any?,
        _: Any?, _: Any?, _: Any?, _: Any?, _: Any?, _: Any?, _: Any?, _: Any?,
        _: Any?, _: Any?, _: Any?, _: Any?, _: Any?, _: Any?, _: Any?, _: Any?, _: Any?
) -> Any? {
    return a1
}

private class Foo33 : KotlinFunction33 {
    func invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, p6: Any?, p7: Any?, p8: Any?, p9: Any?,
    p10: Any?, p11: Any?, p12: Any?, p13: Any?, p14: Any?, p15: Any?, p16: Any?, p17: Any?, p18: Any?, p19: Any?,
    p20: Any?, p21: Any?, p22: Any?, p23: Any?, p24: Any?, p25: Any?, p26: Any?, p27: Any?, p28: Any?, p29: Any?,
    p30: Any?, p31: Any?, p32: Any?, p33: Any?
    ) -> Any? {
        return foo33(a1: p1
                   , nil, nil, nil, nil, nil, nil, nil, nil
                   , nil, nil, nil, nil, nil, nil, nil, nil
                   , nil, nil, nil, nil, nil, nil, nil, nil
                   , nil, nil, nil, nil, nil, nil, nil, nil)
    }
}
