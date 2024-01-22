/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Kt

private func testCallSimple() throws {
    var result: KotlinInt? = nil
    var error: Error? = nil
    var completionCalled = 0

    CoroutinesKt.suspendFun { _result, _error in
        completionCalled += 1
        result = _result
        error = _error
    }

    try assertEquals(actual: completionCalled, expected: 1)
    try assertEquals(actual: result, expected: 42)
    try assertNil(error)
}

private func testUnitCallSimple() throws {
#if LEGACY_SUSPEND_UNIT_FUNCTION_EXPORT
    var result: KotlinUnit? = nil
#endif
    var error: Error? = nil
    var completionCalled = 0

#if LEGACY_SUSPEND_UNIT_FUNCTION_EXPORT
    CoroutinesKt.unitSuspendFun { _result, _error in
        completionCalled += 1
        result = _result
        error = _error
    }
#else
    CoroutinesKt.unitSuspendFun { _error in
        completionCalled += 1
        error = _error
    }
#endif

#if LEGACY_SUSPEND_UNIT_FUNCTION_EXPORT
    try assertSame(actual: result, expected: KotlinUnit.shared)
#endif
    try assertEquals(actual: completionCalled, expected: 1)
    try assertNil(error)
}

private func testUnitCallNonMainDispatcher() throws {
#if LEGACY_SUSPEND_UNIT_FUNCTION_EXPORT
    var result: KotlinUnit? = nil
#endif
    var error: Error? = nil
    var completionCalled = 0

    let group = DispatchGroup()

#if ALLOW_SUSPEND_ANY_THREAD
#if LEGACY_SUSPEND_UNIT_FUNCTION_EXPORT
    DispatchQueue.global().async(group: group) {
        CoroutinesKt.unitSuspendFun { _result, _error in
            completionCalled += 1
            result = _result
            error = _error
        }
    }
#else
    DispatchQueue.global().async(group: group) {
        CoroutinesKt.unitSuspendFun { _error in
            completionCalled += 1
            error = _error
        }
    }
#endif
#endif

#if ALLOW_SUSPEND_ANY_THREAD
    group.wait()
    try assertEquals(actual: completionCalled, expected: 1)
#endif
}


private func testCallSuspendFun(doSuspend: Bool, doThrow: Bool) throws {
    class C {}
    let expectedResult = C()

    var completionCalled = 0
    var result: AnyObject? = nil
    var error: Error? = nil

    CoroutinesKt.suspendFun(result: expectedResult, doSuspend: doSuspend, doThrow: doThrow) { _result, _error in
        completionCalled += 1
        result = _result as AnyObject?
        error = _error
    }

    try assertEquals(actual: completionCalled, expected: 1)

    if doThrow {
        try assertNil(result)
        try assertTrue(error?.kotlinException is CoroutineException)
    } else {
        try assertSame(actual: result, expected: expectedResult)
        try assertNil(error)
    }
}

private func testCallUnitSuspendFun(doSuspend: Bool, doThrow: Bool) throws {
    var completionCalled = 0
    var result: AnyObject? = nil
    var error: Error? = nil

#if LEGACY_SUSPEND_UNIT_FUNCTION_EXPORT
    CoroutinesKt.unitSuspendFun(doSuspend: doSuspend, doThrow: doThrow) { _result, _error in
        completionCalled += 1
        result = _result as AnyObject?
        error = _error
    }
#else
    CoroutinesKt.unitSuspendFun(doSuspend: doSuspend, doThrow: doThrow) { _error in
        completionCalled += 1
        error = _error
    }
#endif

    try assertEquals(actual: completionCalled, expected: 1)

    if doThrow {
#if LEGACY_SUSPEND_UNIT_FUNCTION_EXPORT
        try assertNil(result)
#endif
        try assertTrue(error?.kotlinException is CoroutineException)
    } else {
#if LEGACY_SUSPEND_UNIT_FUNCTION_EXPORT
        try assertSame(actual: result, expected: KotlinUnit.shared)
#endif
        try assertNil(error)
    }
}

private class WeakRefHolder {
    weak var value: AnyObject? = nil
}

#if NO_GENERICS
typealias AnyContinuationHolder = ContinuationHolder
#else
typealias AnyContinuationHolder = ContinuationHolder<AnyObject>
#endif

