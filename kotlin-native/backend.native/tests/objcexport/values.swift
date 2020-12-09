/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Foundation
import Kt

// -------- Tests --------

func testVals() throws {
    print("Values from Swift")
    let dbl = ValuesKt.dbl
    let flt = ValuesKt.flt
    let int = ValuesKt.integer
    let long = ValuesKt.longInt
    
    print(dbl)
    print(flt)
    print(int)
    print(long)
    
    try assertEquals(actual: dbl, expected: 3.14 as Double, "Double value isn't equal.")
    try assertEquals(actual: flt, expected: 2.73 as Float, "Float value isn't equal.")
    try assertEquals(actual: int, expected: 42)
    try assertEquals(actual: long, expected: 1984)
}

func testVars() throws {
    print("Variables from Swift")
    var intVar = ValuesKt.intVar
    var strVar = ValuesKt.str
    var strAsId = ValuesKt.strAsAny
    
    print(intVar)
    print(strVar)
    print(strAsId)
    
    try assertEquals(actual: intVar, expected: 451)
    try assertEquals(actual: strVar, expected: "Kotlin String")
    try assertEquals(actual: strAsId as! String, expected: "Kotlin String as Any")
    
    strAsId = "Swift str"
    ValuesKt.strAsAny = strAsId
    print(ValuesKt.strAsAny)
    try assertEquals(actual: ValuesKt.strAsAny as! String, expected: strAsId as! String)
    
    // property with custom getter/setter backed by the Kotlin's var
    var intProp : Int32 {
        get {
            return ValuesKt.intVar * 2
        }
        set(value) {
            ValuesKt.intVar = 123 + value
        }
    }
    intProp += 10   
    print(intProp)
    print(ValuesKt.intVar)
    try assertEquals(actual: ValuesKt.intVar * 2, expected: intProp, "Property backed by var")
}

func testDoubles() throws {
    print("Doubles in Swift")
    let minDouble = ValuesKt.minDoubleVal as! Double
    let maxDouble = ValuesKt.maxDoubleVal as! NSNumber

    print(minDouble)
    print(maxDouble)
    print(ValuesKt.nanDoubleVal)
    print(ValuesKt.nanFloatVal)
    print(ValuesKt.infDoubleVal)
    print(ValuesKt.infFloatVal)

    try assertEquals(actual: minDouble, expected: Double.leastNonzeroMagnitude, "Min double")
    try assertEquals(actual: maxDouble, expected: Double.greatestFiniteMagnitude as NSNumber, "Max double")
    try assertTrue(ValuesKt.nanDoubleVal.isNaN, "NaN double")
    try assertTrue(ValuesKt.nanFloatVal.isNaN, "NaN float")
    try assertEquals(actual: ValuesKt.infDoubleVal, expected: Double.infinity, "Inf double")
    try assertEquals(actual: ValuesKt.infFloatVal, expected: -Float.infinity, "-Inf float")
}

func testNumbers() throws {
    try assertEquals(actual: KotlinBoolean(value: true).boolValue, expected: true)
    try assertEquals(actual: KotlinBoolean(value: false).intValue, expected: 0)
    try assertEquals(actual: KotlinBoolean(value: true), expected: true)
    try assertFalse(KotlinBoolean(value: false) as! Bool)

    try assertEquals(actual: KotlinByte(value: -1).int8Value, expected: -1)
    try assertEquals(actual: KotlinByte(value: -1).int32Value, expected: -1)
    try assertEquals(actual: KotlinByte(value: -1).doubleValue, expected: -1.0)
    try assertEquals(actual: KotlinByte(value: -1), expected: NSNumber(value: Int64(-1)))
    try assertFalse(KotlinByte(value: -1) == NSNumber(value: -1.5))
    try assertEquals(actual: KotlinByte(value: -1), expected: -1)
    try assertTrue(KotlinByte(value: -1) == -1)
    try assertFalse(KotlinByte(value: -1) == 1)
    try assertEquals(actual: KotlinByte(value: -1) as! Int32, expected: -1)

    try assertEquals(actual: KotlinShort(value: 111).int16Value, expected: 111)
    try assertEquals(actual: KotlinShort(value: -15) as! Int16, expected: -15)
    try assertEquals(actual: KotlinShort(value: 47), expected: 47)

    try assertEquals(actual: KotlinInt(value: 99).int32Value, expected: 99)
    try assertEquals(actual: KotlinInt(value: -1) as! Int32, expected: -1)
    try assertEquals(actual: KotlinInt(value: 72), expected: 72)

    try assertEquals(actual: KotlinLong(value: 65).int64Value, expected: 65)
    try assertEquals(actual: KotlinLong(value: 10000000000) as! Int64, expected: 10000000000)
    try assertEquals(actual: KotlinLong(value: 8), expected: 8)

    try assertEquals(actual: KotlinUByte(value: 17).uint8Value, expected: 17)
    try assertEquals(actual: KotlinUByte(value: 42) as! UInt8, expected: 42)
    try assertEquals(actual: 88, expected: KotlinUByte(value: 88))

    try assertEquals(actual: KotlinUShort(value: 40000).uint16Value, expected: 40000)
    try assertEquals(actual: KotlinUShort(value: 1) as! UInt16, expected: UInt16(1))
    try assertEquals(actual: KotlinUShort(value: 65000), expected: 65000)

    try assertEquals(actual: KotlinUInt(value: 3).uint32Value, expected: 3)
    try assertEquals(actual: KotlinUInt(value: UInt32.max) as! UInt32, expected: UInt32.max)
    try assertEquals(actual: KotlinUInt(value: 2), expected: 2)

    try assertEquals(actual: KotlinULong(value: 55).uint64Value, expected: 55)
    try assertEquals(actual: KotlinULong(value: 0) as! UInt64, expected: 0)
    try assertEquals(actual: KotlinULong(value: 7), expected: 7)

    try assertEquals(actual: KotlinFloat(value: 1.0).floatValue, expected: 1.0)
    try assertEquals(actual: KotlinFloat(value: 22.0) as! Float, expected: 22)
    try assertEquals(actual: KotlinFloat(value: 41.0), expected: 41)
    try assertEquals(actual: KotlinFloat(value: -5.5), expected: -5.5)

    try assertEquals(actual: KotlinDouble(value: 0.5).doubleValue, expected: 0.5)
    try assertEquals(actual: KotlinDouble(value: 45.0) as! Double, expected: 45)
    try assertEquals(actual: KotlinDouble(value: 89.0), expected: 89)
    try assertEquals(actual: KotlinDouble(value: -3.7), expected: -3.7)

    ValuesKt.ensureEqualBooleans(actual: KotlinBoolean(value: true), expected: true)
    ValuesKt.ensureEqualBooleans(actual: false, expected: false)

    ValuesKt.ensureEqualBytes(actual: KotlinByte(value: 42), expected: 42)
    ValuesKt.ensureEqualBytes(actual: -11, expected: -11)

    ValuesKt.ensureEqualShorts(actual: KotlinShort(value: 256), expected: 256)
    ValuesKt.ensureEqualShorts(actual: -1, expected: -1)

    ValuesKt.ensureEqualInts(actual: KotlinInt(value: 100000), expected: 100000)
    ValuesKt.ensureEqualInts(actual: -7, expected: -7)

    ValuesKt.ensureEqualLongs(actual: KotlinLong(value: Int64.max), expected: Int64.max)
    ValuesKt.ensureEqualLongs(actual: 17, expected: 17)

    ValuesKt.ensureEqualUBytes(actual: KotlinUByte(value: 6), expected: 6)
    ValuesKt.ensureEqualUBytes(actual: 255, expected: 255)

    ValuesKt.ensureEqualUShorts(actual: KotlinUShort(value: 300), expected: 300)
    ValuesKt.ensureEqualUShorts(actual: 65535, expected: UInt16.max)

    ValuesKt.ensureEqualUInts(actual: KotlinUInt(value: 70000), expected: 70000)
    ValuesKt.ensureEqualUInts(actual: 48, expected: 48)

    ValuesKt.ensureEqualULongs(actual: KotlinULong(value: UInt64.max), expected: UInt64.max)
    ValuesKt.ensureEqualULongs(actual: 39, expected: 39)

    ValuesKt.ensureEqualFloats(actual: KotlinFloat(value: 36.6), expected: 36.6)
    ValuesKt.ensureEqualFloats(actual: 49.5, expected: 49.5)
    ValuesKt.ensureEqualFloats(actual: 18, expected: 18.0)

    ValuesKt.ensureEqualDoubles(actual: KotlinDouble(value: 12.34), expected: 12.34)
    ValuesKt.ensureEqualDoubles(actual: 56.78, expected: 56.78)
    ValuesKt.ensureEqualDoubles(actual: 3, expected: 3)

    func checkBox<T: Equatable, B : NSObject>(_ value: T, _ boxFunction: (T) -> B?) throws {
        let box = boxFunction(value)!
        try assertEquals(actual: box as! T, expected: value)
        print(type(of: box))
        print(B.self)
        try assertTrue(box.isKind(of: B.self))
    }

    try checkBox(true, ValuesKt.box)
    try checkBox(Int8(-1), ValuesKt.box)
    try checkBox(Int16(-2), ValuesKt.box)
    try checkBox(Int32(-3), ValuesKt.box)
    try checkBox(Int64(-4), ValuesKt.box)
    try checkBox(UInt8(5), ValuesKt.box)
    try checkBox(UInt16(6), ValuesKt.box)
    try checkBox(UInt32(7), ValuesKt.box)
    try checkBox(UInt64(8), ValuesKt.box)
    try checkBox(Float(8.7), ValuesKt.box)
    try checkBox(Double(9.4), ValuesKt.box)
}

