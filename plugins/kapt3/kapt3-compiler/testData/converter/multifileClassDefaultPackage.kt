// STRICT

// EXPECTED_ERROR: (other:-1:-1) test.M1: Can't reference type 'RootClass' from default package in Java stub.
// EXPECTED_ERROR_K2: (other:-1:-1) test.M1: Can't reference type 'AnotherRootClass' from default package in Java stub.

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