// This code is extracted to a function just to ensure that all local variables get released at the end.
private func callSuspendFunAsync(
    weakRefToObjectCapturedByCompletion: WeakRefHolder,
    continuationHolder: AnyContinuationHolder,
    completionHandler: @escaping (Any?, Error?) -> Void
) throws {
    class C {}
    let capturedByCompletion = C()
    weakRefToObjectCapturedByCompletion.value = capturedByCompletion

    CoroutinesKt.suspendFunAsync(result: nil, continuationHolder: continuationHolder) { _result, _error in
        try! assertSame(actual: capturedByCompletion, expected: weakRefToObjectCapturedByCompletion.value)
        completionHandler(_result, _error)
    }
}

private func testSuspendFuncAsync(doThrow: Bool) throws {
    var completionCalled = 0
    var result: AnyObject? = nil
    var error: Error? = nil

    let continuationHolder = AnyContinuationHolder()

    let weakRefToObjectCapturedByCompletion = WeakRefHolder()
    try assertTrue(weakRefToObjectCapturedByCompletion.value === nil)
    try autoreleasepool {
        try callSuspendFunAsync(
            weakRefToObjectCapturedByCompletion: weakRefToObjectCapturedByCompletion,
            continuationHolder: continuationHolder
        ) { _result, _error in
            completionCalled += 1
            result = _result as AnyObject?
            error = _error
        }
    }
    CoroutinesKt.gc()
    // This assert checks that suspendFunAsync retains the completion handler:
    try assertFalse(weakRefToObjectCapturedByCompletion.value === nil)

    try assertEquals(actual: completionCalled, expected: 0)

    if doThrow {
        let exception = CoroutineException()
        continuationHolder.resumeWithException(exception: exception)

        try assertEquals(actual: completionCalled, expected: 1)

        try assertNil(result)
        try assertSame(actual: error?.kotlinException as AnyObject?, expected: exception)
    } else {
        class C {}
        let expectedResult = C()
        continuationHolder.resume(value: expectedResult)

        try assertEquals(actual: completionCalled, expected: 1)

        try assertSame(actual: result, expected: expectedResult)
        try assertNil(error)
    }

#if !NOOP_GC
    CoroutinesKt.gc()
    // This assert checks that the completion handler gets properly released after all:
    try assertTrue(weakRefToObjectCapturedByCompletion.value === nil)
#endif
}

#if NO_GENERICS
typealias UnitContinuationHolder = ContinuationHolder
#else
typealias UnitContinuationHolder = ContinuationHolder<KotlinUnit>
#endif

// This code is extracted to a function just to ensure that all local variables get released at the end.
private func callUnitSuspendFunAsync(
    weakRefToObjectCapturedByCompletion: WeakRefHolder,
    continuationHolder: UnitContinuationHolder,
    completionHandler: @escaping (Error?) -> Void
) throws {
    class C {}
    let capturedByCompletion = C()
    weakRefToObjectCapturedByCompletion.value = capturedByCompletion
#if LEGACY_SUSPEND_UNIT_FUNCTION_EXPORT
    CoroutinesKt.unitSuspendFunAsync(continuationHolder: continuationHolder) { _result, _error in
        try! assertSame(actual: capturedByCompletion, expected: weakRefToObjectCapturedByCompletion.value)
        completionHandler(_error)
    }
#else
    CoroutinesKt.unitSuspendFunAsync(continuationHolder: continuationHolder) { _error in
        try! assertSame(actual: capturedByCompletion, expected: weakRefToObjectCapturedByCompletion.value)
        completionHandler(_error)
    }
#endif
}

private func testUnitSuspendFuncAsync(doThrow: Bool) throws {
    var completionCalled = 0
    var error: Error? = nil

    let continuationHolder = UnitContinuationHolder()

    let weakRefToObjectCapturedByCompletion = WeakRefHolder()
    try assertTrue(weakRefToObjectCapturedByCompletion.value === nil)
    try autoreleasepool {
        try callUnitSuspendFunAsync(
            weakRefToObjectCapturedByCompletion: weakRefToObjectCapturedByCompletion,
            continuationHolder: continuationHolder
        ) { _error in
            completionCalled += 1
            error = _error
        }
    }
    CoroutinesKt.gc()
    // This assert checks that unitSuspendFunAsync retains the completion handler:
    try assertFalse(weakRefToObjectCapturedByCompletion.value === nil)

    try assertEquals(actual: completionCalled, expected: 0)

    if doThrow {
        let exception = CoroutineException()
        continuationHolder.resumeWithException(exception: exception)

        try assertEquals(actual: completionCalled, expected: 1)

        try assertSame(actual: error?.kotlinException as AnyObject?, expected: exception)
    } else {
        continuationHolder.resume(value: KotlinUnit.shared)

        try assertEquals(actual: completionCalled, expected: 1)

        try assertNil(error)
    }

#if !NOOP_GC
    CoroutinesKt.gc()
    // This assert checks that the completion handler gets properly released after all:
    try assertTrue(weakRefToObjectCapturedByCompletion.value === nil)
#endif
}

