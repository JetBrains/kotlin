// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Inheritance
// FILE: covariant_interface_override.kt

// A Kotlin class implements an interface member with a narrower type than the interface declares. Swift only
// accepts exactly-typed protocol witnesses, so the class's own member does not satisfy the protocol requirement.

open class Animal {
    open fun name(): String = "animal"
}

open class Cat : Animal() {
    override fun name(): String = "kotlin-cat"
}

// Covariant return type.

interface Shelter {
    fun adopt(): Animal
}

open class CatShelter : Shelter {
    override fun adopt(): Cat = Cat()
}

fun adoptViaInterface(s: Shelter): String = s.adopt().name()
fun adoptViaClass(s: CatShelter): String = s.adopt().name()

// Nullable return type narrowed to non-null.

interface MaybeNamer {
    fun title(): String?
}

open class SureNamer : MaybeNamer {
    override fun title(): String = "kotlin-title"
}

fun titleViaInterface(n: MaybeNamer): String? = n.title()
fun titleViaClass(n: SureNamer): String = n.title()
