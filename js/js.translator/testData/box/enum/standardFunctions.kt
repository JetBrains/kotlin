// !LANGUAGE: +EnumEntries
// IGNORE_BACKEND: JS
// KJS_WITH_FULL_RUNTIME
// EXPECTED_REACHABLE_NODES: 1555

// TODO: Remove after enumEntries become public
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package foo

import kotlin.enums.enumEntries

enum class EmptyEnum

enum class A {
    a() {
    },
    b(),
    c
}

@OptIn(ExperimentalStdlibApi::class)
fun box(): String {
    if (enumValues<EmptyEnum>().size != 0) return "enumValues<EmptyEnum>().size != 0"
    if (enumValues<A>().asList() != listOf(A.a, A.b, A.c)) return "Wrong enumValues<A>(): " + enumValues<A>().toString()
    if (enumEntries<EmptyEnum>().size != 0) return "enumEntries<EmptyEnum>().size != 0"
    if (enumEntries<A>() != listOf(A.a, A.b, A.c)) return "Wrong enumEntries<A>(): " + enumEntries<A>().toString()
    if (enumEntries<A>() != enumEntries<A>()) return "Enum entries create a new EntriesList for each call"
    if (enumValueOf<A>("b") != A.b) return "enumValueOf<A>('b') != A.b"
    return "OK"
}