// FULL_JDK
// WITH_STDLIB

<!WRONG_ANNOTATION_TARGET!>@file:Log<!> // Prohibited

import lombok.extern.java.Log
import lombok.AccessLevel

@Log(access = AccessLevel.PUBLIC)
class LogExamplePublic

@Log(access = AccessLevel.PROTECTED)
open class LogExampleProtectedBase

class LogExampleProtected : LogExampleProtectedBase() {
    fun test() {
        log.info("Test LogExampleProtected") // OK
    }
}

@Log(access = AccessLevel.PRIVATE)
open class LogExamplePrivateBase {
    fun testBase() {
        log.info("Test LogExamplePrivateBase") // OK
    }
}

class LogExamplePrivate : LogExamplePrivateBase() {
    fun test() {
        <!INVISIBLE_REFERENCE!>log<!>.info("Test LogExamplePrivate") // Invisible
    }
}

<!WRONG_ANNOTATION_TARGET!>@Log<!> // Prohibited, `'@lombok.extern.java.Log' is legal only on classes and enums` in Java
interface Interface

<!WRONG_ANNOTATION_TARGET!>@Log<!> // Prohibited
fun func() {}

<!WRONG_ANNOTATION_TARGET!>@Log<!> // Prohibited
typealias TA = String

val logOnAnonymousObject = <!WRONG_ANNOTATION_TARGET!>@Log<!> object {} // Prohibited, because companion objects are disallowed inside anonymous objects, `Annotations are not allowed here` in Java

fun check() {
    <!WRONG_ANNOTATION_TARGET!>@Log<!> // Prohibited, because companion objects are disallowed inside local classes
    class LocalClass

    LogExamplePublic.log.info("Test LogExamplePublic") // OK
    LogExampleProtected.<!UNRESOLVED_REFERENCE!>log<!>.info("Test LogExampleProtected") // INVISIBLE
    LogExamplePrivate.<!UNRESOLVED_REFERENCE!>log<!>.info("Test LogExamplePrivate") // INVISIBLE
}

<!LOG_PROPERTY_ALREADY_EXISTS!>@Log<!>
class LogOnOuterClassWhenItsCompanionHasLogField {
    companion object MyCompanion {
        val log = "No log"
    }
}

class LogOnCompanionWhenCompanionHasLogField {
    <!LOG_PROPERTY_ALREADY_EXISTS!>@Log<!>
    companion object MyCompanion {
        val log = "No log"
    }
}