private func testCall() throws {
    try testCallSuspendFun(doSuspend: true, doThrow: false)
    try testCallSuspendFun(doSuspend: false, doThrow: false)
    try testCallSuspendFun(doSuspend: true, doThrow: true)
    try testCallSuspendFun(doSuspend: false, doThrow: true)

    try testCallUnitSuspendFun(doSuspend: true, doThrow: false)
    try testCallUnitSuspendFun(doSuspend: false, doThrow: false)
    try testCallUnitSuspendFun(doSuspend: true, doThrow: true)
    try testCallUnitSuspendFun(doSuspend: false, doThrow: true)

    try testSuspendFuncAsync(doThrow: false)
    try testSuspendFuncAsync(doThrow: true)

    try testUnitSuspendFuncAsync(doThrow: false)
    try testUnitSuspendFuncAsync(doThrow: true)
}

private func testCallSuspendFunChain(doSuspend: Bool, doThrow: Bool) throws {
    class C {}
    let expectedResult = C()

    var completionCalled = 0
    var result: AnyObject? = nil
    var error: Error? = nil

    CoroutinesKt.suspendFun(result: expectedResult, doSuspend: doSuspend, doThrow: doThrow) { _resultOuter, _errorOuter in
        CoroutinesKt.suspendFun(result: expectedResult, doSuspend: doSuspend, doThrow: doThrow) { _result, _error in
            completionCalled += 1
            result = _result as AnyObject?
            error = _error
        }
    }

    try assertEquals(actual: completionCalled, expected: 1)

    if doThrow {
        try assertNil(result)
        try assertTrue(error?.kotlinException is CoroutineException)
    } else {
        try assertSame(actual: result, expected: expectedResult)
        try assertNil(error)
    }
}

private func testCallChain() throws {
    try testCallSuspendFunChain(doSuspend: true, doThrow: false)
    try testCallSuspendFunChain(doSuspend: false, doThrow: false)
    try testCallSuspendFunChain(doSuspend: true, doThrow: true)
    try testCallSuspendFunChain(doSuspend: false, doThrow: true)
}

private class SuspendFunImpl : SuspendFun {
    class E : Error {}

    var completion: (() -> Void)? = nil

    func suspendFun(doYield: Bool, doThrow: Bool, completionHandler: @escaping (KotlinInt?, Error?) -> Void) {
        func callCompletion() {
            if doThrow {
                completionHandler(nil, E())
            } else {
                completionHandler(17, nil)
            }
        }

        if doYield {
            self.completion = callCompletion
        } else {
            callCompletion()
        }
    }
}

private func testSuspendFunImpl(doYield: Bool, doThrow: Bool) throws {
#if NO_GENERICS
    let resultHolder = ResultHolder()
#else
    let resultHolder = ResultHolder<KotlinInt>()
#endif

    let impl = SuspendFunImpl()

    CoroutinesKt.callSuspendFun(
        suspendFun: impl,
        doYield: doYield,
        doThrow: doThrow,
        resultHolder: resultHolder
    )

    if doYield {
        try assertEquals(actual: resultHolder.completed, expected: 0)
        guard let completion = impl.completion else { try fail() }
        completion()
    }

    try assertEquals(actual: resultHolder.completed, expected: 1)

    if doThrow {
        try assertNil(resultHolder.result)
        if let e = resultHolder.exception {
            try assertFailsWith(SuspendFunImpl.E.self) { try CoroutinesKt.throwException(exception: e) }
        } else {
            try fail()
        }
    } else {
        try assertEquals(actual: resultHolder.result as! Int, expected: 17)
        try assertNil(resultHolder.exception)
    }
}

