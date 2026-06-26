// KIND: STANDALONE_LLDB
// FIR_IDENTICAL
// IGNORE_NATIVE: cacheMode=NO
// IGNORE_NATIVE: cacheMode=STATIC_ONLY_DIST
// IGNORE_NATIVE: cacheMode=STATIC_EVERYWHERE
// IGNORE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE
// INPUT_DATA_FILE: canInspectValuesOfPrimitiveTypes.in
// OUTPUT_DATA_FILE: canInspectValuesOfPrimitiveTypesWithCache.out
fun main(args: Array<String>) {
    var a: Byte =  1
    var b: Int  =  2
    var c: Long = -3
    var d: Char = 'c'
    var e: Boolean = true
    return
}
