// WITH_STDLIB
// FILE: FooFileFacade1.kt
package foo

const val fooFileConst1 = 1
fun file1() {}

// FILE: FooFileFacade2.kt
package foo

const val fooFileConst2 = 2
fun file2() {}

// FILE: BarFileFacade1.kt
package bar

const val barFileConst1 = 1
fun file1() {}

// FILE: FooMultiFilePart1.kt
@file:JvmMultifileClass
@file:JvmName("MultiFoo")

package foo

const val fooMultiFileConst1 = 1
fun multiFile1() {}

// FILE: FooMultiFilePart2.kt
@file:JvmMultifileClass
@file:JvmName("MultiFoo")

package foo

const val fooMultiFileConst2 = 2
fun multiFile2() {}

// FILE: BarMultiFilePart1.kt
@file:JvmMultifileClass
@file:JvmName("MultiBar")

package bar

const val barMultiFileConst1 = 1
fun multiFile1() {}

// FILE: BarMultiFilePart2.kt
@file:JvmMultifileClass
@file:JvmName("MultiBar")

package bar

const val barMultiFileConst2 = 2
fun multiFile2() {}

// FILE: AnotherFooMultiFilePart1.kt
@file:JvmMultifileClass
@file:JvmName("AnotherMultiFoo")

package foo

fun anotherMultiFile1() {}
