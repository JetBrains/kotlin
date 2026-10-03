// FREE_COMPILER_ARGS: -XXLanguage:+FullValueClasses
package lib

value class Secret private constructor(val a: Int, val b: Int) {
    companion object {
        fun of(x: Int) = Secret(x, x)
    }
}

inline fun twice(secret: Secret) = Secret.of(secret.a * 2)