func testLists() throws {
    let numbersList = ValuesKt.numbersList
    let gold = [1, 2, 13]
    for i in 0..<gold.count {
        try assertEquals(actual: gold[i], expected: Int(numbersList[i] as! NSNumber), "Numbers list")
    }

    let anyList = ValuesKt.anyList
    for i in anyList {
        print(i)
    }
//    try assertEquals(actual: gold, expected: anyList, "Numbers list")
}

func testLazyVal() throws {
    let lazyVal = ValuesKt.lazyVal
    print(lazyVal)
    try assertEquals(actual: lazyVal, expected: "Lazily initialized string", "lazy value")
}

let goldenArray = ["Delegated", "global", "array", "property"]

func testDelegatedProp() throws {
    let delegatedGlobalArray = ValuesKt.delegatedGlobalArray
    guard Int(delegatedGlobalArray.size) == goldenArray.count else {
        throw TestError.assertFailed("Size differs")
    }
    for i in 0..<delegatedGlobalArray.size {
        print(delegatedGlobalArray.get(index: i)!)
    }
}

func testGetterDelegate() throws {
    let delegatedList = ValuesKt.delegatedList
    guard delegatedList.count == goldenArray.count else {
        throw TestError.assertFailed("Size differs")
    }
    for val in delegatedList {
        print(val)
    }
}

func testNulls() throws {
    let nilVal : Any? = ValuesKt.nullVal
    try assertTrue(nilVal == nil, "Null value")

    ValuesKt.nullVar = nil
    var nilVar : Any? = ValuesKt.nullVar
    try assertTrue(nilVar == nil, "Null variable")
}

func testAnyVar() throws {
    let anyVar : Any = ValuesKt.anyValue
    print(anyVar)
    if let str = anyVar as? String {
        print(str)
        try assertEquals(actual: str, expected: "Str")
    } else {
        throw TestError.assertFailed("Incorrect type passed from Any")
    }
}

func testFunctions() throws {
    let _: Any? = ValuesKt.emptyFun()

    let str = ValuesKt.strFun()
    try assertEquals(actual: str, expected: "fooStr")

    try assertEquals(actual: ValuesKt.argsFun(i: 10, l: 20, d: 3.5, s: "res") as! String,
            expected: "res10203.5")

    try assertEquals(actual: ValuesKt.multiply(int: 3, long: 2), expected: 6)
}

class SwiftThrowing : SwiftOverridableMethodsWithThrows {
    class E : Error {}

    func unit() throws -> Void { throw E() }
    func nothing() throws -> Void { throw E() }
    func any() throws -> Any { throw E() }
    func block() throws -> () -> KotlinInt { throw E() }
}

class TestThrowingConstructorRelease : Throwing {
    static var deinitialized = false
    deinit {
        TestThrowingConstructorRelease.deinitialized = true
    }
}

class SwiftNotThrowing : SwiftOverridableMethodsWithThrows {
    func unit() throws -> Void {  }
    func nothing() throws -> Void { throw SwiftThrowing.E() }
    func any() throws -> Any { return 42 as Int32 }
    func block() throws -> () -> KotlinInt { return { 17 } }
}

class SwiftUnitCaller : MethodsWithThrowsUnitCaller {
    func call(methods: MethodsWithThrows) throws -> Void {
        try methods.unit()
    }
}

class SwiftThrowingWithBridge : ThrowsWithBridge {
    override func plusOne(x: Int32) throws -> KotlinInt {
        throw SwiftThrowing.E()
    }
}

class SwiftNotThrowingWithBridge : ThrowsWithBridge {
    override func plusOne(x: Int32) throws -> KotlinInt {
        return KotlinInt(value: x + 1)
    }
}

private func testThrowing(file: String = #file, line: Int = #line, _ block: () throws -> Void) throws {
    try assertFailsWithKotlin(MyException.self, file: file, line: line, block: block)
}

