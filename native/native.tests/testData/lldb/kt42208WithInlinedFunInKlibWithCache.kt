// KIND: STANDALONE_LLDB
// FREE_COMPILER_ARGS: -Xklib-ir-inliner=full
// IGNORE_NATIVE: cacheMode=NO
// IGNORE_NATIVE: cacheMode=STATIC_ONLY_DIST
// IGNORE_NATIVE: cacheMode=STATIC_EVERYWHERE
// IGNORE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE
// IGNORE_NATIVE: mode=ONE_STAGE_MULTI_MODULE
// ^^^ See KT-77365 regarding the `mode=` filter.
// INPUT_DATA_FILE: kt42208WithInlinedFunInKlibWithCache.in
// OUTPUT_DATA_FILE: kt42208WithInlinedFunInKlibWithCache.out

// FILE: kt42208-1.kt
fun main() {
    foo()()
}
// FILE: kt42208-2.kt
// aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
inline fun foo() = {
    throw Error()
}