private func testSuspendFunImpl2(doYield: Bool, doThrow: Bool) throws {
    let impl = SuspendFunImpl()

    var completionCalled = 0
    var result: KotlinInt? = nil
    var error: Error? = nil

    CoroutinesKt.callSuspendFun2(suspendFun: impl, doYield: doYield, doThrow: doThrow) { _result, _error in
        completionCalled += 1
        result = _result
        error = _error
    }

    if doYield {
        try assertEquals(actual: completionCalled, expected: 0)
        guard let completion = impl.completion else { try fail() }
        completion()
    }

    try assertEquals(actual: completionCalled, expected: 1)

    if doThrow {
        try assertNil(result)
        try assertTrue(error is SuspendFunImpl.E)
    } else {
        try assertEquals(actual: result, expected: 17)
        try assertNil(error)
    }
}

private func testOverride() throws {
    try testSuspendFunImpl(doYield: false, doThrow: false)
    try testSuspendFunImpl(doYield: false, doThrow: true)
    try testSuspendFunImpl(doYield: true, doThrow: false)
    try testSuspendFunImpl(doYield: true, doThrow: true)

    try testSuspendFunImpl2(doYield: false, doThrow: false)
    try testSuspendFunImpl2(doYield: false, doThrow: true)
    try testSuspendFunImpl2(doYield: true, doThrow: false)
    try testSuspendFunImpl2(doYield: true, doThrow: true)
}

private class SwiftSuspendBridge : AbstractSuspendBridge {
    class E : Error {}

    override func intAsAny(value: KotlinInt, completionHandler: @escaping (KotlinInt?, Error?) -> Void) {
        completionHandler(value, nil)
    }

#if LEGACY_SUSPEND_UNIT_FUNCTION_EXPORT
    override func unit(value: KotlinInt, completionHandler: @escaping (KotlinUnit?, Error?) -> Void) {
        completionHandler(KotlinUnit(), nil)
    }
#else
    override func unit(value: KotlinInt, completionHandler: @escaping (Error?) -> Void) {
        completionHandler(nil)
    }
#endif

    override func unitAsAny(value: KotlinInt, completionHandler: @escaping (KotlinUnit?, Error?) -> Void) {
        completionHandler(KotlinUnit(), nil)
    }

    override func nullableUnit(value: KotlinInt, completionHandler: @escaping (KotlinUnit?, Error?) -> Void) {
        completionHandler(KotlinUnit(), nil)
    }

    override func nothingAsInt(value: KotlinInt, completionHandler: @escaping (KotlinNothing?, Error?) -> Void) {
        completionHandler(nil, E())
    }

    override func nothingAsAny(value: KotlinInt, completionHandler: @escaping (KotlinNothing?, Error?) -> Void) {
        completionHandler(nil, E())
    }

#if LEGACY_SUSPEND_UNIT_FUNCTION_EXPORT
    override func nothingAsUnit(value: KotlinInt, completionHandler: @escaping (KotlinNothing?, Error?) -> Void) {
        completionHandler(nil, E())
    }
#else
    override func nothingAsUnit(value: KotlinInt, completionHandler: @escaping (Error?) -> Void) {
        completionHandler(E())
    }
#endif
}

private func testBridges() throws {
#if NO_GENERICS
    let resultHolder = ResultHolder()
#else
    let resultHolder = ResultHolder<KotlinUnit>()
#endif
    try CoroutinesKt.callSuspendBridge(bridge: SwiftSuspendBridge(), resultHolder: resultHolder)

    try assertEquals(actual: resultHolder.completed, expected: 1)
    try assertNil(resultHolder.exception)
    try assertSame(actual: resultHolder.result as AnyObject, expected: KotlinUnit())
}

private func testImplicitThrows1() throws {
    var error: Error? = nil
    var completionCalled = 0

#if LEGACY_SUSPEND_UNIT_FUNCTION_EXPORT
    var result: KotlinUnit? = nil

    CoroutinesKt.throwCancellationException { _result, _error in
        completionCalled += 1
        result = _result
        error = _error
    }

    try assertNil(result)
#else
    CoroutinesKt.throwCancellationException { _error in
        completionCalled += 1
        error = _error
    }
#endif

    try assertEquals(actual: completionCalled, expected: 1)
    try assertTrue(error?.kotlinException is KotlinCancellationException)
}

