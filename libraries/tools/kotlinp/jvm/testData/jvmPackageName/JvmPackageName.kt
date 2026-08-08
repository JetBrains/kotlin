// WITH_STDLIB
// FILE: Foo.kt
package foo

fun file1() {}

// FILE: FooMultiFile.kt
@file:JvmMultifileClass
@file:JvmName("MultiFoo")
package foo

fun multiFile1() {}

// FILE: BarAsJ.kt
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:JvmPackageName("j")
package bar

fun file0() {}

// FILE: FooAsJJJ.kt
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:JvmPackageName("jjj")
package foo

fun file2() {}

// FILE: BarAsJJ.kt
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:JvmName("JJ")
@file:JvmPackageName("jj")
package bar

fun jj() {}
