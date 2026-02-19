/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import Foundation
import ValuesGenerics

// -------- Tests --------

func testVararg() throws {
    let ktArray = KotlinArray<KotlinInt>(size: 3, init: { (_) -> KotlinInt in return KotlinInt(int:42) })
    let arr: [Int] = ValuesKt.varargToList(args: ktArray as! KotlinArray<AnyObject>) as! [Int]
    try assertEquals(actual: arr, expected: [42, 42, 42])
}

func testDataClass() throws {
    let f = "1" as NSString
    let s = "2" as NSString
    let t = "3" as NSString

    let tripleVal = TripleVals<NSString>(first: f, second: s, third: t)
    try assertEquals(actual: tripleVal.first, expected: f, "Data class' value")
    try assertEquals(actual: tripleVal.first, expected: "1", "Data class' value literal")
    print(tripleVal)
    try assertEquals(actual: String(describing: tripleVal), expected: "TripleVals(first=\(f), second=\(s), third=\(t))")

    let tripleVar = TripleVars<NSString>(first: f, second: s, third: t)
    try assertEquals(actual: tripleVar.first, expected: f, "Data class' value")
    print(tripleVar)
    try assertEquals(actual: String(describing: tripleVar), expected: "[\(f), \(s), \(t)]")

    tripleVar.first = t
    tripleVar.second = f
    tripleVar.third = s
    try assertEquals(actual: String(describing: tripleVar), expected: "[\(t), \(f), \(s)]")
}

func testInlineClasses() throws {
    let ic1: Int32 = 42
    let ic1N = ValuesKt.box(ic1: 17)
    let ic2 = "foo"
    let ic2N = "bar"
    let ic3 = TripleVals<AnyObject>(first: KotlinInt(int:1), second: KotlinInt(int:2), third: KotlinInt(int:3))
    let ic3N = ValuesKt.box(ic3: nil)

    try assertEquals(
        actual: ValuesKt.concatenateInlineClassValues(ic1: ic1, ic1N: ic1N, ic2: ic2, ic2N: ic2N, ic3: ic3, ic3N: ic3N),
        expected: "42 17 foo bar TripleVals(first=1, second=2, third=3) null"
    )

    try assertEquals(
        actual: ValuesKt.concatenateInlineClassValues(ic1: ic1, ic1N: nil, ic2: ic2, ic2N: nil, ic3: nil, ic3N: nil),
        expected: "42 null foo null null null"
    )

    try assertEquals(actual: ValuesKt.getValue1(ic1), expected: 42)
    try assertEquals(actual: ValuesKt.getValueOrNull1(ic1N) as! Int, expected: 17)

    try assertEquals(actual: ValuesKt.getValue2(ic2), expected: "foo")
    try assertEquals(actual: ValuesKt.getValueOrNull2(ic2N), expected: "bar")

    try assertEquals(actual: ValuesKt.getValue3(ic3), expected: ic3)
    try assertEquals(actual: ValuesKt.getValueOrNull3(ic3N), expected: nil)
}

func testGeneric() throws {
    let a = SomeGeneric<SomeData>(t: SomeData(num: 52))
    let asd : SomeData = a.myVal()!
    try assertEquals(actual: asd.num, expected: 52)

    let nulls = GenOpen<SomeData>(arg: SomeData(num: 62))
    let nullssd : SomeData = nulls.arg!
    try assertEquals(actual: nullssd.num, expected: 62)

    let isnull = GenOpen<SomeData>(arg: nil)
    try assertEquals(actual: isnull.arg, expected: nil)

    let nonnulls = GenNonNull<SomeData>(arg: SomeData(num: 72))
    let nonnullssd : SomeData = nonnulls.arg
    try assertEquals(actual: nonnullssd.num, expected: 72)
    try assertEquals(actual: (Values_genericsKt.starGeneric(arg: nonnulls as! GenNonNull<AnyObject>) as! SomeData).num, expected: 72)

    let sd = SomeData(num: 33)
    let nullColl = GenCollectionsNull<SomeData>(arg: sd, coll: [sd])
    let nonNullColl = GenCollectionsNonNull<SomeData>(arg: sd, coll: [sd])

    try assertEquals(actual: (nullColl.coll[0] as! SomeData).num, expected: 33)
    let nonNullCollSd : SomeData = nonNullColl.coll[0]
    try assertEquals(actual: nonNullCollSd.num, expected: 33)
    try assertEquals(actual: nonNullColl.arg, expected: nonNullCollSd)

    let mixed = GenNullability<SomeData>(arg: sd, nArg: sd)
    try assertEquals(actual: mixed.asNullable()?.num, expected: 33)
    try assertEquals(actual: mixed.pAsNullable?.num, expected: 33)
    let mixedSd : SomeData? = mixed.pAsNullable
    try assertEquals(actual: mixedSd, expected: mixed.nArg)
}

