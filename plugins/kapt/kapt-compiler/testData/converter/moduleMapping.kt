// DUMP_MODULE_MAPPING

// FILE: facade.kt
package test

fun facade(): String = ""

// FILE: a.kt
@file:JvmMultifileClass
@file:JvmName("Multi")
package test

fun a() {}

// FILE: b.kt
@file:JvmMultifileClass
@file:JvmName("Multi")
package test

val b: Int = 0

// FILE: renamed.kt
@file:JvmName("Renamed")
package test.other

const val c = 1

// FILE: classOnly.kt
package test

class C

// FILE: root.kt
fun root() {}
