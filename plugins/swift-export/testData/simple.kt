// WITH_STDLIB

// FILE: foo.kt
package namespace1
fun foo(): Int = 123

// FILE: bar.kt
package namespace2
fun bar(): Int = 321

// FILE: main.kt
package namespace1.main

import namespace1.*
import namespace2.*
fun foobar(): Int = foo() + bar()
