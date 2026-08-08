// WITH_STDLIB
// FILE: K1_A.kt
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:JvmPackageName("j1")
@file:JvmName("K1")
@file:JvmMultifileClass
package k1

fun f1() {}

// FILE: K1_B.kt
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:JvmPackageName("j1")
@file:JvmName("K1")
@file:JvmMultifileClass
package k1

val v2 = Unit

// FILE: K1_C.kt
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:JvmPackageName("j1")
@file:JvmName("K1")
@file:JvmMultifileClass
package k1

typealias T3 = List<String>

// FILE: K2_A.kt
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:JvmPackageName("j2")
@file:JvmName("K2")
@file:JvmMultifileClass
package k2

fun f1() {}

// FILE: K2_B.kt
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:JvmPackageName("j3")
@file:JvmName("K2")
@file:JvmMultifileClass
package k2

val v2 = Unit

// FILE: K2_C.kt
@file:JvmName("K2")
@file:JvmMultifileClass
package k2

typealias T3 = List<String>
