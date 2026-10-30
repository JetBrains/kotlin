// KIND: STANDALONE
// MODULE: Inheritance
// FILE: cached_bridges.kt

// KT-89121: with STATIC_EVERYWHERE or INCREMENTAL, the generated Swift Export
// bridges are cached, including their @BindClassToObjCName annotations. The adapters must reach the final binary.
open class Base {
    fun foo(): String = "foo"
}
