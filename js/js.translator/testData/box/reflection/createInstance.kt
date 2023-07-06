// IGNORE_BACKEND: JS
// KJS_WITH_FULL_RUNTIME

import kotlin.reflect.createInstance
import kotlin.test.assertTrue
import kotlin.test.fail

// Good classes

class Simple
class PrimaryWithDefaults(val d1: String = "d1", val d2: Int = 2)
class Secondary(val s: String) {
    constructor() : this("s")
}
class SecondaryWithDefaults(val s: String) {
    constructor(x: Int = 0) : this(x.toString())
}
class SecondaryWithDefaultsNoPrimary {
    constructor(x: Int) {}
    constructor(s: String = "") {}
}

// Bad classes

class NoNoArgConstructor(val s: String) {
    constructor(x: Int) : this(x.toString())
}
class NoArgAndDefault() {
    constructor(x: Int = 0) : this()
}
class DefaultPrimaryAndDefaultSecondary(val s: String = "") {
    constructor(x: Int = 0) : this(x.toString())
}
class SeveralDefaultSecondaries {
    constructor(x: Int = 0) {}
    constructor(s: String = "") {}
    constructor(d: Double = 3.14) {}
}
class PrivateConstructor private constructor()
object Object
interface Interface
enum class EnumFoo { A, B }

abstract class AbstractClass

sealed class SealedClass

// -----------

inline fun <reified T : Any> test() {
    val instance = T::class.createInstance()
    assertTrue(instance is T)
}

inline fun <reified T : Any> testFail() {
    try {
        T::class.createInstance()
        fail("createInstance should have failed on ${T::class}")
    } catch (e: Exception) {
        // OK
    }
}

fun box(): String {
    test<Any>()
    test<Simple>()
    test<PrimaryWithDefaults>()
    test<Secondary>()
    test<SecondaryWithDefaults>()
    test<SecondaryWithDefaultsNoPrimary>()

    testFail<NoNoArgConstructor>()
    testFail<NoArgAndDefault>()
    testFail<DefaultPrimaryAndDefaultSecondary>()
    testFail<SeveralDefaultSecondaries>()
    testFail<PrivateConstructor>()
    testFail<Object>()
    testFail<Interface>()
    testFail<EnumFoo>()
    testFail<AbstractClass>()
    testFail<SealedClass>()

    return "OK"
}
