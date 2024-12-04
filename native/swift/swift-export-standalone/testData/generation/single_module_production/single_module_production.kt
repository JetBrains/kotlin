// KIND: STANDALONE
// MODULE: main2
// FILE: main.kt
package demo.shared

fun foo2() = 6
// MODULE: main(dependency)
// SWIFT_EXPORT_CONFIG: multipleModulesHandlingStrategy=IntoSingleModule, packageRoot=org.kotlin.foo
// FILE: shared.kt
package demo.shared

typealias Integer = Int
// FILE: baz.kt
package org.kotlin.baz

typealias Integer = Int
// FILE: foo.bar.kt
package org.kotlin.foo.bar

typealias Integer = Int
// FILE: foo.kt
package org.kotlin.foo

import org.dependency.*

typealias Typealias = Int

class Clazz

fun function(arg: Int) = arg

var variable: Int = foo()

val constant: Int = 0

// MODULE: dependency()
// FILE: foo_deps.kt
package org.dependency

fun foo() = 5
