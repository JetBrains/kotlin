// STRICT

// Errors should be reported in this test, but they aren't because of KT-72659.

// FILE: a.kt

class RootClass

class AnotherRootClass

// FILE: test/b.kt

@file:JvmMultifileClass
@file:JvmName("M1")
package test

import RootClass

fun foo(): RootClass? = null

// FILE: test/c.kt

@file:JvmMultifileClass
@file:JvmName("M1")
package test

import AnotherRootClass

fun bar(): AnotherRootClass? = null