// Swift ignores the variance and lets you force-cast to whatever you need, for better or worse.
// This would *not* work with direct Swift interop.
func testGenericVariance() throws {
    let sd = SomeData(num: 22)

    let variOut = GenVarOut<SomeData>(arg: sd)
    let variOutAny : GenVarOut<BaseData> = variOut as! GenVarOut<BaseData>
    let variOutOther : GenVarOut<SomeOtherData> = variOut as! GenVarOut<SomeOtherData>

    let variOutCheck = "variOut: \(variOut.arg.asString()), variOutAny: \(variOutAny.arg.asString()), variOutOther: \(variOutOther.arg.asString())"
    try assertEquals(actual: variOutCheck, expected: "variOut: 22, variOutAny: 22, variOutOther: 22")

    let variIn = GenVarIn<SomeData>(tArg: sd)
    let variInAny : GenVarIn<BaseData> = variIn as! GenVarIn<BaseData>
    let variInOther : GenVarIn<SomeOtherData> = variIn as! GenVarIn<SomeOtherData>

    let varInCheck = "variIn: \(variIn.valString()), variInAny: \(variInAny.valString()), variInOther: \(variInOther.valString())"
    try assertEquals(actual: varInCheck, expected: "variIn: SomeData(num=22), variInAny: SomeData(num=22), variInOther: SomeData(num=22)")

    let variCoType:GenVarOut<BaseData> = Values_genericsKt.variCoType()
    try assertEquals(actual: "890", expected: variCoType.arg.asString())

    let variContraType:GenVarIn<SomeData> = Values_genericsKt.variContraType()
    try assertEquals(actual: "SomeData(num=1890)", expected: variContraType.valString())
}

// Swift should completely ignore this, as should objc. Really verifying that the header generator
// deals with this
func testGenericUseSiteVariance() throws {
    let sd = SomeData(num: 22)

    let varUse = GenVarUse<BaseData>(arg: sd)
    let varUseArg = GenVarUse<BaseData>(arg: sd)

    varUse.varUse(a: varUseArg, b: GenVarUse<SomeData>(arg: sd) as! GenVarUse<BaseData>)
}

func testGenericInterface() throws {
    let a: NoGeneric = SomeGeneric<SomeData>(t: SomeData(num: 52))
    try assertEquals(actual: (a.myVal() as! SomeData).num, expected: 52)
}

func testGenericInheritance() throws {
    let ge = GenEx<SomeData, SomeOtherData>(myT:SomeOtherData(str:"Hello"), baseT:SomeData(num: 11))
    let geT : SomeData = ge.t
    try assertEquals(actual: geT.num, expected: 11)
    let gemyT : SomeOtherData = ge.myT
    try assertEquals(actual: gemyT.str, expected: "Hello")
    let geBase = ge as GenBase<SomeData>
    let geBaseT : SomeData = geBase.t
    try assertEquals(actual: geBaseT.num, expected: 11)

    //Similar to above but param names don't match and will dupe property definitions on child class
    //Functional, but should be fixed
    let ge2 = GenEx2<SomeData, SomeOtherData>(myT:SomeOtherData(str:"Hello2"), baseT:SomeData(num: 22))
    let ge2Val : SomeData = ge2.t
    let ge2SODVal : SomeOtherData = ge2.myT
    let ge2base : GenBase<SomeData> = ge2 as GenBase<SomeData>
    let ge2BaseVal : SomeData = ge2base.t
    try assertEquals(actual: ge2Val, expected: ge2BaseVal)

    let geAny = GenExAny<SomeData, SomeOtherData>(myT:SomeOtherData(str:"Hello"), baseT:SomeData(num: 131))
    try assertEquals(actual: (geAny.t as! SomeData).num, expected: 131)
    let geBaseAny = geAny as! GenBase<SomeData>
    let geBaseAnyT : SomeData = geBaseAny.t
    try assertEquals(actual: geBaseAnyT.num, expected: 131)
}

