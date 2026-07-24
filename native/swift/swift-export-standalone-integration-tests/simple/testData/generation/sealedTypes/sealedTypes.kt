// KIND: STANDALONE
// MODULE: common
// TARGET_PLATFORM: Common
// SWIFT_EXPORT_CONFIG: packageRoot=org.kotlin.foo
// FILE: main.kt
package org.kotlin.foo

/* expect */ sealed interface SealedInterfaceA

/* expect */ sealed class SealedClassA() : SealedInterfaceA

sealed class SealedClassB : SealedClassA()

class ClassC : SealedClassA()

class ClassD : SealedClassB()

class ClassE : SealedInterfaceA

sealed interface SealedInterfaceB : SealedInterfaceA

interface InterfaceC : SealedInterfaceA

internal class ClassF : SealedInterfaceA

internal class ClassG : SealedClassA()

// FILE: deprecation.kt
package org.kotlin.foo

sealed class SealedClassNonDeprecated

@Deprecated("unavailable", level = DeprecationLevel.ERROR)
class DeprecatedErrorSubClass : SealedClassNonDeprecated()

@Deprecated("deprecated")
class DeprecatedWarningSubClass : SealedClassNonDeprecated()

@Deprecated("unavailable", level = DeprecationLevel.ERROR)
sealed class SealedClassDeprecatedError

@Suppress("DEPRECATION_ERROR")
class NonDeprecatedSubClassA : SealedClassDeprecatedError()

@Deprecated("deprecated")
sealed class SealedClassDeprecatedWarning

class NonDeprecatedSubClassB : SealedClassDeprecatedWarning()

// FILE: optin.kt
package org.kotlin.foo

@RequiresOptIn(message = "This needs an OptIn")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
annotation class OptInA

@RequiresOptIn(message = "This needs an OptIn")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
annotation class OptInB

sealed class SealedNonOptInClass

@OptInA
sealed class SealedOptInClass : SealedNonOptInClass()

class NonSealedNonOptInClassA : SealedNonOptInClass()

@OptInB
@OptIn(OptInA::class)
class NonSealedOptInClass : SealedOptInClass()

@OptIn(OptInA::class)
class NonSealedNonOptInClassB : SealedOptInClass()

// FILE: nameing.kt
package org.kotlin.foo

sealed class MySealedClass

class MyClassA {
    sealed class Inner : MySealedClass()
}

class MyClassB {
    sealed class Inner : MySealedClass()
}

// FILE: generic_interface.kt
package org.kotlin.foo

sealed interface QueryResult<T> {
    class Value<T>(val value: T) : QueryResult<T>
    class AsyncValue<T>(val value: T) : QueryResult<T>
}

// FIXME: platform test are not supported right now, enable in KT-86819
// DISABLED MODULE: platform()()(common)
// TARGET_PLATFORM: Native
// FILE: platform.kt
//package org.kotlin.foo
//
//actual sealed interface SealedInterfaceA
//
//actual sealed class SealedClassA actual constructor() : SealedInterfaceA
//
//class ClassHH : SealedClassA()
