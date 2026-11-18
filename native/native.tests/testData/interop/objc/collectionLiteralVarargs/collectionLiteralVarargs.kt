// The legacy lowering for *[...] uses arrayOf(...). KT-81722 switches it to Array.of(...)
// when the experimental CollectionLiterals feature is enabled. KT-82852 keeps variadic
// interop working with that lowering. The test enables the feature explicitly until it is on by default.

@file:OptIn(kotlin.ExperimentalCollectionLiteralsApi::class)

import collectionLiteralVarargs.TestVarargs

fun main() {
    val result = TestVarargs(format = "%d %d %d", args = *[1, 2, 3]).formatted
    check(result == "1 2 3") { "Unexpected formatted string: $result" }
}