func testExceptions() throws {
    try testThrowing { try ValuesKt.throwException(error: false) }
    do {
        try ValuesKt.throwException(error: true)
    } catch let error as NSError {
        try assertTrue(error.kotlinException is MyError)
    }

    try assertFalse(TestThrowingConstructorRelease.deinitialized)
    try testThrowing { try TestThrowingConstructorRelease(doThrow: true) }
    ValuesKt.gc()
    try assertTrue(TestThrowingConstructorRelease.deinitialized)

    try testThrowing { try Throwing(doThrow: true) }

    let throwing = try Throwing(doThrow: false)
    try testThrowing { try throwing.unit() }
    try testThrowing { try throwing.nothing() }
    try testThrowing { try throwing.nothingN() }
    try testThrowing { try throwing.any() }
    try testThrowing { try throwing.anyN() }
    try testThrowing { try throwing.block()() }
    try testThrowing { try throwing.blockN() }
    try testThrowing { try throwing.pointer() }
    try testThrowing { try throwing.pointerN() }
    try testThrowing { try throwing.int() }
    try testThrowing { try throwing.longN() }
    try testThrowing { try throwing.double() }

    let notThrowing = try NotThrowing()

    try notThrowing.unit()
    try assertEquals(actual: notThrowing.nothingN(), expected: nil)
    try assertTrue(notThrowing.any() is KotlinBase)
    try assertTrue(notThrowing.anyN() is KotlinBase)
    try assertEquals(actual: notThrowing.block()(), expected: 42)
    try assertTrue(notThrowing.blockN() == nil)
    try assertEquals(actual: Int(bitPattern: notThrowing.pointer()), expected: 1)
    try assertEquals(actual: notThrowing.pointerN(), expected: nil)
    try assertEquals(actual: notThrowing.int(), expected: 42)
    try assertEquals(actual: notThrowing.longN(), expected: nil)
    try assertEquals(actual: notThrowing.double(), expected: 3.14)

    try ValuesKt.testSwiftThrowing(methods: SwiftThrowing())
    try ValuesKt.testSwiftNotThrowing(methods: SwiftNotThrowing())

    do {
        try ValuesKt.callUnit(methods: SwiftThrowing())
    } catch let e as SwiftThrowing.E {
        // Ok.
    }

    try ValuesKt.callUnitCaller(caller: SwiftUnitCaller(), methods: throwing)

    try ValuesKt.testSwiftThrowing(test: SwiftThrowingWithBridge(), flag: false)
    try ValuesKt.testSwiftThrowing(test: SwiftThrowingWithBridge(), flag: true)
    try ValuesKt.testSwiftNotThrowing(test: SwiftNotThrowingWithBridge())
}

func testFuncType() throws {
    let s = "str"
    let fFunc: () -> String = { return s }
    try assertEquals(actual: ValuesKt.funArgument(foo: fFunc), expected: s, "Using function type arguments failed")
}

func testGenericsFoo() throws {
    let fun = { (i: Int) -> String in return "S \(i)" }
    // wrap lambda to workaround issue with type conversion inability:
    // (Int) -> String can't be cast to (Any?) -> Any?
    let wrapper = { (t: Any?) -> Any? in return fun(t as! Int) }
    let res = ValuesKt.genericFoo(t: 42, foo: wrapper)
    try assertEquals(actual: res as! String, expected: "S 42")
}

func testVararg() throws {
    let ktArray = KotlinArray<AnyObject>(size: 3, init: { (_) -> NSNumber in return 42 })
    let arr: [Int] = ValuesKt.varargToList(args: ktArray) as! [Int]
    try assertEquals(actual: arr, expected: [42, 42, 42])
}

func testStrExtFun() throws {
    try assertEquals(actual: ValuesKt.subExt("String", i: 2), expected: "r")
    try assertEquals(actual: ValuesKt.subExt("A", i: 2), expected: "nothing")
}

func testAnyToString() throws {
    try assertEquals(actual: ValuesKt.toString(nil), expected: "null")
    try assertEquals(actual: ValuesKt.toString(42), expected: "42")
}

func testAnyPrint() throws {
    print("BEGIN")
    ValuesKt.print(nil)
    ValuesKt.print("Print")
    ValuesKt.print(123456789)
    ValuesKt.print(3.14)
    ValuesKt.print([3, 2, 1])
    print("END")
}

func testCharExtensions() throws {
    try assertTrue(ValuesKt.isA(ValuesKt.boxChar(65)))
    try assertFalse(ValuesKt.isA(ValuesKt.boxChar(66)))
}

func testLambda() throws {
    try assertEquals(actual: ValuesKt.sumLambda(3, 4), expected: 7)

    var blockRuns = 0

    try assertTrue(ValuesKt.runUnitBlock { blockRuns += 1 })
    try assertEquals(actual: blockRuns, expected: 1)

    let unitBlock: () -> Void = ValuesKt.asUnitBlock {
        blockRuns += 1
        return 42
    }
    try assertTrue(unitBlock() == Void())
    try assertEquals(actual: blockRuns, expected: 2)

    let nothingBlock: () -> Void = ValuesKt.asNothingBlock { blockRuns += 1 }
    try assertTrue(ValuesKt.runNothingBlock(block: nothingBlock))
    try assertEquals(actual: blockRuns, expected: 3)

    try assertTrue(ValuesKt.getNullBlock() == nil)
    try assertTrue(ValuesKt.isBlockNull(block: nil))

    // Test dynamic conversion:
    let intBlocks = IntBlocksImpl()
    try assertEquals(actual: intBlocks.getPlusOneBlock()(1), expected: 2)
    try assertEquals(actual: intBlocks.callBlock(argument: 2) { KotlinInt(value: $0.int32Value + 2) }, expected: 4)

    // Test round trip with dynamic conversion:
    let coercedUnitBlock: () -> KotlinUnit = UnitBlockCoercionImpl().coerce { blockRuns += 1 }
    try assertTrue(coercedUnitBlock() === KotlinUnit())
    try assertEquals(actual: blockRuns, expected: 4)

    let uncoercedUnitBlock: () -> Void = UnitBlockCoercionImpl().uncoerce {
        blockRuns += 1
        return KotlinUnit()
    }
    try assertTrue(uncoercedUnitBlock() == Void())
    try assertEquals(actual: blockRuns, expected: 5)

    let blockMustBeFunction0: @convention(block) () -> AnyObject? = { return nil }
    try assertTrue(ValuesKt.isFunction(obj: blockMustBeFunction0))
    try assertTrue(ValuesKt.isFunction0(obj: blockMustBeFunction0))
    try assertFalse(ValuesKt.isFunction(obj: NSObject()))
    try assertFalse(ValuesKt.isFunction0(obj: NSObject()))

    // Test no function class for dynamic conversion:
    let blockAsMissingFunction: @convention(block) (AnyObject?, AnyObject?, AnyObject?, AnyObject?, AnyObject?) -> AnyObject?
            = { return $0 ?? $1 ?? $2 ?? $3 ?? $4 }

    try assertTrue(ValuesKt.isFunction(obj: blockAsMissingFunction))
    try assertFalse(ValuesKt.isFunction0(obj: blockAsMissingFunction))
}

