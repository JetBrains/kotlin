// MODULE: lib1
// FILE: lib1.kt

package lib1

interface I {
    fun interfaceFun(default: Int = 42)

    companion object {
        val companionObjectVal = "foo"
    }
}

fun <T> take(x: T) { }
fun getBoolean(): Boolean = true

fun functionCalls(i: I) {
    if (getBoolean()) {
        take(I.companionObjectVal)
    }

    while (getBoolean()) {
        i.interfaceFun()
    }
}

// MODULE: lib2
// FILE: lib2.kt

package lib2

object Object {
    private object PrivateObject {
        fun foo(): Nothing = error("Dead end")
    }
}

sealed interface SealedInterface {
    val v: HashSet<Pair<String, Number>>

    enum class Enum : SealedInterface {
        FOO, BAR;

        override val v: HashSet<Pair<String, Number>>
            get() = setOf<Pair<String, Number>>().toMutableSet().toHashSet()
    }

    data object Object : SealedInterface {
        override val v by lazy<HashSet<Pair<String, Number>>> {
            TODO("Never would be implemented")
        }
    }
}

// MODULE: lib3(lib1, lib2)
// FILE: lib3.kt

package lib3

const val FOO = "BAR"
const val BAR = FOO.length

class IImpl : lib1.I {
    override fun interfaceFun(default: Int) {
        kotlin.math.tanh(kotlin.math.PI * default)
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun doStuff() {
    lib1.take(42)
    check(lib1.getBoolean())
    lib1.functionCalls(IImpl())

    println(lib2.Object.toString())
    lib2.SealedInterface.Enum.entries.forEach { println(it) }
    check(lib2.SealedInterface.Object.v.isEmpty())
}

// MODULE: app(lib1, lib2, lib3)
// FILE: app.kt

package app

fun main() {
    lib3.doStuff()
}
