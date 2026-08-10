// KIND: STANDALONE_LLDB
// IGNORE_NATIVE: cacheMode=STATIC_ONLY_DIST
// IGNORE_NATIVE: cacheMode=STATIC_EVERYWHERE
// IGNORE_NATIVE: cacheMode=STATIC_PER_FILE_EVERYWHERE
// IGNORE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE
// INPUT_DATA_FILE: stdlibRelativePaths.in
// OUTPUT_DATA_FILE: stdlibRelativePaths.out
fun main() {
    // Ideally, we should be testing relative paths in user klibs as well.
    // But that turned out to be trickier than expected.
    // Also, this test doesn't check that lldb can display the actual stdlib sources,
    // because it requires additional configuration to make lldb find those sources.
    // All these improvements are tracked in KT-83757.
    Throwable()

    // Here we test the synthetic file "[K][Suspend]Functions" -- it is the only file in stdlib that has relative path with one segment.
    val lambda = {}
    lambda()
}
