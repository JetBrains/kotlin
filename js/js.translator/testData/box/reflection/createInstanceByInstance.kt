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
class PrivateConstructor private constructor() {
    companion object {
        fun create() = PrivateConstructor()
    }
}
object Object
enum class EnumFoo { A, B }

// -----------

inline fun <T : Any> testInstance(x: T) {
    val kclass = x::class
    val anotherInstance = kclass.createInstance()
    assertTrue(kclass.isInstance(x) && kclass.isInstance(anotherInstance))
}

inline fun <T : Any> testInstanceFail(x: T) {
    try {
        val kclass = x::class
        kclass.createInstance()
        fail("createInstance should have failed on $kclass")
    } catch (e: Exception) {
        // OK
    }
}

fun box(): String {
    testInstance(Simple())
    testInstance(PrimaryWithDefaults("d2"))
    testInstance(Secondary("test"))
    testInstance(SecondaryWithDefaults("test"))
    testInstance(SecondaryWithDefaultsNoPrimary(2))

    testInstanceFail(NoNoArgConstructor(4))
    testInstanceFail(NoArgAndDefault())
    testInstanceFail(DefaultPrimaryAndDefaultSecondary(4))
    testInstanceFail(SeveralDefaultSecondaries(4))
    testInstanceFail(PrivateConstructor.create())
    testInstanceFail(Object)
    testInstanceFail(EnumFoo.A)

    return "OK"
}
