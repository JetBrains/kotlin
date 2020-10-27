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

private func testSuspendFuncAsync(doThrow: Bool) throws {
    var completionCalled = 0
    var result: AnyObject? = nil
    var error: Error? = nil

    let continuationHolder = ContinuationHolder<AnyObject>()

    CoroutinesKt.suspendFunAsync(result: nil, continuationHolder: continuationHolder) { _result, _error in
        completionCalled += 1
        result = _result as AnyObject?
        error = _error
    }

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
}

private func testCall() throws {
    try testCallSuspendFun(doSuspend: true, doThrow: false)
    try testCallSuspendFun(doSuspend: false, doThrow: false)
    try testCallSuspendFun(doSuspend: true, doThrow: true)
    try testCallSuspendFun(doSuspend: false, doThrow: true)

    try testSuspendFuncAsync(doThrow: false)
    try testSuspendFuncAsync(doThrow: true)
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
    let resultHolder = ResultHolder<KotlinInt>()

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
        try assertEquals(actual: resultHolder.result, expected: 17)
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

    override func unitAsAny(value: KotlinInt, completionHandler: @escaping (KotlinUnit?, Error?) -> Void) {
        completionHandler(KotlinUnit(), nil)
    }

    override func nothingAsInt(value: KotlinInt, completionHandler: @escaping (KotlinNothing?, Error?) -> Void) {
        completionHandler(nil, E())
    }

    override func nothingAsAny(value: KotlinInt, completionHandler: @escaping (KotlinNothing?, Error?) -> Void) {
        completionHandler(nil, E())
    }

    override func nothingAsUnit(value: KotlinInt, completionHandler: @escaping (KotlinNothing?, Error?) -> Void) {
        completionHandler(nil, E())
    }
}

private func testBridges() throws {
    let resultHolder = ResultHolder<KotlinUnit>()
    try CoroutinesKt.callSuspendBridge(bridge: SwiftSuspendBridge(), resultHolder: resultHolder)

    try assertEquals(actual: resultHolder.completed, expected: 1)
    try assertNil(resultHolder.exception)
    try assertSame(actual: resultHolder.result, expected: KotlinUnit())
}

private func testImplicitThrows1() throws {
    var result: KotlinUnit? = nil
    var error: Error? = nil
    var completionCalled = 0

    CoroutinesKt.throwCancellationException { _result, _error in
        completionCalled += 1
        result = _result
        error = _error
    }

    try assertEquals(actual: completionCalled, expected: 1)
    try assertNil(result)
    try assertTrue(error?.kotlinException is KotlinCancellationException)
}

private func testImplicitThrows2() throws {
    var result: KotlinUnit? = nil
    var error: Error? = nil
    var completionCalled = 0

    ThrowCancellationExceptionImpl().throwCancellationException { _result, _error in
        completionCalled += 1
        result = _result
        error = _error
    }

    try assertEquals(actual: completionCalled, expected: 1)
    try assertNil(result)
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
    try testSuspendFunctionType1(f: CoroutinesKt.getSuspendLambda1())
    try testSuspendFunctionType1(f: CoroutinesKt.getSuspendCallableReference1())
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

private class SuspendFunction1SwiftImpl : KotlinSuspendFunction1 {
    func invoke(p1: Any?, completionHandler: (Any?, Error?) -> Void) {
        completionHandler("\(p1 ?? "nil") Swift", nil)
    }
}

class CoroutinesTests : SimpleTestProvider {
    override init() {
        super.init()

        test("TestCallSimple", testCallSimple)
        test("TestCall", testCall)
        test("TestOverride", testOverride)
        test("TestBridges", testBridges)
        test("TestImplicitThrows1", testImplicitThrows1)
        test("TestImplicitThrows2", testImplicitThrows2)
        test("TestSuspendFunctionType", testSuspendFunctionType)
        test("TestSuspendFunctionSwiftImpl", testSuspendFunctionSwiftImpl)
    }
}