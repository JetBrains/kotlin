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

// Nothing is generated into either, so the warning is truthful: `log` is unresolved at the use sites below.
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

// `lombok.log.fieldIsStatic` alone decides whether the logger is static, so the annotation belongs on the class
// rather than on its companion object - and on the latter it generated nothing whatsoever with
// `fieldIsStatic=false`, KT-88288.
class LogOnCompanion {
    <!ANNOTATION_HAS_NO_EFFECT!>@Log<!>
    companion object MyCompanion {
        fun test() {
            <!UNRESOLVED_REFERENCE!>log<!>.info("Nothing is generated into the companion object")
        }
    }
}

// The annotation on the class keeps working: an inert one must not suppress one that has an effect.
@Log
class LogOnBothClassAndItsCompanion {
    <!ANNOTATION_HAS_NO_EFFECT!>@Log<!>
    companion object MyCompanion {
        fun test() {
            log.info("Generated from the annotation on the class")
        }
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
    Interface.<!UNRESOLVED_REFERENCE!>log<!>.info("Interface") // Nothing is generated, KT-87871
    AnnotationClass.<!UNRESOLVED_REFERENCE!>log<!>.info("AnnotationClass") // Nothing is generated, KT-87871
}
