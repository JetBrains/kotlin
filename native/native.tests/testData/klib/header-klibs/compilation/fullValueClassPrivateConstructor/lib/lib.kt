// ISSUE: KT-89980
// FREE_COMPILER_ARGS: -XXLanguage:+FullValueClasses
// A header klib keeps what the value class representation needs, even with a private primary constructor,
// so compiling against it gives the same klib as compiling against the full one.
package lib

value class Secret private constructor(val a: Int, val b: Int) {
    companion object {
        fun of(x: Int) = Secret(x, x)
    }
}

inline fun twice(secret: Secret) = Secret.of(secret.a * 2)
