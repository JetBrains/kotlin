// WITH_RUNTIME

import java.io.IOException
import java.lang.ArithmeticException
import java.lang.RuntimeException

@Throws(RuntimeException::class)
fun a() {}

@Throws(Throwable::class)
fun b() {}

@Throws(IOException::class, ArithmeticException::class)
fun c() {}