// -------- Tests for classes and interfaces -------
class ValIEmptyExt : I {
    func iFun() -> String {
        return "ValIEmptyExt::iFun"
    }
}

class ValIExt : I {
    func iFun() -> String {
        return "ValIExt::iFun"
    }
}

func testInterfaceExtension() throws {
    try assertEquals(actual: ValIEmptyExt().iFun(), expected: "ValIEmptyExt::iFun")
    try assertEquals(actual: ValIExt().iFun(), expected: "ValIExt::iFun")
}

func testClassInstances() throws {
    try assertEquals(actual: OpenClassI().iFun(), expected: "OpenClassI::iFun")
    try assertEquals(actual: DefaultInterfaceExt().iFun(), expected: "I::iFun")
    try assertEquals(actual: FinalClassExtOpen().iFun(), expected: "FinalClassExtOpen::iFun")
    try assertEquals(actual: MultiExtClass().iFun(), expected: "PI::iFun")
    try assertEquals(actual: MultiExtClass().piFun() as! Int, expected: 42)
    try assertEquals(actual: ConstrClass(i: 1, s: "str", a: "Any").iFun(), expected: "OpenClassI::iFun")
    try assertEquals(actual: ExtConstrClass(i: 123).iFun(), expected: "ExtConstrClass::iFun::123-String-AnyS")
}

func testEnum() throws {
    try assertEquals(actual: ValuesKt.passEnum(), expected: Enumeration.answer)
    try assertEquals(actual: ValuesKt.passEnum().enumValue, expected: 42)
    try assertEquals(actual: ValuesKt.passEnum().name, expected: "ANSWER")
    ValuesKt.receiveEnum(e: 1)
}

func testDataClass() throws {
    let f = "1"
    let s = "2"
    let t = "3"

    let tripleVal = TripleVals<NSString>(first: f as NSString, second: s as NSString, third: t as NSString)
    try assertEquals(actual: tripleVal.first as! String, expected: f, "Data class' value")
    try assertEquals(actual: tripleVal.component2() as! String, expected: s, "Data class' component")
    print(tripleVal)
    try assertEquals(actual: String(describing: tripleVal), expected: "TripleVals(first=\(f), second=\(s), third=\(t))")

    let tripleVar = TripleVars<NSString>(first: f as NSString, second: s as NSString, third: t as NSString)
    try assertEquals(actual: tripleVar.first as! String, expected: f, "Data class' value")
    try assertEquals(actual: tripleVar.component2() as! String, expected: s, "Data class' component")
    print(tripleVar)
    try assertEquals(actual: String(describing: tripleVar), expected: "[\(f), \(s), \(t)]")

    tripleVar.first = t as NSString
    tripleVar.second = f as NSString
    tripleVar.third = s as NSString
    try assertEquals(actual: tripleVar.component2() as! String, expected: f, "Data class' component")
    try assertEquals(actual: String(describing: tripleVar), expected: "[\(t), \(f), \(s)]")
}

func testCompanionObj() throws {
    try assertEquals(actual: WithCompanionAndObject.Companion().str, expected: "String")
    try assertEquals(actual: ValuesKt.getCompanionObject().str, expected: "String")

    let namedFromCompanion = ValuesKt.getCompanionObject().named
    let named = ValuesKt.getNamedObject()
    try assertTrue(named === namedFromCompanion, "Should be the same Named object")

    try assertEquals(actual: ValuesKt.getNamedObjectInterface().iFun(), expected: named.iFun(), "Named object's method")
}

