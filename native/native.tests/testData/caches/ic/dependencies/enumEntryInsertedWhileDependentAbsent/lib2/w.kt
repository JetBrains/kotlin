package lib2

import lib1.E

fun describe(e: E): String = when (e) {
    E.B -> "b"
    E.C -> "c"
}
