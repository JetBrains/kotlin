// WITH_STDLIB

// FILE: a.kt
@file:JvmMultifileClass
@file:JvmName("M1")
package test

fun foo() {}

// FILE: b.kt
@file:JvmMultifileClass
@file:JvmName("M1")
package test

fun bar() {}

// FILE: c.kt
@file:JvmMultifileClass
@file:JvmName("M2")
package test

fun baz() {}