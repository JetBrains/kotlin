// FIR_IDENTICAL
@file:Suppress("UNUSED_PARAMETER")

package custom.pkg

class Foo

typealias MyTransformer = (String) -> Int

// top-level properties
val v1 = 1
val v2 = "hello"
val v3: (String) -> Int = { it.length }
val v4: MyTransformer = v3

object Bar
