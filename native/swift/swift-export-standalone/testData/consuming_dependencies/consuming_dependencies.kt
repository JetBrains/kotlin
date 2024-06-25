// KIND: STANDALONE
// MODULE: main(dependency)
// FILE: main.kt
import dependency.one.*
import dependency.two.*
import dependency.three.*

fun main() = foo()

// MODULE: dependency(dependency_deeper,dependency_deeper_neighbor)
// FILE: foo_deps.kt
package dependency.one

import dependency.two.*
import dependency.three.*

fun foo() = bar()


// MODULE: dependency_deeper()
// FILE: bar_deps_deep.kt
package dependency.two

fun bar() = 5

// MODULE: dependency_deeper_neighbor()
// FILE: bar_deps_deeper.kt
package dependency.three

class Bar