private func testImplicitThrows2() throws {
    var error: Error? = nil
    var completionCalled = 0

#if LEGACY_SUSPEND_UNIT_FUNCTION_EXPORT
    var result: KotlinUnit? = nil

    ThrowCancellationExceptionImpl().throwCancellationException { _result, _error in
            completionCalled += 1
            result = _result
            error = _error
    }

    try assertNil(result)
#else
    ThrowCancellationExceptionImpl().throwCancellationException { _error in
        completionCalled += 1
        error = _error
    }
#endif

    try assertEquals(actual: completionCalled, expected: 1)
    try assertTrue(error?.kotlinException is KotlinCancellationException)
}

private func testSuspendFunctionType0(f: KotlinSuspendFunction0, expectedResult: String) throws {
    try assertTrue((f as AnyObject) is KotlinSuspendFunction0)

    var result: String? = nil
    var error: Error? = nil
    var completionCalled = 0

    f.invoke { _result, _error in
        completionCalled += 1
        result = _result as? String
        error = _error
    }

    try assertEquals(actual: completionCalled, expected: 1)
    try assertEquals(actual: result, expected: expectedResult)
    try assertNil(error)
}

private func testSuspendFunctionType1(f: KotlinSuspendFunction1) throws {
    try assertTrue((f as AnyObject) is KotlinSuspendFunction1)

    var result: String? = nil
    var error: Error? = nil
    var completionCalled = 0

    f.invoke(p1: "suspend function type") { _result, _error in
        completionCalled += 1
        result = _result as? String
        error = _error
    }

    try assertEquals(actual: completionCalled, expected: 1)
    try assertEquals(actual: result, expected: "suspend function type 1")
    try assertNil(error)
}

private func testSuspendFunctionType() throws {
    try testSuspendFunctionType0(f: CoroutinesKt.getSuspendLambda0(), expectedResult: "lambda 0")
    try testSuspendFunctionType0(f: CoroutinesKt.getSuspendCallableReference0(), expectedResult: "callable reference 0")
    try testSuspendFunctionType0(f: CoroutinesKt.getSuspendChild0(), expectedResult: "child 0")
    try testSuspendFunctionType1(f: CoroutinesKt.getSuspendLambda1())
    try testSuspendFunctionType1(f: CoroutinesKt.getSuspendCallableReference1())
    try testSuspendFunctionType1(f: CoroutinesKt.getSuspendChild1())
}

private func testKSuspendFunctionType0(f: KotlinKSuspendFunction0, expectedResult: String) throws {
    try assertTrue((f as AnyObject) is KotlinKSuspendFunction0)

    var result: String? = nil
    var error: Error? = nil
    var completionCalled = 0

    f.invoke { _result, _error in
        completionCalled += 1
        result = _result as? String
        error = _error
    }

    try assertEquals(actual: completionCalled, expected: 1)
    try assertEquals(actual: result, expected: expectedResult)
    try assertNil(error)
}

private func testKSuspendFunctionType1(f: KotlinKSuspendFunction1) throws {
    try assertTrue((f as AnyObject) is KotlinKSuspendFunction1)

    var result: String? = nil
    var error: Error? = nil
    var completionCalled = 0

    f.invoke(p1: "suspend function type") { _result, _error in
        completionCalled += 1
        result = _result as? String
        error = _error
    }

    try assertEquals(actual: completionCalled, expected: 1)
    try assertEquals(actual: result, expected: "suspend function type 1")
    try assertNil(error)
}

private func testKSuspendFunctionType() throws {
    try testKSuspendFunctionType0(f: CoroutinesKt.getKSuspendCallableReference0(), expectedResult: "callable reference 0")
    try testKSuspendFunctionType1(f: CoroutinesKt.getKSuspendCallableReference1())
}

private func testSuspendFunctionSwiftImpl() throws {
    var result: String? = nil
    var error: Error? = nil
    var completionCalled = 0

    CoroutinesKt.invoke1(block: SuspendFunction1SwiftImpl(), argument: "suspend function") { _result, _error in
        completionCalled += 1
        result = _result as? String
        error = _error
    }

    try assertEquals(actual: completionCalled, expected: 1)
    try assertEquals(actual: result, expected: "suspend function Swift")
    try assertNil(error)
}

#if NO_GENERICS
typealias AnyResultHolder = ResultHolder
#else
typealias AnyResultHolder = ResultHolder<AnyObject>
#endif

private extension AnyResultHolder {
    func getSuccessfulResult() throws -> Any? {
        try assertEquals(actual: completed, expected: 1)
        try assertNil(exception)
        return result
    }
}

