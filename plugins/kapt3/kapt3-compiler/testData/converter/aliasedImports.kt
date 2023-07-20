// CORRECT_ERROR_TYPES
// NO_VALIDATION

@file:Suppress("UNRESOLVED_REFERENCE")
import java.util.Date as MyDate
import java.util.concurrent.TimeUnit as MyTimeUnit
import java.util.concurrent.TimeUnit.*
import java.util.concurrent.TimeUnit.MICROSECONDS as MyMicroseconds
import kotlin.arrayOf
import a.b.ABC as MyABC
import bcd as MyBCD

class Test {
    lateinit var date: MyDate
    lateinit var timeUnit: MyTimeUnit

    lateinit var abc: MyABC
    lateinit var bcd: MyBCD

    class MyDate {
        lateinit var date2: MyDate
    }
}

class Test2 {
    lateinit var date: MyDate
}