func testGenericInnerClass() throws {

    let nestedClass = GenOuterGenNested<SomeData>(b: SomeData(num: 543))
    let nestedClassB : SomeData = nestedClass.b
    try assertEquals(actual: nestedClassB.num, expected: 543)

    let innerClass = GenOuterGenInner<SomeData, SomeOtherData>(GenOuter<SomeOtherData>(a: SomeOtherData(str: "ggg")), c: SomeData(num: 66), aInner: SomeOtherData(str: "ttt"))
    let innerClassC : SomeData = innerClass.c
    try assertEquals(actual: innerClassC.num, expected: 66)
    let outerFun : SomeOtherData = innerClass.outerFun()
    let outerVal : SomeOtherData = innerClass.outerVal
    try assertEquals(actual: outerFun, expected: outerVal)
    try assertEquals(actual: outerFun.str, expected: "ggg")

    Values_genericsKt.genInnerFunc(obj: innerClass)
    Values_genericsKt.genInnerFuncAny(obj: innerClass as! GenOuterGenInner<AnyObject, AnyObject>)

    let innerReturned : GenOuterGenInner<SomeOtherData, SomeData> = Values_genericsKt.genInnerCreate()
    let innerReturnedInner : SomeOtherData = innerReturned.c
    try assertEquals(actual: innerReturnedInner.str, expected: "ppp")

    let nestedClassSame = GenOuterSameGenNestedSame<SomeData>(a: SomeData(num: 545))
    let nestedClassSameA : SomeData = nestedClassSame.a
    try assertEquals(actual: nestedClassSameA.num, expected: 545)

    let nested = GenOuterSameNestedNoGeneric()

    let innerClassSame = GenOuterSameGenInnerSame<SomeOtherData, SomeData>(GenOuterSame<SomeData>(a: SomeData(num: 44)), a: SomeOtherData(str: "rrr"))
    let innerClassSameA : SomeOtherData = innerClassSame.a
    try assertEquals(actual: innerClassSame.a.str, expected: "rrr")

    let gob : GenOuterBlankGenInner<SomeOtherData> = GenOuterBlankGenInner<SomeOtherData>(GenOuterBlank(sd: SomeData(num: 321)), arg: SomeOtherData(str: "aaa"))
    let gob2 : GenOuterBlank2GenInner<SomeOtherData> = GenOuterBlank2GenInner<SomeOtherData>(GenOuterBlank2(oarg: SomeOtherData(str: "ooo")), arg: SomeOtherData(str: "bbb"))

    let gobsod : SomeOtherData = gob.arg!
    try assertEquals(actual: gobsod.str, expected: "aaa")

    let gob2arg : SomeOtherData = gob2.arg!
    let gob2out : SomeOtherData = gob2.fromOuter()!

    try assertEquals(actual: gob2arg.str, expected: "bbb")
    try assertEquals(actual: gob2out.str, expected: "ooo")

    let inarg = GenOuterDeepGenShallowInner<SomeOtherData>(GenOuterDeep<SomeOtherData>(oarg: SomeOtherData(str: "fff")))
    let godeep : GenOuterDeepGenShallowInnerGenDeepInner<SomeOtherData> = GenOuterDeepGenShallowInnerGenDeepInner<SomeOtherData>(inarg)
    let deepval : SomeOtherData = godeep.o()!
    try assertEquals(actual: deepval.str, expected: "fff")

    let deep2 = GenOuterDeep2()
    let deep2Before = GenOuterDeep2.Before(deep2)
    let deep2After = GenOuterDeep2.After(deep2)
    let deep2soi = GenOuterDeep2.GenShallowOuterInner(deep2)
    let deep2si = GenOuterDeep2GenShallowOuterInnerGenShallowInner<SomeData>(deep2soi)
    let deep2i = GenOuterDeep2GenShallowOuterInnerGenShallowInnerGenDeepInner<SomeData>(deep2si)

    let gbb : GenBothBlank.GenInner = GenBothBlank.GenInner(GenBothBlank(a: SomeData(num: 22)), b: SomeOtherData(str: "ttt"))
    try assertEquals(actual: gbb.b.str, expected: "ttt")
}

