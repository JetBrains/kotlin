// STRICT
// JAVAC_OPTIONS: -Xmaxerrs=1

// There are two errors actually.
// But we specified the max error count, so the error output is limited.

// EXPECTED_ERROR: (other:-1:-1) test.C: Can't reference type 'RootAnnotation' from default package in Java stub.

// FILE: a.kt

class RootClass

annotation class RootAnnotation

// FILE: b.kt
package test

import RootClass
import RootAnnotation

fun f(): RootClass? = null

@RootAnnotation
class C
