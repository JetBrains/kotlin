// KIND: STANDALONE_LLDB
// IGNORE_NATIVE: cacheMode=NO
// IGNORE_NATIVE: cacheMode=STATIC_ONLY_DIST
// IGNORE_NATIVE: cacheMode=STATIC_EVERYWHERE
// IGNORE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE
// INPUT_DATA_FILE: kt42208WithCache.in
// OUTPUT_DATA_FILE: kt42208WithCache.out



// FILE: kt42208-1.kt
fun main() {
    foo()()
}
// FILE: kt42208-2.kt
// aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
inline fun foo() = {
    throw Error()
}
