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

// TODO KT-87871: the warning is a lie for both of these - `LoggerGenerator` has no interface/annotation-class
//  guard, so a companion object holding `log` is generated anyway.
<!ANNOTATION_HAS_NO_EFFECT!>@Log<!> // `'@lombok.extern.java.Log' is legal only on classes and enums` in Java
interface Interface

<!ANNOTATION_HAS_NO_EFFECT!>@Log<!>
annotation class AnnotationClass

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

// The member property would shadow a logger generated into the companion object, so nothing is generated at all -
// not even the companion object itself - and the use site keeps resolving to the member.
<!LOG_PROPERTY_ALREADY_EXISTS!>@Log<!>
class LogOnClassWithMemberLogPropertyAndNoCompanion {
    val log = ""

    fun test() {
        log.<!UNRESOLVED_REFERENCE!>info<!>("Test LogOnClassWithMemberLogPropertyAndNoCompanion")
    }
}

<!LOG_PROPERTY_ALREADY_EXISTS!>@Log<!>
class LogOnClassWithMemberLogPropertyAndCompanion {
    val log = ""

    companion object MyCompanion

    fun test() {
        log.<!UNRESOLVED_REFERENCE!>info<!>("Test LogOnClassWithMemberLogPropertyAndCompanion")
        MyCompanion.<!UNRESOLVED_REFERENCE!>log<!>.info("Not generated into the existing companion object either")
    }
}

@Log(access = AccessLevel.PROTECTED)
class LogAccessLevelProtected

@Log(access = <!UNSUPPORTED_ACCESS_LEVEL!>AccessLevel.PACKAGE<!>) // Prohibited, KT-88337
class LogAccessLevelPackage

@Log(access = <!UNSUPPORTED_ACCESS_LEVEL!>AccessLevel.<!DEPRECATION!>MODULE<!><!>) // Prohibited, KT-88337
class LogAccessLevelModule

fun test() {
    LogAccessLevelProtected.<!INVISIBLE_REFERENCE!>log<!>.info("")
    Interface.<!INVISIBLE_REFERENCE!>log<!>.info("Interface") // TODO: should be unresolved, KT-87871
    AnnotationClass.<!INVISIBLE_REFERENCE!>log<!>.info("AnnotationClass") // TODO: should be unresolved, KT-87871
}
