// TARGET_BACKEND: JVM_IR

// WITH_STDLIB

/**
 * This test checks that in the case when serialization plugin is applied, but kotlinx-serialization-core runtime is not present in compile classpath,
 * compilation of regular Kotlin classes still finishes succesfully.
 *
 * Such requirement is needed for cases when plugin is applied to a Gradle module, but runtime dependency is provided only in certain configurations,
 * e.g. only in `testImplementation` configuration (see :wasm:wasm-ir module). In such setup, production sources have plugin applied, but no runtime in classpath.
 */

data class X(val i: Int) {
    companion object {
        fun x(): X = X(42)
    }
}

fun box(): String {
    val i = X.x().i
    return if (i == 42) "OK" else i.toString()
}
