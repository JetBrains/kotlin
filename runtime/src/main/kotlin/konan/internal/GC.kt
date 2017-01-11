package konan.internal

// Garbage collector interface.
// TODO: more functions to come.
object GC {
    @SymbolName("Kotlin_konan_internal_GC_collect")
    external fun collect()
}