func testInlineClasses() throws {
    let ic1: Int32 = 42
    let ic1N = ValuesKt.box(ic1: 17)
    let ic2 = "foo"
    let ic2N = "bar"
    let ic3 = TripleVals<NSNumber>(first: 1, second: 2, third: 3)
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

class TestSharedIImpl : NSObject, I {
    func iFun() -> String {
        return "TestSharedIImpl::iFun"
    }
}

func testShared() throws {
    func assertFrozen(_ obj: AnyObject) throws {
        try assertTrue(ValuesKt.isFrozen(obj: obj), "isFrozen(\(obj))")
    }

    func assertNotFrozen(_ obj: AnyObject) throws {
        try assertFalse(ValuesKt.isFrozen(obj: obj), "isFrozen(\(obj))")
    }

    try assertFrozen(NSObject())
    try assertFrozen(TestSharedIImpl())
    try assertFrozen(ValuesKt.kotlinLambda(block: { return $0 }) as AnyObject)
    try assertNotFrozen(FinalClassExtOpen())
}

class PureSwiftClass {
}

struct PureSwiftStruct {
    var x: Int
}
class PureSwiftKotlinInterfaceImpl : I {
    func iFun() -> String {
        return "pure"
    }
}

func testPureSwiftClasses() throws {
    let pureSwiftClass = PureSwiftClass()
    try assertTrue(ValuesKt.same(pureSwiftClass) as? AnyObject === pureSwiftClass)

    try assertEquals(actual: 123, expected: (ValuesKt.same(PureSwiftStruct(x: 123)) as? PureSwiftStruct)?.x)
    try assertEquals(actual: "pure", expected: ValuesKt.iFunExt(PureSwiftKotlinInterfaceImpl()))
}

func testNames() throws {
    try assertEquals(actual: ValuesKt.PROPERTY_NAME_MUST_NOT_BE_ALTERED_BY_SWIFT, expected: 111)
    try assertEquals(actual: Deeply.NestedType().thirtyTwo, expected: 32)
    try assertEquals(actual: WithGenericDeeplyNestedType<AnyObject>().thirtyThree, expected: 33)
    try assertEquals(actual: CKeywords(float: 1.0, enum : 42, goto: true).goto_, expected: true)
    try assertEquals(actual: TypeOuter.Type_().thirtyFour, expected: 34)
    try assertTrue(String(describing: DeeplyNestedIType.self).hasSuffix("DeeplyNestedIType"))
}

class Base123 : Base23, ExtendedBase1 {
    override func same(value: KotlinInt?) -> KotlinInt {
        return value!
    }
}

func testSwiftOverride() throws {
    let impl = Base123()
    try assertEquals(actual: ValuesKt.call(base1: impl, value: 1), expected: 1)
    try assertEquals(actual: ValuesKt.call(extendedBase1: impl, value: 2), expected: 2)
    try assertEquals(actual: ValuesKt.call(base2: impl, value: 3), expected: 3)
    try assertEquals(actual: ValuesKt.call(base3: impl, value: 4), expected: 4)
    try assertEquals(actual: ValuesKt.call(base23: impl, value: 5), expected: 5)
}

class TransformIntToLongCallingSuper : TransformIntToLong {
    override func map(value: KotlinInt) -> KotlinLong {
        return super.map(value: value)
    }
}

func testKotlinOverride() throws {
    try assertEquals(actual: TransformInheritingDefault<NSNumber>().map(value: 1) as! Int32, expected: 1)
    try assertEquals(actual: TransformIntToDecimalString().map(value: 2), expected: "2")
    try assertEquals(actual: TransformIntToDecimalString().map(intValue: 3), expected: "3")
    try assertEquals(actual: ValuesKt.createTransformDecimalStringToInt().map(value: "4") as! Int32, expected: 4)
    try assertEquals(actual: TransformIntToLongCallingSuper().map(value: 5), expected: 5)
}

// See https://github.com/JetBrains/kotlin-native/issues/2945
func testGH2945() throws {
    let gh2945 = GH2945(errno: 1)
    try assertEquals(actual: 1, expected: gh2945.errno)
    gh2945.errno = 2
    try assertEquals(actual: 2, expected: gh2945.errno)

    try assertEquals(actual: 7, expected: gh2945.testErrnoInSelector(p: 3, errno: 4))
}

// See https://github.com/JetBrains/kotlin-native/issues/2830
func testGH2830() throws {
  try assertTrue(GH2830().getI() is GH2830I)
}

// See https://github.com/JetBrains/kotlin-native/issues/2959
func testGH2959() throws {
  try assertEquals(actual: GH2959().getI(id: 2959)[0].id, expected: 2959)
}

func testKClass() throws {
  let test = TestKClass()

  let testKClass = test.getKotlinClass(clazz: TestKClass.self)!
  try assertTrue(test.isTestKClass(kClass: testKClass))
  try assertFalse(test.isI(kClass: testKClass))
  try assertEquals(actual: testKClass.simpleName, expected: "TestKClass")

  let iKClass = test.getKotlinClass(protocol: TestKClassI.self)!
  try assertFalse(test.isTestKClass(kClass: iKClass))
  try assertTrue(test.isI(kClass: iKClass))
  try assertEquals(actual: iKClass.simpleName, expected: "I")

  try assertTrue(test.getKotlinClass(clazz: NSObject.self) == nil)
  try assertTrue(test.getKotlinClass(clazz: PureSwiftClass.self) == nil)
  try assertTrue(test.getKotlinClass(clazz: PureSwiftKotlinInterfaceImpl.self) == nil)
  try assertTrue(test.getKotlinClass(clazz: Base123.self) == nil)

  try assertTrue(test.getKotlinClass(protocol: NSObjectProtocol.self) == nil)
}

open class TestSR10177WorkaroundBase<T> {}
class TestSR10177WorkaroundDerived : TestSR10177WorkaroundBase<TestSR10177Workaround> {}

// See https://bugs.swift.org/browse/SR-10177 and https://bugs.swift.org/browse/SR-10217
func testSR10177Workaround() throws {
    let test = TestSR10177WorkaroundDerived()
    try assertTrue(String(describing: test).contains("TestSR10177WorkaroundDerived"))
}

func testClashes() throws {
    let test = TestClashesImpl()
    let test1: TestClashes1 = test
    let test2: TestClashes2 = test

    try assertEquals(actual: 1, expected: test1.clashingProperty)
    try assertEquals(actual: 1, expected: test2.clashingProperty_ as! Int32)
    try assertEquals(actual: 2, expected: test2.clashingProperty__ as! Int32)
}

func testInvalidIdentifiers() throws {
    let test = TestInvalidIdentifiers()

    try assertTrue(TestInvalidIdentifiers._Foo() is TestInvalidIdentifiers._Foo)
    try assertFalse(TestInvalidIdentifiers.Bar_() is TestInvalidIdentifiers._Foo)

    try assertEquals(actual: 42, expected: test.a_d_d(_1: 13, _2: 14, _3: 15))

    test._status = "OK"
    try assertEquals(actual: "OK", expected: test._status)

    try assertEquals(actual: TestInvalidIdentifiers.E._4_.value, expected: 4)
    try assertEquals(actual: TestInvalidIdentifiers.E._5_.value, expected: 5)
    try assertEquals(actual: TestInvalidIdentifiers.E.__.value, expected: 6)
    try assertEquals(actual: TestInvalidIdentifiers.E.___.value, expected: 7)

    try assertEquals(actual: TestInvalidIdentifiers.Companion_()._42, expected: 42)

    try assertEquals(actual: Set([test.__, test.___]), expected: Set(["$".utf16.first, "_".utf16.first]))
}

class ImplementingHiddenSubclass : TestDeprecation.ImplementingHidden {
    override func effectivelyHidden() -> Int32 {
        return -2
    }
}

func testDeprecation() throws {
    let test = TestDeprecation()
    try assertEquals(actual: test.openNormal(), expected: 1)

    let testHiddenOverride: TestDeprecation = TestDeprecation.HiddenOverride()
    try assertEquals(actual: testHiddenOverride.openNormal(), expected: 2)

    let testErrorOverride: TestDeprecation = TestDeprecation.ErrorOverride()
    try assertEquals(actual: testErrorOverride.openNormal(), expected: 3)

    let testWarningOverride: TestDeprecation = TestDeprecation.WarningOverride()
    try assertEquals(actual: testWarningOverride.openNormal(), expected: 4)

    try assertEquals(actual: test.callEffectivelyHidden(obj: ImplementingHiddenSubclass()), expected: -2)
}

func setAssociatedObject(object: AnyObject, value: AnyObject) {
    objc_setAssociatedObject(
        object,
        UnsafeRawPointer(bitPattern: 1)!,
        value,
        objc_AssociationPolicy.OBJC_ASSOCIATION_RETAIN
    )
}

func testWeakRefs() throws {
    try testWeakRefs0(frozen: false)
    try testWeakRefs0(frozen: true)
}

func testWeakRefs0(frozen: Bool) throws {
    func getObj(test: TestWeakRefs) -> AnyObject {
        return autoreleasepool { test.getObj() as AnyObject }
    }

    func test1() throws {
        var test = TestWeakRefs(frozen: frozen)

        var obj: AnyObject? = getObj(test: test)
        weak var ref = getObj(test: test)

        ValuesKt.gc()
        try assertTrue(ref === getObj(test: test)) // There are both Kotlin and Swift references to the object.

        obj = nil
        ValuesKt.gc()
        try assertTrue(ref === getObj(test: test)) // There are only Kotlin references to the object.

        test.clearObj()
        ValuesKt.gc()
        try assertTrue(ref === nil)
    }

    func test2() throws {
        var test = TestWeakRefs(frozen: frozen)

        var obj: AnyObject? = getObj(test: test)
        weak var ref = getObj(test: test)

        ValuesKt.gc()
        try assertTrue(ref === obj!) // There are both Kotlin and Swift references to the object.

        test.clearObj()
        ValuesKt.gc()
        try assertTrue(ref === obj!) // There are only Swift references to the object.

        obj = nil
        ValuesKt.gc()
        try assertTrue(ref === nil)
    }

    func test3() throws {
        class Holder {
            static weak var ref: AnyObject? = nil
            static var deinitialized = false

            deinit {
                // Access weak ref to Kotlin object during its counterpart dealloc:
                try! assertTrue(Holder.ref === nil)
                Holder.deinitialized = true
            }
        }

        Holder.deinitialized = false
        Holder.ref = nil

        var test = TestWeakRefs(frozen: frozen)

        Holder.ref = getObj(test: test)

        // Prepare Holder() to get deinitialized along with getObj(test: test):
        setAssociatedObject(
            object: getObj(test: test),
            value: Holder()
        )

        try assertFalse(Holder.ref === nil)
        try assertFalse(Holder.deinitialized)

        test.clearObj()
        ValuesKt.gc()

        try assertTrue(Holder.ref === nil)
        try assertTrue(Holder.deinitialized)
    }

    func test4() throws {
        class Holder {
            static weak var ref1: AnyObject? = nil
            static weak var ref2: AnyObject? = nil
            static var deinitialized: Int = 0

            deinit {
                // Access weak ref to Kotlin object during its counterpart dealloc:
                try! assertTrue(Holder.ref1 === nil)
                try! assertTrue(Holder.ref2 === nil)
                Holder.deinitialized += 1
            }
        }

        Holder.deinitialized = 0
        Holder.ref1 = nil
        Holder.ref2 = nil

        var test = TestWeakRefs(frozen: frozen)

        autoreleasepool {
            let cycle = test.createCycle()

            let obj1 = cycle[0] as AnyObject
            let obj2 = cycle[1] as AnyObject

            // Prepare Holders to get deinitialized along with obj1 and obj2:
            setAssociatedObject(object: obj1, value: Holder())
            setAssociatedObject(object: obj2, value: Holder())

            Holder.ref1 = obj1
            Holder.ref2 = obj2
        }

        try assertFalse(Holder.ref1 === nil)
        try assertFalse(Holder.ref2 === nil)
        try assertEquals(actual: Holder.deinitialized, expected: 0)

        ValuesKt.gc()

        try assertTrue(Holder.ref1 === nil)
        try assertTrue(Holder.ref2 === nil)
        try assertEquals(actual: Holder.deinitialized, expected: 2)
    }

    try test1()
    try test2()
    try test3()
    try test4()
}

var falseFlag = false

class TestSharedRefs {
    private func testLambdaSimple() throws {
        func getClosure() -> (() -> Void) {
            let lambda = autoreleasepool {
                SharedRefs().createLambda()
            }
            return { if falseFlag { lambda() } }
        }

        DispatchQueue.global().async(execute: getClosure())
    }

    private static func launchInNewThread(initializeKotlinRuntime: Bool, block: @escaping () -> Void) -> pthread_t {
        class Closure {
            static var currentBlock: (() -> Void)? = nil
            static var initializeKotlinRuntime: Bool = false
        }

        Closure.currentBlock = block
        Closure.initializeKotlinRuntime = initializeKotlinRuntime

        var thread: pthread_t? = nil
        let createCode = pthread_create(&thread, nil, { _ in
            if Closure.initializeKotlinRuntime {
                let ignore = SharedRefs() // Ensures that Kotlin runtime gets initialized.
            }

            Closure.currentBlock!()
            Closure.currentBlock = nil

            return nil
        }, nil)
        try! assertEquals(actual: createCode, expected: 0)
        return thread!
    }

    private static func joinThread(thread: pthread_t) {
        let joinCode = pthread_join(thread, nil)
        try! assertEquals(actual: joinCode, expected: 0)
    }

    private static func runInNewThread(initializeKotlinRuntime: Bool, block: @escaping () -> Void) {
        let thread = launchInNewThread(initializeKotlinRuntime: initializeKotlinRuntime, block: block)
        joinThread(thread: thread)
    }

    private func runInNewThread(initializeKotlinRuntime: Bool, block: @escaping () -> Void) {
        return TestSharedRefs.runInNewThread(initializeKotlinRuntime: initializeKotlinRuntime, block: block)
    }

    private func testObjectPartialRelease() {
        let object = autoreleasepool { SharedRefs().createRegularObject() }
        var objectVar: AnyObject? = object

        runInNewThread(initializeKotlinRuntime: true) {
            objectVar = nil
        }
    }

    private func testRunRefCount<T>(
        run: (@escaping () -> Void) -> Void,
        createObject: @escaping (SharedRefs) -> T
    ) throws {
        let refs = SharedRefs()

        var objectVar1: T? = autoreleasepool { createObject(refs) }
        var objectVar2: T? = nil

        try assertTrue(refs.hasAliveObjects())

        run {
            objectVar2 = objectVar1!
            objectVar1 = nil
        }

        try assertTrue(refs.hasAliveObjects())

        run {
            objectVar2 = nil
        }

        try assertFalse(refs.hasAliveObjects())
    }

    private func testBackgroundRefCount<T>(createObject: @escaping (SharedRefs) -> T) throws {
        try testRunRefCount(
            run: { runInNewThread(initializeKotlinRuntime: false, block: $0) },
            createObject: createObject
        )

        try testRunRefCount(
            run: { runInNewThread(initializeKotlinRuntime: true, block: $0) },
            createObject: createObject
        )
    }

    private func testReferenceOutlivesThread(releaseWithKotlinRuntime: Bool) throws {
        var objectVar: AnyObject? = nil
        weak var objectWeakVar: AnyObject? = nil
        var collection: AnyObject? = nil

        runInNewThread(initializeKotlinRuntime: false) {
            autoreleasepool {
                let refs = SharedRefs()
                collection = refs.createCollection()

                let object = refs.createRegularObject()
                objectVar = object
                objectWeakVar = object

                try! assertTrue(objectWeakVar === object)
            }
        }

        runInNewThread(initializeKotlinRuntime: releaseWithKotlinRuntime) {
            objectVar = nil
            collection = nil
            ValuesKt.gc()
            try! assertTrue(objectWeakVar === nil)
        }

    }

    private func testMoreWorkBeforeThreadExit() throws {
        class Deinit {
            static var object1: AnyObject? = nil
            static var object2: AnyObject? = nil
            static weak var weakVar2: AnyObject? = nil

            deinit {
                TestSharedRefs.runInNewThread(initializeKotlinRuntime: false) {
                    Deinit.object2 = nil
                }
            }
        }

        runInNewThread(initializeKotlinRuntime: false) {
            autoreleasepool {
                let object1 = SharedRefs.MutableData()
                Deinit.object1 = object1
                setAssociatedObject(object: object1, value: Deinit())

                let object2 = SharedRefs.MutableData()
                Deinit.object2 = object2
                Deinit.weakVar2 = object2
            }

            TestSharedRefs.runInNewThread(initializeKotlinRuntime: false) {
                Deinit.object1 = nil
            }
        }

        try assertTrue(Deinit.weakVar2 === nil)
    }

    func testRememberNewObject(createObject: @escaping (SharedRefs) -> AnyObject) throws {

        class TestImpl : TestRememberNewObject {
            let cleanupFinishedSemaphore = DispatchSemaphore(value: 0)
            let threadWaitingForCleanupSemaphore = DispatchSemaphore(value: 0)

            var obj: AnyObject? = nil

            func getObject() -> Any {
                return obj!
            }

            func waitForCleanup() {
                threadWaitingForCleanupSemaphore.signal()
                cleanupFinishedSemaphore.wait()
            }
        }

        let test = TestImpl()

        let refs = SharedRefs()
        try assertFalse(refs.hasAliveObjects())

        autoreleasepool {
            test.obj = createObject(refs)
        }

        try assertTrue(refs.hasAliveObjects())

        let thread = TestSharedRefs.launchInNewThread(initializeKotlinRuntime: false) {
            ValuesKt.testRememberNewObject(test: test)
        }

        test.threadWaitingForCleanupSemaphore.wait()
        test.obj = nil
        ValuesKt.gc()

        try assertTrue(refs.hasAliveObjects())

        test.cleanupFinishedSemaphore.signal()

        TestSharedRefs.joinThread(thread: thread)
        try assertFalse(refs.hasAliveObjects())
    }

    func test() throws {
        try testLambdaSimple()
        try testObjectPartialRelease()

        try testBackgroundRefCount(createObject: { $0.createLambda() })
        try testBackgroundRefCount(createObject: { $0.createRegularObject() })
        try testBackgroundRefCount(createObject: { $0.createCollection() })

        try testBackgroundRefCount(createObject: { $0.createFrozenLambda() })
        try testBackgroundRefCount(createObject: { $0.createFrozenRegularObject() })
        try testBackgroundRefCount(createObject: { $0.createFrozenCollection() })

        try testReferenceOutlivesThread(releaseWithKotlinRuntime: false)
        try testReferenceOutlivesThread(releaseWithKotlinRuntime: true)
        try testMoreWorkBeforeThreadExit()

        try testRememberNewObject(createObject: { $0.createFrozenRegularObject() })
        try testRememberNewObject(createObject: { $0.createFrozenCollection() })

        usleep(300 * 1000)
    }
}

// See https://github.com/JetBrains/kotlin-native/issues/2931
func testGH2931() throws {
    for i in 0..<50000 {
        let holder = GH2931.Holder()
        let queue = DispatchQueue.global(qos: .background)
        let group = DispatchGroup()

        for j in 0..<2 {
            group.enter()
            queue.async {
                autoreleasepool {
                    holder.data
                }
                group.leave()
            }
        }

        group.wait()
    }
}

class ClassForTypeCheckInheritor : ClassForTypeCheck { }

func testClassTypeCheck() throws {
    try assertTrue(ValuesKt.testClassTypeCheck(x: ClassForTypeCheckInheritor()))
}

class ClassForInterfaceTypeCheckInheritor1 : InterfaceForTypeCheck { }
class ClassForInterfaceTypeCheckInheritor2 : Base23, InterfaceForTypeCheck { }
class ClassForInterfaceTypeCheckInheritor3 : Base23, ExtendedBase1, InterfaceForTypeCheck { }
class ClassForInterfaceTypeCheck_Fail : Base23 { }

func testInterfaceTypeCheck() throws {
    try assertTrue(ValuesKt.testInterfaceTypeCheck(x: ClassForInterfaceTypeCheckInheritor1()))
    try assertTrue(ValuesKt.testInterfaceTypeCheck(x: ClassForInterfaceTypeCheckInheritor2()))
    try assertTrue(ValuesKt.testInterfaceTypeCheck(x: ClassForInterfaceTypeCheckInheritor3()))
    try assertFalse(ValuesKt.testInterfaceTypeCheck(x: ClassForInterfaceTypeCheck_Fail()))
}

class AbstractInterface : AbstractInterfaceBase {
    override func bar() -> Int32 {
        return 42
    }
}

// See https://github.com/JetBrains/kotlin-native/issues/3503
func testGH3503_1() throws {
    try assertEquals(actual: ValuesKt.testAbstractInterfaceCall(x: AbstractInterface()), expected: 42)
}

class AbstractInterface2 : AbstractInterfaceBase2 {

}

func testGH3503_2() throws {
    try assertEquals(actual: ValuesKt.testAbstractInterfaceCall2(x: AbstractInterface2()), expected: 42)
}

class AbstractInterface3 : AbstractInterfaceBase3 {
    override func foo() -> Int32 {
        return 42
    }
}

func testGH3503_3() throws {
    try assertEquals(actual: ValuesKt.testAbstractInterfaceCall(x: AbstractInterface3()), expected: 42)
}

func testGH3525() throws {
    try assertEquals(actual: ValuesKt.gh3525BaseInitCount, expected: 0)
    try assertEquals(actual: ValuesKt.gh3525InitCount, expected: 0)

    let gh3525_1 = GH3525()
    try assertTrue(gh3525_1 is GH3525)

    try assertEquals(actual: ValuesKt.gh3525BaseInitCount, expected: 1)
    try assertEquals(actual: ValuesKt.gh3525InitCount, expected: 1)

    let gh3525_2 = GH3525()
    try assertTrue(gh3525_2 is GH3525)

    try assertEquals(actual: ValuesKt.gh3525BaseInitCount, expected: 1)
    try assertEquals(actual: ValuesKt.gh3525InitCount, expected: 1)

    try assertTrue(gh3525_1 === gh3525_2)
}

func testStringConversion() throws {
    func test1() throws {
        let test = TestStringConversion()

        let buffer = NSMutableString()
        buffer.append("a")
        test.str = buffer
        buffer.append("b")

        try assertEquals(actual: buffer, expected: "ab")
        // Ensure test.str isn't affected by buffer mutation:
        try assertEquals(actual: test.str as! NSString, expected: "a")
    }

    func ensureNoCopy(nsStr: NSString) throws {
        let test = TestStringConversion()

        test.str = nsStr
        let nsStr2 = test.str as! NSString

        // Ensure no additional NSString created on both conversions:
        try assertTrue(nsStr === nsStr2)
    }

    func test2() throws {
        var str = "a"
        str += NSObject().description
        try ensureNoCopy(nsStr: str as NSString)

        try ensureNoCopy(nsStr: NSString("abc"))

        try ensureNoCopy(nsStr: NSString(format: "%d%d%d", 3, 2, 1))
    }

    try test1()
    try test2()
}

class GH3825SwiftImpl : GH3825 {
    class E : Error {}

    func call0(callback: () -> KotlinBoolean) throws {
        if callback().boolValue { throw E() }
    }

    func call1(doThrow: Bool, callback: () -> Void) throws {
        if doThrow { throw E() }
        callback()
    }

    func call2(callback: () -> Void, doThrow: Bool) throws {
        if doThrow { throw E() }
        callback()
    }
}

func testGH3825() throws {
    try ValuesKt.testGH3825(gh3825: GH3825SwiftImpl())

    let test = GH3825KotlinImpl()
    var count = 0

    try testThrowing { try test.call0 { true } }
    try test.call0 {
        count += 1
        return false
    }
    try assertEquals(actual: count, expected: 1)

    try testThrowing { try test.call1(doThrow: true) { count += 1 } }
    try test.call1(doThrow: false) { count += 1 }
    try assertEquals(actual: count, expected: 2)

    try testThrowing { try test.call2(callback: { count += 1 }, doThrow: true)}
    try test.call2(callback: { count += 1 }, doThrow: false)
    try assertEquals(actual: count, expected: 3)
}


func testMapsExport() throws {
	// Original reproducer failed in different way for MutableMap (iOS 11) and Map (MacOS 10.14, iOS 13)

    try assertEquals(actual: ValuesKt.mapBoolean2String()[true], expected: "true")
    try assertEquals(actual: ValuesKt.mapByte2Short()[-1], expected: 2)
    try assertEquals(actual: ValuesKt.mapShort2Byte()[-2], expected: 1)
    try assertEquals(actual: ValuesKt.mapInt2Long()[-4], expected: 8)
    try assertEquals(actual: ValuesKt.mapLong2Long()[-8], expected: 8)
    try assertEquals(actual: ValuesKt.mapUByte2Boolean()[128], expected: true)
    try assertEquals(actual: ValuesKt.mapUShort2Byte()[0x8000], expected: 1)
    try assertEquals(actual: ValuesKt.mapUInt2Long()[0x7FFFFFFF], expected: 7)
    // the following samples require explicit cast to KotlinUInt or KotlinULong
    try assertEquals(actual: ValuesKt.mapUInt2Long()[KotlinUInt(-0x8000_0000)], expected: 8)
    _ = ValuesKt.mapULong2Long() as! [KotlinULong: KotlinLong] // test cast
    var u64: UInt64 = 0x8000_0000_0000_0000
    try assertEquals(actual: ValuesKt.mapULong2Long()[KotlinULong(value: u64)], expected: 8)

    _ = ValuesKt.mapFloat2Float() as! [KotlinFloat: KotlinFloat] // test cast
    try assertEquals(actual: ValuesKt.mapFloat2Float()[3.14], expected: 100.0)
    try assertEquals(actual: ValuesKt.mapDouble2String()[2.718281828459045], expected: "2.718281828459045")

	// test also explicit cast to [:] of primitiva types, e.g. [Int: Int]
    try assertEquals(actual: (ValuesKt.mutBoolean2String() as! [Bool: String])[true], expected: "true")
    try assertEquals(actual: (ValuesKt.mutByte2Short() as! [Int8: Int16])[-1], expected: 2)
    try assertEquals(actual: (ValuesKt.mutShort2Byte() as! [Int16: Int8])[-2], expected: 1)
    try assertEquals(actual: (ValuesKt.mutInt2Long() as! [Int: Int64])[-4], expected: 8)
    try assertEquals(actual: (ValuesKt.mutLong2Long() as! [Int64: Int64])[-8], expected: 8)

    try assertEquals(actual: (ValuesKt.mutUByte2Boolean() as! [UInt8: Bool])[128], expected: true)
    try assertEquals(actual: (ValuesKt.mutUShort2Byte() as! [UInt16: Int8])[0x8000], expected: 1)
    // the following samples require explicit cast to KotlinUInt or KotlinULong
    try assertEquals(actual: (ValuesKt.mutUInt2Long() as! [UInt: Int64])[UInt(0x8000_0000)], expected: 8)

    try assertEquals(actual: (ValuesKt.mutULong2Long() as! [UInt64: Int64])[u64], expected: 8)

    try assertEquals(actual: (ValuesKt.mutFloat2Float() as! [Float: Float])[3.14], expected: 100.0)
    try assertEquals(actual: (ValuesKt.mutDouble2String() as! [Double: String])[2.718281828459045], expected: "2.718281828459045")
}

class Baz_FakeOverrideInInterface : Bar_FakeOverrideInInterface {
    func foo(t: Any?) {}
}

func testFakeOverrideInInterface() throws {
    ValuesKt.callFoo_FakeOverrideInInterface(obj: Baz_FakeOverrideInInterface())
}

// -------- Execution of the test --------

class ValuesTests : SimpleTestProvider {
    override init() {
        super.init()

        test("TestValues", testVals)
        test("TestVars", testVars)
        test("TestDoubles", testDoubles)
        test("TestNumbers", testNumbers)
        test("TestLists", testLists)
        test("TestLazyValues", testLazyVal)
        test("TestDelegatedProperties", testDelegatedProp)
        test("TestGetterDelegate", testGetterDelegate)
        test("TestNulls", testNulls)
        test("TestAnyVar", testAnyVar)
        test("TestFunctions", testFunctions)
        test("TestExceptions", testExceptions)
        test("TestFuncType", testFuncType)
        test("TestGenericsFoo", testGenericsFoo)
        test("TestVararg", testVararg)
        test("TestStringExtension", testStrExtFun)
        test("TestAnyToString", testAnyToString)
        test("TestAnyPrint", testAnyPrint)
        test("TestCharExtensions", testCharExtensions)
        test("TestLambda", testLambda)
        test("TestInterfaceExtension", testInterfaceExtension)
        test("TestClassInstances", testClassInstances)
        test("TestEnum", testEnum)
        test("TestDataClass", testDataClass)
        test("TestCompanionObj", testCompanionObj)
        test("TestInlineClasses", testInlineClasses)
        test("TestShared", testShared)
        test("TestPureSwiftClasses", testPureSwiftClasses)
        test("TestNames", testNames)
        test("TestSwiftOverride", testSwiftOverride)
        test("TestKotlinOverride", testKotlinOverride)
        test("TestGH2945", testGH2945)
        test("TestGH2830", testGH2830)
        test("TestGH2959", testGH2959)
        test("TestKClass", testKClass)
        test("TestSR10177Workaround", testSR10177Workaround)
        test("TestClashes", testClashes)
        test("TestInvalidIdentifiers", testInvalidIdentifiers)
        test("TestDeprecation", testDeprecation)
        test("TestWeakRefs", testWeakRefs)
        test("TestSharedRefs", TestSharedRefs().test)
        test("TestClassTypeCheck", testClassTypeCheck)
        test("TestInterfaceTypeCheck", testInterfaceTypeCheck)
        test("TestGH3503_1", testGH3503_1)
        test("TestGH3503_2", testGH3503_2)
        test("TestGH3503_3", testGH3503_3)
        test("TestGH3525", testGH3525)
        test("TestStringConversion", testStringConversion)
        test("TestGH3825", testGH3825)
        test("TestMapsExport", testMapsExport)
        test("TestFakeOverrideInInterface", testFakeOverrideInInterface)

        // Stress test, must remain the last one:
        test("TestGH2931", testGH2931)
    }
}