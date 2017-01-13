package kotlin.test

impl fun AssertionError(message: String): Throwable = kotlin.AssertionError(message)
impl fun AssertionError(): Throwable = kotlin.AssertionError()