func testGenericClashing() throws {
    let gcId = GenClashId<SomeData, SomeOtherData>(arg: SomeData(num: 22), arg2: SomeOtherData(str: "lll"))
    try assertEquals(actual: gcId.x() as! NSString, expected: "Foo")
    let gcIdArg : SomeData = gcId.arg
    try assertEquals(actual: gcIdArg.num, expected: 22)
    let gcIdArg2 : SomeOtherData = gcId.arg2
    try assertEquals(actual: gcIdArg2.str, expected: "lll")

    let gcClass = GenClashClass<SomeData, SomeOtherData, NSString>(arg: SomeData(num: 432), arg2: SomeOtherData(str: "lll"), arg3: "Bar")
    try assertEquals(actual: gcClass.int(), expected: 55)
    try assertEquals(actual: gcClass.sd().num, expected: 88)
    try assertEquals(actual: gcClass.list()[1].num, expected: 22)
    try assertEquals(actual: gcClass.arg.num, expected: 432)
    try assertEquals(actual: gcClass.clash().str, expected: "aaa")
    try assertEquals(actual: gcClass.arg2.str, expected: "lll")
    try assertEquals(actual: gcClass.arg3, expected: "Bar")

    //GenClashNames uses type parameter names that force the Objc class name itself to be mangled. Swift keeps names however
    let clashNames = GenClashNames<SomeData, SomeData, SomeData, SomeData>()
    try assertEquals(actual: clashNames.foo().str, expected: "nnn")
    try assertEquals(actual: clashNames.bar().str, expected: "qqq")
    try assertTrue(clashNames.baz(arg: ClashnameParam(str: "meh")), "ClashnameParam issue")

    let clashNamesEx = GenClashEx<SomeData>()

    let geClash = GenExClash<SomeOtherData>(myT:SomeOtherData(str:"Hello"))
    try assertEquals(actual: geClash.t.num, expected: 55)
    try assertEquals(actual: geClash.myT.str, expected: "Hello")
}

func testGenericExtensions() throws {
    let gnn = GenNonNull<SomeData>(arg: SomeData(num: 432))
    try assertEquals(actual: (gnn.foo() as! SomeData).num, expected: 432)
}

// -------- Execution of the test --------

class Values_genericsTests : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "TestVararg", method: withAutorelease(testVararg)),
            TestCase(name: "TestDataClass", method: withAutorelease(testDataClass)),
            TestCase(name: "TestInlineClasses", method: withAutorelease(testInlineClasses)),
            TestCase(name: "TestGeneric", method: withAutorelease(testGeneric)),
            TestCase(name: "TestGenericVariance", method: withAutorelease(testGenericVariance)),
            TestCase(name: "TestGenericUseSiteVariance", method: withAutorelease(testGenericUseSiteVariance)),
            TestCase(name: "TestGenericInheritance", method: withAutorelease(testGenericInheritance)),
            TestCase(name: "TestGenericInterface", method: withAutorelease(testGenericInterface)),
            TestCase(name: "TestGenericInnerClass", method: withAutorelease(testGenericInnerClass)),
            TestCase(name: "TestGenericClashing", method: withAutorelease(testGenericClashing)),
            TestCase(name: "TestGenericExtensions", method: withAutorelease(testGenericExtensions)),
        ]
    }
}
