import Kt

private func testUnwrapsToSame() throws {
    let throwable = ThrowableAsError()
    try assertSame(actual: throwable.asError().kotlinException as AnyObject, expected: throwable)
}

private func testCanThrowAndCatch() throws {
    let throwable = ThrowableAsError()
    let impl = ThrowsThrowableAsErrorImpl(throwable: throwable)
    let caught = ThrowableAsErrorKt.callAndCatchThrowableAsError(throwsThrowableAsError: impl)
    try assertSame(actual: caught, expected: throwable)
}

private class ThrowsThrowableAsErrorImpl : ThrowsThrowableAsError {
    var throwable: KotlinThrowable

    init(throwable: KotlinThrowable) {
        self.throwable = throwable
    }

    func throwError() throws {
        throw throwable.asError()
    }
}

private func testCanThrowAndCatchSuspend() throws {
    let throwable = ThrowableAsError()
    let impl = ThrowsThrowableAsErrorSuspendImpl(throwable: throwable)
    let caught = ThrowableAsErrorKt.callAndCatchThrowableAsErrorSuspend(throwsThrowableAsErrorSuspend: impl)
    try assertSame(actual: caught, expected: throwable)
}

private class ThrowsThrowableAsErrorSuspendImpl : ThrowsThrowableAsErrorSuspend {
    var throwable: KotlinThrowable

    init(throwable: KotlinThrowable) {
        self.throwable = throwable
    }

#if LEGACY_SUSPEND_UNIT_FUNCTION_EXPORT
    func throwError(completionHandler: @escaping (KotlinUnit?, Error?) -> Void) {
        completionHandler(nil, throwable.asError())
    }
#else
    func throwError(completionHandler: @escaping (Error?) -> Void) {
        completionHandler(throwable.asError())
    }
#endif
}

class ThrowableAsErrorTests : SimpleTestProvider {
    override init() {
        super.init()

        test("TestUnwrapsToSame", testUnwrapsToSame)
        test("TestCanThrowAndCatch", testCanThrowAndCatch)
        test("TestCanThrowAndCatchSuspend", testCanThrowAndCatchSuspend)
    }
}
