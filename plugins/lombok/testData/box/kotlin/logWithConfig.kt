// FULL_JDK
// FIR_DUMP

// FILE: test.kt

import lombok.extern.java.Log
import lombok.AccessLevel

@Log
class LogExample {
    fun test() {
        myLog.info("Test nonstatic logger with custom name")
    }
}

open class Base {
    val myLog = "no log"
}

@Log
class Derived : Base() {
    fun test() {
        myLog.info("test Derived") // `Derived.log` is more prioritized than `Base.log`
    }
}

class LogOnNestedClass {
    @Log
    class Nested {
        fun test() {
            myLog.info("Check @Log on nested class")
        }
    }
}

class LogOnInnerClass<T> {
    @Log
    inner class Inner {
        fun test() {
            myLog.info("Check @Log on inner class")
        }
    }
}

@Log
object LogOnObject {
    fun test() {
        myLog.info("Check @Log on object")
    }
}

@Log
enum class LogOnEnum {
    ExampleEntry;

    fun test() {
        myLog.info("Check @Log on enum")
    }
}

// Generate `myLog` despite the confusing extension and contextual properties that actually don't conflict with the property being generated.
@Log(access = AccessLevel.PUBLIC)
class LogWhenNonConflictingExtensionAndContextualProperty {
    val LogWhenNonConflictingExtensionAndContextualProperty.myLog: Int get() = 1

    context(p: LogWhenNonConflictingExtensionAndContextualProperty)
    val myLog: LogWhenNonConflictingExtensionAndContextualProperty get() = p
}

fun box(): String {
    LogExample().test()
    Derived().test()
    LogOnNestedClass.Nested().test()
    LogOnInnerClass<String>().Inner().test()
    LogOnObject.test()
    LogOnEnum.ExampleEntry.test()
    LogWhenNonConflictingExtensionAndContextualProperty().myLog.info("Check LogWhenNonConflictingExtensionAndContextualProperty")
    return "OK"
}

// FILE: lombok.config
lombok.log.fieldName=myLog
lombok.log.fieldIsStatic=false
