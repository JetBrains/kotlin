import kotlinx.cinterop.*
import kotlin.test.*
import objcTests.*

@Test fun testExceptions() {
    assertFailsWith<MyException> {
        ExceptionThrowerManager.throwExceptionWith(object : NSObject(), ExceptionThrowerProtocol {
            override fun throwException() {
                throw MyException()
            }
        })
    }
}

private class MyException : Throwable()