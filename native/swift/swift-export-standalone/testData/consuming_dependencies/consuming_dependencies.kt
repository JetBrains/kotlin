// KIND: STANDALONE
// MODULE: main_one(dependency)
// FILE: main.kt
package org.main.first

import dependency.one.*
import dependency.two.*
import dependency.three.*

fun main_first() = foo()

// MODULE: main_two(dependency_deeper_neighbor)
// FILE: main.kt
package org.main.second

import dependency.three.*

val deps_instance_2: Any = Bar()

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
