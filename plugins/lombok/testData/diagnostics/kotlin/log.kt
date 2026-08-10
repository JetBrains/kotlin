// FULL_JDK
// WITH_STDLIB

<!ANNOTATION_HAS_NO_EFFECT!>@file:Log<!> // Maybe will be implement in future (apply `@Log` to all suitable declarations)

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

<!ANNOTATION_HAS_NO_EFFECT!>@Log<!> // `'@lombok.extern.java.Log' is legal only on classes and enums` in Java
interface Interface

<!WRONG_ANNOTATION_TARGET!>@Log<!> // Prohibited
fun func() {}

<!WRONG_ANNOTATION_TARGET!>@Log<!> // Prohibited
typealias TA = String

val logOnAnonymousObject = <!ANNOTATION_HAS_NO_EFFECT!>@Log<!> object {} // Companion objects are disallowed inside anonymous objects, `Annotations are not allowed here` in Java

val logOnLiteral = <!ANNOTATION_HAS_NO_EFFECT!>@Log<!> 1
val logOnCall = <!ANNOTATION_HAS_NO_EFFECT!>@Log<!> func()

fun check() {
    <!ANNOTATION_HAS_NO_EFFECT!>@Log<!> // Companion objects are disallowed inside local classes
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

// TODO KT-88248: 'LOG_PROPERTY_ALREADY_EXISTS' should be reported on both classes below, as Java Lombok does
//  regardless of whether the existing field is static. With the default 'lombok.log.fieldIsStatic=true' the
//  checker only inspects the companion object, so a member property named 'log' goes unnoticed: the logger is
//  still generated into the companion and gets shadowed by the member, leaving a confusing 'UNRESOLVED_REFERENCE'
//  at the use site. See 'logWithConfig.kt' for the same declaration being reported with 'fieldIsStatic=false'.
@Log
class LogOnClassWithMemberLogPropertyAndNoCompanion {
    val log = ""

    fun test() {
        log.<!UNRESOLVED_REFERENCE!>info<!>("Test LogOnClassWithMemberLogPropertyAndNoCompanion")
    }
}

@Log
class LogOnClassWithMemberLogPropertyAndCompanion {
    val log = ""

    companion object MyCompanion

    fun test() {
        log.<!UNRESOLVED_REFERENCE!>info<!>("Test LogOnClassWithMemberLogPropertyAndCompanion")
        MyCompanion.log.info("The generated logger is still there, just shadowed by the member property")
    }
}