// Reported in https://youtrack.jetbrains.com/issue/KT-51043
private func testSuspendFunction0SwiftImplStartInKotlin() throws {
    let resultHolder = AnyResultHolder()
    try CoroutinesKt.startCoroutineUninterceptedOrReturn(fn: SuspendFunction0SwiftImpl(), resultHolder: resultHolder)
    try assertEquals(actual: resultHolder.getSuccessfulResult() as? String, expected: "Swift")
}

private func testSuspendFunction1SwiftImplStartInKotlin() throws {
    let resultHolder = AnyResultHolder()
    try CoroutinesKt.startCoroutineUninterceptedOrReturn(fn: SuspendFunction1SwiftImpl(), receiver: "receiver", resultHolder: resultHolder)
    try assertEquals(actual: resultHolder.getSuccessfulResult() as? String, expected: "receiver Swift")
}

private func testSuspendFunction2SwiftImplStartInKotlin() throws {
    let resultHolder = AnyResultHolder()
    try CoroutinesKt.startCoroutineUninterceptedOrReturn(fn: SuspendFunction2SwiftImpl(), receiver: "receiver", param: "param", resultHolder: resultHolder)
    try assertEquals(actual: resultHolder.getSuccessfulResult() as? String, expected: "receiver param Swift")
}

private func testSuspendFunction0SwiftImplCreateInKotlin() throws {
    let resultHolder = AnyResultHolder()
    try CoroutinesKt.createCoroutineUninterceptedAndResume(fn: SuspendFunction0SwiftImpl(), resultHolder: resultHolder)
    try assertEquals(actual: resultHolder.getSuccessfulResult() as? String, expected: "Swift")
}

private func testSuspendFunction1SwiftImplCreateInKotlin() throws {
    let resultHolder = AnyResultHolder()
    try CoroutinesKt.createCoroutineUninterceptedAndResume(fn: SuspendFunction1SwiftImpl(), receiver: "receiver", resultHolder: resultHolder)
    try assertEquals(actual: resultHolder.getSuccessfulResult() as? String, expected: "receiver Swift")
}

private class SuspendFunction0SwiftImpl : KotlinSuspendFunction0 {
    func invoke(completionHandler: (Any?, Error?) -> Void) {
        completionHandler("Swift", nil)
    }
}

private class SuspendFunction1SwiftImpl : KotlinSuspendFunction1 {
    func invoke(p1: Any?, completionHandler: (Any?, Error?) -> Void) {
        completionHandler("\(p1 ?? "nil") Swift", nil)
    }
}

private class SuspendFunction2SwiftImpl : KotlinSuspendFunction2 {
    func invoke(p1: Any?, p2: Any?, completionHandler: (Any?, Error?) -> Void) {
        completionHandler("\(p1 ?? "nil") \(p2 ?? "nil") Swift", nil)
    }
}

class CoroutinesTests : SimpleTestProvider {
    override init() {
        super.init()

        test("TestCallSimple", testCallSimple)
        test("TestCallUnitSimple", testUnitCallSimple)
        test("TestCallFromNonMainDispatcher", testUnitCallNonMainDispatcher)
        test("TestCall", testCall)
        test("TestCallChain", testCallChain)
        test("TestOverride", testOverride)
        test("TestBridges", testBridges)
        test("TestImplicitThrows1", testImplicitThrows1)
        test("TestImplicitThrows2", testImplicitThrows2)
        test("TestSuspendFunctionType", testSuspendFunctionType)
        test("TestKSuspendFunctionType", testSuspendFunctionType)
        test("TestSuspendFunctionSwiftImpl", testSuspendFunctionSwiftImpl)
        test("TestSuspendFunction0SwiftImplStartInKotlin", testSuspendFunction0SwiftImplStartInKotlin)
        test("TestSuspendFunction1SwiftImplStartInKotlin", testSuspendFunction1SwiftImplStartInKotlin)
        test("TestSuspendFunction2SwiftImplStartInKotlin", testSuspendFunction2SwiftImplStartInKotlin)
        test("TestSuspendFunction0SwiftImplCreateInKotlin", testSuspendFunction0SwiftImplCreateInKotlin)
        test("TestSuspendFunction1SwiftImplCreateInKotlin", testSuspendFunction1SwiftImplCreateInKotlin)
    }
}
